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

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) startPlayback() else runCatching { player?.pause() }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            if (visible) startPlayback()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            applyScaling()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            releasePlayer()
            super.onDestroy()
        }

        private fun startPlayback() {
            if (!surfaceHolder.surface.isValid) return
            val prefs = getSharedPreferences("motionwall", MODE_PRIVATE)
            val value = prefs.getString("video", null) ?: return
            val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return

            if (player != null) {
                runCatching { player?.setSurface(surfaceHolder.surface) }
                applySettings()
                runCatching { if (visible && player?.isPlaying == false) player?.start() }
                return
            }

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
            val prefs = getSharedPreferences("motionwall", MODE_PRIVATE)
            val enabled = prefs.getBoolean("sound", false)
            runCatching { player?.setVolume(if (enabled) 1f else 0f, if (enabled) 1f else 0f) }
            applyScaling()
        }

        private fun applyScaling() {
            val crop = getSharedPreferences("motionwall", MODE_PRIVATE).getInt("scale", 0) == 0
            runCatching {
                player?.setVideoScalingMode(
                    if (crop) MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    else MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                )
            }
        }

        private fun releasePlayer() {
            runCatching { player?.reset() }
            player?.release()
            player = null
        }
    }
}
