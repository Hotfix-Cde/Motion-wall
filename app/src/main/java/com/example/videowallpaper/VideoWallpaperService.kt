package com.example.videowallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val prefs: SharedPreferences = getSharedPreferences(Keys.PREFS, MODE_PRIVATE)
        private var mediaPlayer: MediaPlayer? = null
        private var currentUri: Uri? = null
        private var visible = false
        private var holderRef: SurfaceHolder? = null

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            holderRef = surfaceHolder
            surfaceHolder.setFormat(PixelFormat.RGBA_8888)
            isTouchEventsEnabled = false
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            holderRef = holder
            maybeStart()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                maybeStart()
            } else {
                mediaPlayer?.pause()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            releasePlayer()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            when (key) {
                Keys.VIDEO_URI -> {
                    releasePlayer()
                    maybeStart()
                }
                Keys.SOUND_ENABLED, Keys.SCALE_MODE -> applySettings()
            }
        }

        private fun maybeStart() {
            if (!visible) return
            val uriString = prefs.getString(Keys.VIDEO_URI, null) ?: return
            val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
            currentUri = uri

            if (mediaPlayer == null) {
                preparePlayer(uri)
            } else {
                applySettings()
                if (mediaPlayer?.isPlaying != true) {
                    runCatching { mediaPlayer?.start() }
                }
            }
        }

        private fun preparePlayer(uri: Uri) {
            val holder = holderRef ?: return

            runCatching {
                val player = MediaPlayer()
                mediaPlayer = player

                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                player.isLooping = true
                player.setVolume(1f, 1f)
                player.setSurface(holder.surface)
                player.setDataSource(applicationContext, uri)

                player.setOnPreparedListener {
                    applySettings()
                    if (visible) {
                        runCatching { it.start() }
                    }
                }

                player.setOnErrorListener { _, what, extra ->
                    Log.w("VideoWallpaper", "MediaPlayer error what=$what extra=$extra")
                    releasePlayer()
                    true
                }

                player.prepareAsync()
            }.onFailure {
                Log.e("VideoWallpaper", "Failed to start video wallpaper", it)
                releasePlayer()
            }
        }

        private fun applySettings() {
            val player = mediaPlayer ?: return

            val soundEnabled = prefs.getBoolean(Keys.SOUND_ENABLED, true)
            val scaleMode = prefs.getString(Keys.SCALE_MODE, ScaleMode.FIT.name) ?: ScaleMode.FIT.name

            val volume = if (soundEnabled) 1f else 0f
            runCatching { player.setVolume(volume, volume) }

            val scalingMode = if (scaleMode == ScaleMode.CROP.name) {
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            } else {
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }

            runCatching { player.setVideoScalingMode(scalingMode) }
        }

        private fun releasePlayer() {
            runCatching {
                mediaPlayer?.setOnPreparedListener(null)
                mediaPlayer?.setOnErrorListener(null)
                mediaPlayer?.setOnCompletionListener(null)
                mediaPlayer?.stop()
            }
            runCatching { mediaPlayer?.reset() }
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
        }
    }
}
