package com.hotfixcde.motionwall

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MotionWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private val settings by lazy(LazyThreadSafetyMode.NONE) { SettingsManager(applicationContext) }
        private var player: ExoPlayer? = null
        private var holder: SurfaceHolder? = null
        private var surfaceReady = false
        private var visible = true
        private var currentUri = settings.videoUri

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            holder = surfaceHolder
        }

        override fun onSurfaceCreated(surfaceHolder: SurfaceHolder) {
            super.onSurfaceCreated(surfaceHolder)
            holder = surfaceHolder
            surfaceReady = surfaceHolder.surface.isValid
            startOrReconnect()
        }

        override fun onSurfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(surfaceHolder, format, width, height)
            holder = surfaceHolder
            if (surfaceHolder.surface.isValid) {
                player?.setVideoSurfaceHolder(surfaceHolder)
                applySettings()
                if (visible) player?.playWhenReady = true
            }
        }

        override fun onSurfaceDestroyed(surfaceHolder: SurfaceHolder) {
            surfaceReady = false
            runCatching { player?.clearVideoSurfaceHolder(surfaceHolder) }
            holder = null
            super.onSurfaceDestroyed(surfaceHolder)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) {
                startOrReconnect()
            } else {
                player?.playWhenReady = false
            }
        }

        override fun onDestroy() {
            surfaceReady = false
            releasePlayer()
            holder = null
            super.onDestroy()
        }

        private fun startOrReconnect() {
            if (!surfaceReady || !visible) return
            val surfaceHolder = holder ?: return
            if (!surfaceHolder.surface.isValid) return

            val uri = settings.videoUri ?: return
            if (player == null || currentUri != uri) {
                releasePlayer()
                currentUri = uri
                player = ExoPlayer.Builder(applicationContext).build().also { exo ->
                    exo.repeatMode = Player.REPEAT_MODE_ONE
                    exo.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    exo.setVideoSurfaceHolder(surfaceHolder)
                    exo.volume = if (settings.audioEnabled) 1f else 0f
                    exo.setMediaItem(MediaItem.fromUri(uri))
                    exo.prepare()
                    exo.playWhenReady = true
                }
                return
            }

            applySettings()
            player?.setVideoSurfaceHolder(surfaceHolder)
            player?.playWhenReady = true
        }

        private fun applySettings() {
            val exo = player ?: return
            exo.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            exo.volume = if (settings.audioEnabled) 1f else 0f
        }

        private fun releasePlayer() {
            runCatching { player?.stop() }
            player?.release()
            player = null
            currentUri = settings.videoUri
        }
    }
}
