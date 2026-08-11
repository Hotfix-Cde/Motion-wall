package com.example.motionwall

import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

/**
 * MotionWall wallpaper service.
 *
 * The wallpaper uses the same Media3 streaming engine family as the in-app
 * preview. It sends the source directly to the decoder: no compression,
 * conversion, or re-encoding is performed. For adaptive sources, the track
 * selector is configured to prefer the highest supported video representation.
 *
 * Fit modes preserve the source aspect ratio. AUTO and CROP use center-cover;
 * VERTICAL prefers filling the screen height; HORIZONTAL prefers filling the
 * screen width. No mode stretches the video.
 */
class VideoWallpaperService : WallpaperService() {

    companion object {
        private const val LOG_TAG = "MotionWall"
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine :
        Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val prefs = getSharedPreferences(Keys.PREFS, MODE_PRIVATE)
        private var player: ExoPlayer? = null
        private var holderRef: SurfaceHolder? = null
        private var visible = false
        private var videoWidth = 0
        private var videoHeight = 0
        private var pixelWidthHeightRatio = 1f
        private var prepared = false
        private var playbackUri: Uri? = null
        private var resolvingPage = false
        private var resolutionToken = 0L

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        // ---------- surface life cycle ----------

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            holderRef = surfaceHolder
            surfaceHolder.setFormat(PixelFormat.RGBA_8888)
            setTouchEventsEnabled(false)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            holderRef = holder
            player?.setVideoSurface(holder.surface)
            maybeStart()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        // ---------- visibility life cycle ----------

        override fun onVisibilityChanged(newVisible: Boolean) {
            super.onVisibilityChanged(newVisible)
            visible = newVisible
            if (visible) {
                maybeStart()
            } else {
                runCatching {
                    player?.playWhenReady = false
                    player?.pause()
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            releasePlayer()
        }

        // ---------- preferences ----------

        override fun onSharedPreferenceChanged(
            preferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                Keys.VIDEO_URI -> {
                    releasePlayer()
                    maybeStart()
                }
                Keys.SOUND_ENABLED -> applyVolume()
                Keys.FIT_MODE -> applyCrop()
            }
        }

        // ---------- playback ----------

        private fun maybeStart() {
            if (!visible) return
            val sourceUri = prefs.getString(Keys.VIDEO_URI, null)
                ?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return

            if (player == null && !resolvingPage) {
                if (VideoSourceResolver.requiresPageResolution(sourceUri)) {
                    resolvePageSource(sourceUri)
                } else {
                    preparePlayer(sourceUri)
                }
            } else if (player != null) {
                player?.playWhenReady = true
                runCatching { player?.play() }
            }
        }

        private fun resolvePageSource(sourceUri: Uri) {
            resolvingPage = true
            val token = ++resolutionToken
            VideoSourceResolver.resolveForPlayback(applicationContext, sourceUri) { resolved ->
                resolvingPage = false
                if (!visible || token != resolutionToken) return@resolveForPlayback
                if (resolved == null) {
                    Log.w(LOG_TAG, "Social page did not expose a playable video")
                } else {
                    preparePlayer(resolved)
                }
            }
        }

        private fun preparePlayer(uri: Uri) {
            val holder = holderRef ?: return

            runCatching {
                val exo = MediaPlaybackFactory.createPlayer(this@VideoWallpaperService)
                player = exo
                playbackUri = uri
                prepared = false
                videoWidth = 0
                videoHeight = 0
                pixelWidthHeightRatio = 1f

                exo.setVideoSurface(holder.surface)
                exo.repeatMode = Player.REPEAT_MODE_ONE
                exo.volume = if (prefs.getBoolean(Keys.SOUND_ENABLED, true)) 1f else 0f
                exo.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            prepared = true
                            applyCrop()
                            applyVolume()
                            if (visible) exo.playWhenReady = true
                        }
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        videoWidth = videoSize.width
                        videoHeight = videoSize.height
                        pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio
                        applyCrop()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.w(
                            LOG_TAG,
                            "Media3 wallpaper playback failed: ${error.errorCodeName}",
                            error
                        )
                        releasePlayer()
                    }
                })
                exo.setMediaItem(MediaPlaybackFactory.mediaItem(uri))
                exo.prepare()
                exo.playWhenReady = visible
            }.onFailure {
                Log.e(LOG_TAG, "Failed to start video wallpaper", it)
                releasePlayer()
            }
        }

        /** Apply the selected aspect-ratio-preserving fit mode. */
        private fun applyCrop() {
            val exo = player ?: return
            if (!prepared || videoWidth == 0 || videoHeight == 0) return

            val fitMode = prefs.getString(Keys.FIT_MODE, FitMode.AUTO.name)
                ?.let { runCatching { FitMode.valueOf(it) }.getOrNull() }
                ?: FitMode.AUTO

            val useCover = when (fitMode) {
                FitMode.AUTO, FitMode.CROP -> true
                FitMode.VERTICAL -> videoRatio > screenRatio
                FitMode.HORIZONTAL -> videoRatio < screenRatio
            }

            runCatching {
                exo.setVideoScalingMode(
                    if (useCover) {
                        C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    } else {
                        C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    }
                )
            }
        }

        private val screenRatio: Float
            get() {
                val holder = holderRef ?: return 1f
                val w = holder.surfaceFrame.width().toFloat()
                val h = holder.surfaceFrame.height().toFloat()
                return if (w > 0f && h > 0f) w / h else 1f
            }

        private val videoRatio: Float
            get() {
                val pixelRatio = if (pixelWidthHeightRatio > 0f) {
                    pixelWidthHeightRatio
                } else {
                    1f
                }
                return if (videoHeight > 0) {
                    videoWidth.toFloat() * pixelRatio / videoHeight
                } else {
                    1f
                }
            }

        private fun applyVolume() {
            val enabled = prefs.getBoolean(Keys.SOUND_ENABLED, true)
            runCatching { player?.volume = if (enabled) 1f else 0f }
        }

        private fun releasePlayer() {
            resolutionToken += 1
            resolvingPage = false
            runCatching { player?.release() }
            player = null
            playbackUri = null
            prepared = false
            videoWidth = 0
            videoHeight = 0
            pixelWidthHeightRatio = 1f
        }
    }
}
