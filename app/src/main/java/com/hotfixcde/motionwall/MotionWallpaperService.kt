package com.hotfixcde.motionwall

import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class MotionWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = MotionEngine()

    private inner class MotionEngine : Engine() {
        private var player: MediaPlayer? = null
        private var visible = false
        private var surfaceReady = false
        private var currentUri: Uri? = null

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) startOrResume() else runCatching { player?.pause() }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            startOrResume()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            applySettings()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            releasePlayer()
            super.onDestroy()
        }

        private fun prefs() = getSharedPreferences("motionwall", MODE_PRIVATE)

        private fun storedUri(): Uri? = prefs().getString("video", null)
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }

        private fun startOrResume() {
            if (!surfaceReady || !surfaceHolder.surface.isValid) return
            val uri = storedUri() ?: return

            if (player == null || currentUri != uri) {
                createPlayer(uri)
                return
            }

            applySettings()
            if (visible) runCatching { if (player?.isPlaying == false) player?.start() }
        }

        private fun createPlayer(uri: Uri) {
            releasePlayer()
            if (!surfaceReady || !surfaceHolder.surface.isValid) return

            currentUri = uri
            val mp = MediaPlayer()
            player = mp
            try {
                mp.setDataSource(this@MotionWallpaperService, uri)
                mp.setSurface(surfaceHolder.surface)
                mp.isLooping = true
                mp.setOnPreparedListener { prepared ->
                    applySettings()
                    if (visible) runCatching { prepared.start() }
                }
                mp.setOnErrorListener { _, _, _ ->
                    releasePlayer()
                    true
                }
                mp.prepareAsync()
            } catch (_: Exception) {
                releasePlayer()
            }
        }

        private fun applySettings() {
            val mp = player ?: return
            val p = prefs()
            val sound = p.getBoolean("sound", false)
            runCatching { mp.setVolume(if (sound) 1f else 0f, if (sound) 1f else 0f) }

            // The wallpaper surface already has the device's actual screen dimensions.
            // Let MediaPlayer preserve the source aspect ratio and crop to fill it.
            val crop = p.getInt("scale", 0) == 0
            runCatching {
                mp.setVideoScalingMode(
                    if (crop) MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    else MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                )
            }
        }

        private fun releasePlayer() {
            runCatching { player?.stop() }
            player?.release()
            player = null
            currentUri = null
        }
    }
}
