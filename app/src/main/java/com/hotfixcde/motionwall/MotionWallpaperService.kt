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
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private val prefs = getSharedPreferences(MotionSettingsStore.PREFS_NAME, MODE_PRIVATE)

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) startOrResumePlayback() else player?.pause()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            if (visible) startOrResumePlayback()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            applySurfaceGeometry()
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
                MotionSettingsStore.KEY_SOUND -> applySoundSetting()
                MotionSettingsStore.KEY_SCALE,
                MotionSettingsStore.KEY_ORIENTATION -> {
                    applySurfaceGeometry()
                    applyCurrentSettings()
                }
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
            if (!player!!.isPlaying) player?.start()
        }

        private fun restartPlayback() {
            releasePlayer()
            if (visible && surfaceReady) startPlayback()
        }

        private fun startPlayback() {
            val uri = storedUri() ?: return
            currentUri = uri

            player = MediaPlayer().apply {
                setDataSource(this@MotionWallpaperService, uri)
                setSurface(surfaceHolder.surface)
                // Let MediaPlayer handle the repeat directly. Do not manually seek on
                // completion because that can introduce a visible pause at the loop.
                isLooping = true
                setOnPreparedListener { preparedPlayer ->
                    videoWidth = preparedPlayer.videoWidth
                    videoHeight = preparedPlayer.videoHeight
                    applyCurrentSettings()
                    applySurfaceGeometry()
                    if (visible) preparedPlayer.start()
                }
                setOnVideoSizeChangedListener { _, width, height ->
                    videoWidth = width
                    videoHeight = height
                    applySurfaceGeometry()
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
            applySoundSetting()
            runCatching {
                currentPlayer.setVideoScalingMode(
                    if (prefs.getInt(MotionSettingsStore.KEY_SCALE, 0) == 1) {
                        MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    } else {
                        MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    },
                )
            }
        }

        private fun applySoundSetting() {
            val currentPlayer = player ?: return
            val enabled = prefs.getBoolean(MotionSettingsStore.KEY_SOUND, false)
            runCatching {
                currentPlayer.setVolume(if (enabled) 1f else 0f, if (enabled) 1f else 0f)
            }
        }

        /**
         * Never use the video's native dimensions as the wallpaper surface size.
         * The old implementation did that, which could force Android to rescale the
         * wallpaper buffer and made the orientation controls ineffective.
         *
         * Auto keeps the device surface ratio. Vertical/horizontal select a portrait
         * or landscape 16:9 buffer so the crop/fit setting has a meaningful target.
         */
        private fun applySurfaceGeometry() {
            if (!surfaceReady || surfaceWidth <= 0 || surfaceHeight <= 0) return

            val mode = when (prefs.getInt(MotionSettingsStore.KEY_ORIENTATION, 0)) {
                1 -> OrientationMode.VERTICAL
                2 -> OrientationMode.HORIZONTAL
                else -> OrientationMode.AUTO
            }

            val (targetWidth, targetHeight) = when (mode) {
                OrientationMode.VERTICAL -> {
                    val h = maxOf(surfaceWidth, surfaceHeight)
                    maxOf(1, (h * 9f / 16f).toInt()) to h
                }
                OrientationMode.HORIZONTAL -> {
                    val w = maxOf(surfaceWidth, surfaceHeight)
                    w to maxOf(1, (w * 9f / 16f).toInt())
                }
                OrientationMode.AUTO -> surfaceWidth to surfaceHeight
            }

            if (targetWidth > 0 && targetHeight > 0) {
                runCatching { surfaceHolder.setFixedSize(targetWidth, targetHeight) }
            }
        }

        private fun storedUri(): Uri? = prefs.getString(MotionSettingsStore.KEY_VIDEO, null)
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)

        private fun releasePlayer() {
            runCatching { player?.reset() }
            player?.release()
            player = null
            currentUri = null
            videoWidth = 0
            videoHeight = 0
        }
    }
}
