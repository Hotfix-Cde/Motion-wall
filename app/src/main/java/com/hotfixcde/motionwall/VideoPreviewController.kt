package com.hotfixcde.motionwall

import android.content.Context
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.core.view.isVisible
import kotlin.math.max
import kotlin.math.min

class VideoPreviewController(
    private val context: Context,
    private val textureView: TextureView,
    private val placeholderView: android.view.View,
) : TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var sourceUri: Uri? = null
    private var viewWidth = 0
    private var viewHeight = 0
    private var videoWidth = 0
    private var videoHeight = 0
    private var settings = MotionSettingsStore.load(context)

    init {
        textureView.surfaceTextureListener = this
    }

    fun setSettings(newSettings: MotionSettings) {
        settings = newSettings
        applySettingsToPlayer()
        applyTransform()
    }

    fun setVideo(uri: Uri?) {
        if (sourceUri == uri && (uri == null || player != null)) {
            applySettingsToPlayer()
            applyTransform()
            return
        }

        sourceUri = uri
        if (uri == null) {
            releasePlayer()
            placeholderView.isVisible = true
            return
        }

        // Keep the placeholder until the first video frame is actually rendered.
        // This prevents a black preview while MediaPlayer is still preparing.
        placeholderView.isVisible = true
        if (textureView.isAvailable) preparePlayer()
    }

    fun resume() {
        val currentPlayer = player
        if (currentPlayer != null && !currentPlayer.isPlaying) {
            runCatching { currentPlayer.start() }
        } else if (currentPlayer == null && sourceUri != null && textureView.isAvailable) {
            preparePlayer()
        }
    }

    fun pause() {
        runCatching { player?.pause() }
    }

    fun release() {
        releasePlayer()
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        if (sourceUri != null) preparePlayer() else placeholderView.isVisible = true
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        applyTransform()
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: android.graphics.SurfaceTexture): Boolean {
        releasePlayer()
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: android.graphics.SurfaceTexture) = Unit

    private fun preparePlayer() {
        val uri = sourceUri ?: return
        releasePlayer()

        val surfaceTexture = textureView.surfaceTexture ?: return
        surface = Surface(surfaceTexture)
        placeholderView.isVisible = true

        player = MediaPlayer().apply {
            setDataSource(context, uri)
            setSurface(surface)
            isLooping = true
            setOnPreparedListener { preparedPlayer ->
                this@VideoPreviewController.videoWidth = preparedPlayer.videoWidth
                this@VideoPreviewController.videoHeight = preparedPlayer.videoHeight
                applySettingsToPlayer()
                applyTransform()
                if (textureView.isShown) runCatching { preparedPlayer.start() }
            }
            setOnVideoSizeChangedListener { _, width, height ->
                this@VideoPreviewController.videoWidth = width
                this@VideoPreviewController.videoHeight = height
                applyTransform()
            }
            setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    placeholderView.isVisible = false
                }
                false
            }
            setOnErrorListener { _, _, _ ->
                placeholderView.isVisible = true
                true
            }
            prepareAsync()
        }
    }

    private fun applySettingsToPlayer() {
        val currentPlayer = player ?: return
        runCatching {
            currentPlayer.setVolume(if (settings.soundEnabled) 1f else 0f, if (settings.soundEnabled) 1f else 0f)
            currentPlayer.setVideoScalingMode(
                if (settings.scaleMode == ScaleMode.FIT) {
                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                } else {
                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                },
            )
        }
    }

    /** Preserve aspect ratio. Crop fills the preview; Fit keeps the complete frame. */
    private fun applyTransform() {
        if (!textureView.isAvailable || viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return

        val rotate = shouldRotate(videoWidth, videoHeight)
        val sourceWidth = if (rotate) videoHeight.toFloat() else videoWidth.toFloat()
        val sourceHeight = if (rotate) videoWidth.toFloat() else videoHeight.toFloat()
        val viewW = viewWidth.toFloat()
        val viewH = viewHeight.toFloat()

        val scale = if (settings.scaleMode == ScaleMode.CROP) {
            max(viewW / sourceWidth, viewH / sourceHeight)
        } else {
            min(viewW / sourceWidth, viewH / sourceHeight)
        }
        val scaledW = sourceWidth * scale
        val scaledH = sourceHeight * scale
        val dx = (viewW - scaledW) / 2f
        val dy = (viewH - scaledH) / 2f

        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
            if (rotate) postRotate(90f, viewW / 2f, viewH / 2f)
        }
        textureView.setTransform(matrix)
    }

    private fun shouldRotate(videoWidth: Int, videoHeight: Int): Boolean = when (settings.orientationMode) {
        OrientationMode.AUTO -> false
        OrientationMode.VERTICAL -> videoWidth > videoHeight
        OrientationMode.HORIZONTAL -> videoHeight > videoWidth
    }

    private fun releasePlayer() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        surface?.release()
        surface = null
        videoWidth = 0
        videoHeight = 0
    }
}
