package com.hotfixcde.motionwall

import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class MotionWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = MotionEngine()

    private inner class MotionEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var player: MediaPlayer? = null
        private var visible = false
        private var surfaceReady = false
        private var currentUri: Uri? = null
        private val prefs = getSharedPreferences(MotionSettingsStore.PREFS_NAME, MODE_PRIVATE)

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) startOrResumePlayback() else pausePlayback()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            if (visible) startOrResumePlayback()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            applyCurrentSettings()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            releasePlayer()
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                MotionSettingsStore.KEY_VIDEO -> restartPlayback()
                MotionSettingsStore.KEY_SOUND,
                MotionSettingsStore.KEY_SCALE,
                MotionSettingsStore.KEY_ORIENTATION -> applyCurrentSettings()
            }
        }

        private fun startOrResumePlayback() {
            if (!surfaceReady || !surfaceHolder.surface.isValid) return
            val uri = storedUri() ?: return

            if (player == null || uri != currentUri) {
                restartPlayback()
                return
            }

            applyCurrentSettings()
            runCatching {
                if (player?.isPlaying == false) player?.start()
            }
        }

        private fun pausePlayback() {
            runCatching { player?.pause() }
        }

        private fun restartPlayback() {
            releasePlayer()
            if (visible && surfaceReady && surfaceHolder.surface.isValid) {
                startPlayback()
            }
        }

        private fun startPlayback() {
            val uri = storedUri() ?: return
            if (!surfaceReady || !surfaceHolder.surface.isValid) return

            currentUri = uri
            player = MediaPlayer().apply {
                setDataSource(this@MotionWallpaperService, uri)
                setSurface(surfaceHolder.surface)
                isLooping = true

                setOnPreparedListener { preparedPlayer ->
                    applyCurrentSettings()
                    if (visible) runCatching { preparedPlayer.start() }
                }

                setOnErrorListener { _, _, _ ->
                    releasePlayer()
                    true
                }

                prepareAsync()
            }
        }

        private fun applyCurrentSettings() {
            val currentPlayer = player ?: return

            val soundEnabled = prefs.getBoolean(MotionSettingsStore.KEY_SOUND, false)
            val volume = if (soundEnabled) 1f else 0f
            runCatching { currentPlayer.setVolume(volume, volume) }

            // CROP keeps the original aspect ratio and fills the wallpaper surface.
            // FIT shows the complete frame without changing the source video itself.
            val scaleMode = if (prefs.getInt(MotionSettingsStore.KEY_SCALE, 0) == 1) {
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
            } else {
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
            runCatching { currentPlayer.setVideoScalingMode(scaleMode) }
        }

        private fun storedUri(): Uri? = prefs.getString(MotionSettingsStore.KEY_VIDEO, null)
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)

        private fun releasePlayer() {
            runCatching { player?.reset() }
            player?.release()
            player = null
            currentUri = null
        }
    }
}
