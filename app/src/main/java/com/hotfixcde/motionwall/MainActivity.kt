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
import com.hotfixcde.motionwall.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsManager
    private var previewPlayer: ExoPlayer? = null

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some providers do not support persistable permissions. The URI may still be usable now.
        }
        settings.videoUri = uri
        loadPreview()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = SettingsManager(this)
        setupUi()
    }

    private fun setupUi() {
        binding.btnSelectVideo.setOnClickListener {
            picker.launch(arrayOf("video/*"))
        }

        binding.switchSound.isChecked = settings.audioEnabled
        binding.switchSound.setOnCheckedChangeListener { _, enabled ->
            settings.audioEnabled = enabled
            updateVolume()
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
                settings.orientationMode = position
                applyPreviewOrientation()
            }
        }

        binding.btnSetWallpaper.setOnClickListener {
            if (settings.videoUri == null) {
                binding.tvStatus.text = getString(R.string.choose_video_first)
                return@setOnClickListener
            }
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, MotionWallpaperService::class.java)
                )
            }
            runCatching { startActivity(intent) }.onFailure {
                runCatching { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
                    .onFailure { binding.tvStatus.text = getString(R.string.wallpaper_not_supported) }
            }
        }
    }

    private fun loadPreview() {
        val uri = settings.videoUri
        if (uri == null) {
            binding.playerView.visibility = View.INVISIBLE
            binding.tvPlaceholder.visibility = View.VISIBLE
            binding.tvStatus.text = getString(R.string.no_video_selected)
            return
        }

        binding.playerView.visibility = View.VISIBLE
        binding.tvPlaceholder.visibility = View.GONE
        binding.tvStatus.text = getString(R.string.preview_ready)

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
        applyPreviewOrientation()
    }

    private fun updateVolume() {
        previewPlayer?.volume = if (settings.audioEnabled) 1f else 0f
    }

    private fun applyPreviewOrientation() {
        val mode = settings.orientationMode
        binding.playerView.rotation = when (mode) {
            1 -> 90f
            2 -> 0f
            else -> 0f
        }
    }

    override fun onStart() {
        super.onStart()
        if (settings.videoUri != null) loadPreview()
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
