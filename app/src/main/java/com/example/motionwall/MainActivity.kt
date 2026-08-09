package com.example.motionwall

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.exoplayer.ExoPlayer
import com.example.motionwall.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Main screen of MotionWall.
 *
 * Very small and plain: pick a video, see it playing right here in the
 * app (same look the wallpaper will have), flip the sound switch, choose
 * a fit mode, and press one button to set it as the live wallpaper.
 */
class MainActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private var exoPlayer: ExoPlayer? = null
    private var previewUri: Uri? = null
    private var pendingPermissionUri: Uri? = null

    private val pickVideo =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            pendingPermissionUri = uri
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.onFailure {
                // Some providers do not grant persistable permission;
                // the preview still works for this session.
            }
            chooseVideo(uri)
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

        binding.setWallpaperButton.setOnClickListener {
            launchLiveWallpaperChooser()
        }

        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Keys.SOUND_ENABLED, isChecked).apply()
            applyPreviewVolume()
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
        stopPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        releasePreviewPlayer()
    }

    // ---------- preview ----------

    private fun chooseVideo(uri: Uri) {
        prefs.edit().putString(Keys.VIDEO_URI, uri.toString()).apply()
        loadUi()
        startPreviewIfNeeded()
    }

    private fun startPreviewIfNeeded() {
        if (previewUri == null || exoPlayer != null) return
        val uriString = prefs.getString(Keys.VIDEO_URI, null) ?: return
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        createPreviewPlayer(uri)
    }

    private fun createPreviewPlayer(uri: Uri) {
        previewUri = uri
        val player = ExoPlayer.Builder(this).build()
        exoPlayer = player

        binding.previewView.player = player
        binding.previewView.useController = true
        player.playWhenReady = true
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.setMediaItem(MediaItem.fromUri(uri))
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.preview_error_title)
                    .setMessage(R.string.preview_error_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        })
        player.prepare()

        applyPreviewFitMode()
        applyPreviewVolume()
    }

    private fun applyPreviewFitMode() {
        val player = exoPlayer ?: return
        val mode = prefs.getString(Keys.FIT_MODE, FitMode.AUTO.name)
            ?.let { runCatching { FitMode.valueOf(it) }.getOrNull() }
            ?: FitMode.AUTO
        val resizeMode = when (mode) {
            FitMode.AUTO, FitMode.CROP ->
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            FitMode.VERTICAL ->
                AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            FitMode.HORIZONTAL ->
                AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        }
        binding.previewView.resizeMode = resizeMode
    }

    private fun applyPreviewVolume() {
        val player = exoPlayer ?: return
        val enabled = prefs.getBoolean(Keys.SOUND_ENABLED, true)
        player.volume = if (enabled) 1f else 0f
    }

    private fun stopPreview() {
        releasePreviewPlayer()
    }

    private fun releasePreviewPlayer() {
        exoPlayer?.release()
        exoPlayer = null
        binding.previewView.player = null
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
            runCatching {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            }
        }
    }

    // ---------- ui ----------

    private fun loadUi() {
        val uriText = prefs.getString(Keys.VIDEO_URI, null)
        binding.videoInfoText.text = uriText?.let {
            runCatching { queryDisplayName(Uri.parse(it)) }
                .getOrNull() ?: it
        } ?: getString(R.string.no_video_selected)

        binding.soundSwitch.isChecked = prefs.getBoolean(Keys.SOUND_ENABLED, true)

        val fitMode = prefs.getString(Keys.FIT_MODE, FitMode.AUTO.name)
            ?: FitMode.AUTO.name
        val checkedId = when (FitMode.valueOf(fitMode)) {
            FitMode.AUTO -> R.id.autoRadio
            FitMode.VERTICAL -> R.id.verticalRadio
            FitMode.HORIZONTAL -> R.id.horizontalRadio
            FitMode.CROP -> R.id.cropRadio
        }
        if (binding.fitModeGroup.checkedRadioButtonId != checkedId) {
            binding.fitModeGroup.check(checkedId)
        }

        binding.setWallpaperButton.isEnabled = uriText != null
        applyPreviewFitMode()
        applyPreviewVolume()
    }

    override fun onSharedPreferenceChanged(
        preferences: SharedPreferences?,
        key: String?
    ) {
        when (key) {
            Keys.FIT_MODE -> applyPreviewFitMode()
            Keys.SOUND_ENABLED -> applyPreviewVolume()
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(
                android.provider.OpenableColumns.DISPLAY_NAME
            )
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index) ?: uri.lastPathSegment ?: uri.toString()
            } else {
                uri.lastPathSegment ?: uri.toString()
            }
        } ?: (uri.lastPathSegment ?: uri.toString())
    }
}
