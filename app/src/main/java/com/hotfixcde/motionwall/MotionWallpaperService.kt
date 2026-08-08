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
        private var videoWidth = 0
        private var videoHeight = 0
        private val prefs = getSharedPreferences(MotionSettingsStore.PREFS_NAME, MODE_PRIVATE)

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                startOrResumePlayback()
            } else {
                player?.pause()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            if (visible) {
                startOrResumePlayback()
            }
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
            if (!surfaceReady || !surfaceHolder.surface.isValid) {
                return
            }

            val uri = prefs.getString(MotionSettingsStore.KEY_VIDEO, null)?.takeIf { it.isNotBlank() }?.let(Uri::parse)
                ?: return

            if (player == null || uri != currentUri) {
                restartPlayback()
                return
            }

            applyCurrentSettings()
            if (!player!!.isPlaying) {
                player?.start()
            }
        }

        private fun restartPlayback() {
            releasePlayer()
            if (visible && surfaceReady) {
                startPlayback()
            }
        }

        private fun startPlayback() {
            val uri = prefs.getString(MotionSettingsStore.KEY_VIDEO, null)?.takeIf { it.isNotBlank() }?.let(Uri::parse)
                ?: return
            currentUri = uri

            player = MediaPlayer().apply {
                setDataSource(this@MotionWallpaperService, uri)
                setSurface(surfaceHolder.surface)
                isLooping = true
                setOnPreparedListener { preparedPlayer ->
                    videoWidth = preparedPlayer.videoWidth
                    videoHeight = preparedPlayer.videoHeight
                    applyCurrentSettings()
                    applySurfaceGeometry()
                    preparedPlayer.start()
                }
                setOnVideoSizeChangedListener { _, width, height ->
                    videoWidth = width
                    videoHeight = height
                    applySurfaceGeometry()
                }
                setOnCompletionListener { completedPlayer ->
                    completedPlayer.seekTo(0)
                    if (!completedPlayer.isPlaying) {
                        completedPlayer.start()
                    }
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
            val scaleMode = when (prefs.getInt(MotionSettingsStore.KEY_SCALE, 0)) {
                1 -> ScaleMode.FIT
                else -> ScaleMode.CROP
            }
            currentPlayer.setVolume(if (soundEnabled) 1f else 0f, if (soundEnabled) 1f else 0f)
            currentPlayer.setVideoScalingMode(
                if (scaleMode == ScaleMode.FIT) {
                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                } else {
                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                }
            )
        }

        private fun applySurfaceGeometry() {
            if (videoWidth <= 0 || videoHeight <= 0) {
                return
            }

            val orientationMode = when (prefs.getInt(MotionSettingsStore.KEY_ORIENTATION, 0)) {
                1 -> OrientationMode.VERTICAL
                2 -> OrientationMode.HORIZONTAL
                else -> OrientationMode.AUTO
            }

            val shouldRotate = when (orientationMode) {
                OrientationMode.AUTO -> false
                OrientationMode.VERTICAL -> videoWidth > videoHeight
                OrientationMode.HORIZONTAL -> videoHeight > videoWidth
            }

            val fixedWidth = if (shouldRotate) videoHeight else videoWidth
            val fixedHeight = if (shouldRotate) videoWidth else videoHeight
            runCatching {
                surfaceHolder.setFixedSize(fixedWidth.coerceAtLeast(1), fixedHeight.coerceAtLeast(1))
            }
        }

        private fun releasePlayer() {
            player?.release()
            player = null
            currentUri = null
            videoWidth = 0
            videoHeight = 0
        }
    }
}
