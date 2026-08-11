package com.example.motionwall

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.webkit.URLUtil
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.motionwall.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Main screen for MotionWall.
 *
 * A source can be a local video chosen through Android's document picker or a
 * public, direct HTTP(S) video URL. In either case the source is saved and the
 * exact same URI is used by both the in-app preview and WallpaperService.
 */
class MainActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private var exoPlayer: ExoPlayer? = null
    private var previewUri: Uri? = null

    private val pickVideo =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            saveVideoSource(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(Keys.PREFS, MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        binding.selectVideoButton.setOnClickListener {
            pickVideo.launch(arrayOf("video/*"))
        }
        binding.useUrlButton.setOnClickListener { showUrlDialog() }
        binding.setWallpaperButton.setOnClickListener { launchLiveWallpaperChooser() }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.fitModeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.autoRadio -> FitMode.AUTO.name
                R.id.verticalRadio -> FitMode.VERTICAL.name
                R.id.horizontalRadio -> FitMode.HORIZONTAL.name
                R.id.cropRadio -> FitMode.CROP.name
                else -> FitMode.AUTO.name
            }
            prefs.edit().putString(Keys.FIT_MODE, mode).apply()
            applyPreviewFitMode()
        }

        loadUi()
    }

    override fun onResume() {
        super.onResume()
        loadUi()
        startPreviewIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        // The wallpaper continues independently in WallpaperService. Releasing
        // this player prevents audio/video from remaining active behind the UI.
        releasePreviewPlayer()
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        releasePreviewPlayer()
        super.onDestroy()
    }

    // ---------- source selection ----------

    private fun showUrlDialog() {
        val inputLayout = TextInputLayout(this).apply {
            hint = getString(R.string.video_url_hint)
            helperText = getString(R.string.video_url_helper)
        }
        val input = TextInputEditText(inputLayout.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(false)
            minLines = 2
            val currentSource = prefs.getString(Keys.VIDEO_URI, null)
            if (currentSource?.startsWith("http://") == true ||
                currentSource?.startsWith("https://") == true
            ) {
                setText(currentSource)
                setSelection(text?.length ?: 0)
            }
        }
        inputLayout.addView(input)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.use_video_url)
            .setMessage(R.string.video_url_explanation)
            .setView(inputLayout)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.use_url, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val rawUrl = input.text?.toString()?.trim().orEmpty()
                    val uri = rawUrl.takeIf(::isSupportedWebUrl)?.let(Uri::parse)
                    if (uri == null) {
                        inputLayout.error = getString(R.string.invalid_video_url)
                        return@setOnClickListener
                    }
                    inputLayout.error = null
                    saveVideoSource(uri)
                    dialog.dismiss()
                }
        }
        dialog.show()
    }

    private fun isSupportedWebUrl(value: String): Boolean {
        if (!URLUtil.isValidUrl(value)) return false
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
        return uri.scheme == "https" || uri.scheme == "http"
    }

    private fun saveVideoSource(uri: Uri) {
        releasePreviewPlayer()
        prefs.edit().putString(Keys.VIDEO_URI, uri.toString()).apply()
        loadUi()
        startPreviewIfNeeded()
    }

    // ---------- preview ----------

    /** Starts playback every time the activity becomes visible or source changes. */
    private fun startPreviewIfNeeded() {
        val uri = prefs.getString(Keys.VIDEO_URI, null)
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: run {
                showPreviewMessage(R.string.preview_hint, false)
                return
            }

        if (exoPlayer != null && previewUri == uri) return
        releasePreviewPlayer()
        createPreviewPlayer(uri)
    }

    private fun createPreviewPlayer(uri: Uri) {
        previewUri = uri
        showPreviewMessage(R.string.preview_loading, true)

        val player = ExoPlayer.Builder(this).build()
        exoPlayer = player
        binding.previewView.player = player
        binding.previewView.useController = true
        binding.previewView.controllerShowTimeoutMs = 2_500

        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = if (prefs.getBoolean(Keys.SOUND_ENABLED, true)) 1f else 0f
        player.setMediaItem(MediaItem.fromUri(uri))
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        showPreviewMessage(R.string.preview_ready, false)
                        player.play()
                    }
                    Player.STATE_BUFFERING -> {
                        showPreviewMessage(R.string.preview_loading, true)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                showPreviewMessage(R.string.preview_failed, true)
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.preview_error_title)
                    .setMessage(
                        getString(R.string.preview_error_message) + "\n\n" +
                            getString(R.string.preview_error_detail)
                    )
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        })
        player.prepare()
        player.playWhenReady = true
        applyPreviewFitMode()
    }

    private fun showPreviewMessage(textRes: Int, isVisible: Boolean) {
        binding.previewHintText.setText(textRes)
        binding.previewHintText.isVisible = isVisible
    }

    private fun applyPreviewFitMode() {
        val mode = prefs.getString(Keys.FIT_MODE, FitMode.AUTO.name)
            ?.let { runCatching { FitMode.valueOf(it) }.getOrNull() }
            ?: FitMode.AUTO
        binding.previewView.resizeMode = when (mode) {
            FitMode.AUTO, FitMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            FitMode.VERTICAL -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            FitMode.HORIZONTAL -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        }
    }

    private fun releasePreviewPlayer() {
        exoPlayer?.release()
        exoPlayer = null
        previewUri = null
        if (::binding.isInitialized) binding.previewView.player = null
    }

    // ---------- wallpaper ----------

    private fun launchLiveWallpaperChooser() {
        if (prefs.getString(Keys.VIDEO_URI, null) == null) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.no_video_title)
                .setMessage(R.string.no_video_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, VideoWallpaperService::class.java)
            )
        }
        runCatching { startActivity(intent) }.onFailure {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    // ---------- screen state ----------

    private fun loadUi() {
        val source = prefs.getString(Keys.VIDEO_URI, null)
        binding.videoInfoText.text = source?.let(::sourceDisplayName)
            ?: getString(R.string.no_video_selected)
        binding.setWallpaperButton.isEnabled = source != null

        val fitMode = prefs.getString(Keys.FIT_MODE, FitMode.AUTO.name)
            ?.let { runCatching { FitMode.valueOf(it) }.getOrNull() }
            ?: FitMode.AUTO
        val checkedId = when (fitMode) {
            FitMode.AUTO -> R.id.autoRadio
            FitMode.VERTICAL -> R.id.verticalRadio
            FitMode.HORIZONTAL -> R.id.horizontalRadio
            FitMode.CROP -> R.id.cropRadio
        }
        if (binding.fitModeGroup.checkedRadioButtonId != checkedId) {
            binding.fitModeGroup.check(checkedId)
        }
        applyPreviewFitMode()
    }

    override fun onSharedPreferenceChanged(
        preferences: SharedPreferences?,
        key: String?
    ) {
        if (key == Keys.FIT_MODE) applyPreviewFitMode()
    }

    private fun sourceDisplayName(source: String): String {
        val uri = Uri.parse(source)
        if (uri.scheme == "http" || uri.scheme == "https") {
            return getString(R.string.url_source_prefix, source)
        }
        return runCatching { queryDisplayName(uri) }.getOrDefault(source)
    }

    private fun queryDisplayName(uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index) ?: uri.lastPathSegment ?: uri.toString()
            } else {
                uri.lastPathSegment ?: uri.toString()
            }
        } ?: (uri.lastPathSegment ?: uri.toString())
    }
}
