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
            if (visible) startPlayback() else stopPlayback()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            if (visible) startPlayback()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopPlayback()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            stopPlayback()
            super.onDestroy()
        }

        private fun startPlayback() {
            val value = getSharedPreferences("motionwall", MODE_PRIVATE).getString("video", null) ?: return
            val uri = Uri.parse(value)
            if (player == null) {
                player = MediaPlayer().apply {
                    setDataSource(this@MotionWallpaperService, uri)
                    setSurface(surfaceHolder.surface)
                    isLooping = true
                    val sound = getSharedPreferences("motionwall", MODE_PRIVATE).getBoolean("sound", false)
                    setVolume(if (sound) 1f else 0f, if (sound) 1f else 0f)
                    setOnPreparedListener { if (visible) start() }
                    setOnErrorListener { _, _, _ -> true }
                    prepareAsync()
                }
            } else {
                player?.setSurface(surfaceHolder.surface)
                if (!player!!.isPlaying && visible) player?.start()
            }
        }

        private fun stopPlayback() {
            player?.pause()
        }
    }
}
