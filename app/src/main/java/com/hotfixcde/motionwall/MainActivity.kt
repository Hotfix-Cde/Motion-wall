package com.hotfixcde.motionwall

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.hotfixcde.motionwall.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsManager
    private var previewPlayer: ExoPlayer? = null

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        settings.videoUri = uri
        loadPreview()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = SettingsManager(applicationContext)
        setupUi()
    }

    private fun setupUi() {
        binding.btnSelectVideo.setOnClickListener {
            picker.launch(arrayOf("video/*"))
        }

        binding.switchSound.isChecked = settings.audioEnabled
        binding.switchSound.setOnCheckedChangeListener { _, enabled ->
            settings.audioEnabled = enabled
            previewPlayer?.volume = if (enabled) 1f else 0f
        }

        binding.orientationSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Auto", "Vertical", "Horizontal")
        )
        binding.orientationSpinner.setSelection(settings.orientationMode, false)
        binding.orientationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (settings.orientationMode == position) {
                    applyPreviewMode()
                    return
                }
                settings.orientationMode = position
                applyPreviewMode()
            }
        }

        binding.btnSetWallpaper.setOnClickListener {
            if (settings.videoUri == null) {
                binding.tvStatus.setText(R.string.choose_video_first)
                return@setOnClickListener
            }
            val component = ComponentName(this, MotionWallpaperService::class.java)
            runCatching {
                startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                })
            }.onFailure {
                runCatching {
                    startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                }.onFailure {
                    binding.tvStatus.setText(R.string.wallpaper_not_supported)
                }
            }
        }
    }

    private fun loadPreview() {
        val uri = settings.videoUri
        if (uri == null) {
            binding.playerView.visibility = View.INVISIBLE
            binding.tvPlaceholder.visibility = View.VISIBLE
            binding.tvStatus.setText(R.string.no_video_selected)
            return
        }

        binding.playerView.visibility = View.VISIBLE
        binding.tvPlaceholder.visibility = View.GONE
        binding.tvStatus.setText(R.string.preview_ready)

        if (previewPlayer == null) {
            previewPlayer = ExoPlayer.Builder(this).build().also { player ->
                player.repeatMode = Player.REPEAT_MODE_ONE
                binding.playerView.player = player
            }
        }

        previewPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(uri))
            volume = if (settings.audioEnabled) 1f else 0f
            prepare()
            playWhenReady = true
        }
        applyPreviewMode()
    }

    private fun applyPreviewMode() {
        // The preview uses the same fill/crop strategy as the wallpaper.
        // We intentionally do not rotate the video. "Vertical" and "Horizontal"
        // control framing rather than altering the source file's orientation.
        binding.playerView.resizeMode = when (settings.orientationMode) {
            1, 2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }

    override fun onStart() {
        super.onStart()
        if (::settings.isInitialized && settings.videoUri != null) loadPreview()
    }

    override fun onStop() {
        previewPlayer?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        binding.playerView.player = null
        previewPlayer?.release()
        previewPlayer = null
        super.onDestroy()
    }
}
