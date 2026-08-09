package com.hotfixcde.motionwall

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

class MotionWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private lateinit var settings: SettingsManager
        private var player: ExoPlayer? = null
        private var surfaceHolder: SurfaceHolder? = null
        private var surfaceCreated = false
        private var visible = true
        private var currentUri = settingsUriOrNull()

        private fun settingsUriOrNull() = SettingsManager(applicationContext).videoUri

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            settings = SettingsManager(applicationContext)
            currentUri = settings.videoUri
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceHolder = holder
            surfaceCreated = true
            startOrReconnect()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceHolder = holder
            player?.setVideoSurfaceHolder(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceCreated = false
            if (player != null) {
                runCatching { player?.clearVideoSurfaceHolder(holder) }
            }
            surfaceHolder = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) {
                startOrReconnect()
            } else {
                player?.pause()
            }
        }

        override fun onDestroy() {
            releasePlayer()
            super.onDestroy()
        }

        private fun startOrReconnect() {
            if (!surfaceCreated || !visible) return
            val holder = surfaceHolder ?: return
            if (!holder.surface.isValid) return
            val uri = settings.videoUri ?: return

            if (player == null || currentUri != uri) {
                releasePlayer()
                currentUri = uri
                player = ExoPlayer.Builder(applicationContext).build().also { exo ->
                    exo.repeatMode = Player.REPEAT_MODE_ONE
                    exo.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    exo.volume = if (settings.audioEnabled) 1f else 0f
                    exo.setVideoSurfaceHolder(holder)
                    exo.setMediaItem(MediaItem.fromUri(uri))
                    exo.prepare()
                    exo.playWhenReady = true
                }
            } else {
                player?.setVideoSurfaceHolder(holder)
                player?.volume = if (settings.audioEnabled) 1f else 0f
                player?.playWhenReady = true
            }
        }

        private fun releasePlayer() {
            runCatching { player?.stop() }
            player?.release()
            player = null
        }
    }
}
