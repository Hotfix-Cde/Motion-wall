package com.example.motionwall

import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

/**
 * MotionWall wallpaper service.
 *
 * Plays the user's video file directly from its URI with [MediaPlayer] --
 * the file is never re-encoded or compressed, so the original quality is
 * preserved. The video loops with [MediaPlayer.setLooping], which restarts
 * sample-accurately so there is no noticeable pause between loops.
 *
 * Fit modes (never stretches the video):
 * - AUTO / CROP : fills the whole screen, crops the excess edges
 *                 (center cover).
 * - VERTICAL    : makes the video fill the screen height. When the video
 *                 is wider than that allows, the left/right edges are
 *                 cropped; when it is taller, the top/bottom are shown
 *                 in full and the sides stay centered (no stretching).
 * - HORIZONTAL  : the opposite of VERTICAL.
 */
class VideoWallpaperService : WallpaperService() {

    companion object {
        private const val LOG_TAG = "MotionWall"
        private const val MODE_FIT =
            MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
        private const val MODE_COVER =
            MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine :
        Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val prefs = getSharedPreferences(Keys.PREFS, MODE_PRIVATE)
        private var player: MediaPlayer? = null
        private var holderRef: SurfaceHolder? = null
        private var visible = false
        private var videoWidth = 0
        private var videoHeight = 0
        private var prepared = false

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
                runCatching { player?.pause() }
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
            val uri = prefs.getString(Keys.VIDEO_URI, null)
                ?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return

            if (player == null) {
                preparePlayer(uri)
            } else {
                applyVolume()
                if (player?.isPlaying != true) {
                    runCatching { player?.start() }
                }
            }
        }

        private fun preparePlayer(uri: Uri) {
            val holder = holderRef ?: return

            runCatching {
                val mp = MediaPlayer()
                player = mp

                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                mp.isLooping = true
                mp.setSurface(holder.surface)
                mp.setDataSource(applicationContext, uri)

                mp.setOnPreparedListener {
                    prepared = true
                    readVideoSize()
                    applyCrop()
                    applyVolume()
                    if (visible) runCatching { it.start() }
                }
                mp.setOnErrorListener { _, what, extra ->
                    Log.w(LOG_TAG, "MediaPlayer error what=$what extra=$extra")
                    releasePlayer()
                    true
                }
                mp.prepareAsync()
            }.onFailure {
                Log.e(LOG_TAG, "Failed to start video wallpaper", it)
                releasePlayer()
            }
        }

        /** Read the real video dimensions for crop math. */
        private fun readVideoSize() {
            val mp = player ?: return
            if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                videoWidth = mp.videoWidth
                videoHeight = mp.videoHeight
                return
            }
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(
                    applicationContext,
                    prefs.getString(Keys.VIDEO_URI, null)
                        ?.let { Uri.parse(it) }
                )
                val w = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull() ?: 0
                retriever.release()
                if (w > 0 && h > 0) {
                    videoWidth = w
                    videoHeight = h
                }
            }.onFailure { Log.w(LOG_TAG, "Could not read video size", it) }
        }

        /**
         * Apply the chosen fit mode. The wallpaper surface window is the
         * full screen, so cropping is done with MediaPlayer's built-in
         * center-cover scaling where it matches the mode, and otherwise
         * the video is letterboxed (never stretched).
         */
        private fun applyCrop() {
            val mp = player ?: return
            if (!prepared) return

            readVideoSize()
            if (videoWidth == 0 || videoHeight == 0) {
                runCatching { mp.setVideoScalingMode(MODE_FIT) }
                return
            }

            val fitMode = prefs.getString(Keys.FIT_MODE, FitMode.AUTO.name)
                ?.let { runCatching { FitMode.valueOf(it) }.getOrNull() }
                ?: FitMode.AUTO

            val useCover = when (fitMode) {
                FitMode.AUTO, FitMode.CROP -> true
                // VERTICAL covers (crops the sides) only when the video
                // is wider than the screen; otherwise filling the height
                // already fits inside the width, so no crop is needed
                // and cover would wrongly crop the top/bottom.
                FitMode.VERTICAL -> videoRatio > screenRatio
                // HORIZONTAL covers (crops top/bottom) only when the
                // video is taller than the screen.
                FitMode.HORIZONTAL -> videoRatio < screenRatio
            }

            runCatching {
                mp.setVideoScalingMode(
                    if (useCover) MODE_COVER else MODE_FIT
                )
            }
        }

        private val screenRatio: Float
            get() {
                val holder = holderRef ?: return 1f
                val w = holder.surfaceFrame.width().toFloat()
                val h = holder.surfaceFrame.height().toFloat()
                return if (w > 0 && h > 0) w / h else 1f
            }

        private val videoRatio: Float
            get() =
                if (videoHeight > 0) videoWidth.toFloat() / videoHeight
                else 1f

        private fun applyVolume() {
            val mp = player ?: return
            val enabled = prefs.getBoolean(Keys.SOUND_ENABLED, true)
            val volume = if (enabled) 1f else 0f
            runCatching { mp.setVolume(volume, volume) }
        }

        private fun releasePlayer() {
            runCatching {
                player?.setOnPreparedListener(null)
                player?.setOnErrorListener(null)
                player?.setOnCompletionListener(null)
                player?.stop()
            }
            runCatching { player?.reset() }
            runCatching { player?.release() }
            player = null
            prepared = false
            videoWidth = 0
            videoHeight = 0
        }
    }
}
