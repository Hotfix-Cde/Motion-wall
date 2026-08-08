package com.hotfixcde.motionwall

import android.content.Context
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.View
import kotlin.math.max
import kotlin.math.min

class VideoPreviewController(
    private val context: Context,
    private val textureView: TextureView,
    private val placeholderView: View,
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
        val oldSettings = settings
        settings = newSettings
        if (oldSettings.soundEnabled != newSettings.soundEnabled) {
            applySoundSetting()
        }
        applyTransform()
    }

    fun setVideo(uri: Uri?) {
        if (sourceUri == uri && (uri == null || player != null)) {
            applySoundSetting()
            applyTransform()
            return
        }

        sourceUri = uri
        releasePlayer()
        if (uri == null) {
            placeholderView.visibility = View.VISIBLE
            return
        }

        placeholderView.visibility = View.VISIBLE
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
        if (sourceUri != null) preparePlayer() else placeholderView.visibility = View.VISIBLE
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

    override fun onSurfaceTextureUpdated(surfaceTexture: android.graphics.SurfaceTexture) {
        // Some devices do not reliably send MEDIA_INFO_VIDEO_RENDERING_START.
        // The first texture update is definitive proof that a frame is visible.
        if (placeholderView.visibility == View.VISIBLE && videoWidth > 0 && videoHeight > 0) {
            placeholderView.visibility = View.GONE
        }
    }

    private fun preparePlayer() {
        val uri = sourceUri ?: return
        val surfaceTexture = textureView.surfaceTexture ?: return

        releasePlayer()
        surface = Surface(surfaceTexture)
        placeholderView.visibility = View.VISIBLE

        player = MediaPlayer().apply {
            setDataSource(context, uri)
            setSurface(surface)
            isLooping = true
            setOnPreparedListener { preparedPlayer ->
                videoWidth = preparedPlayer.videoWidth
                videoHeight = preparedPlayer.videoHeight
                applySoundSetting()
                applyTransform()
                runCatching { preparedPlayer.start() }
            }
            setOnVideoSizeChangedListener { _, width, height ->
                videoWidth = width
                videoHeight = height
                applyTransform()
            }
            setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    placeholderView.visibility = View.GONE
                }
                false
            }
            setOnErrorListener { _, _, _ ->
                placeholderView.visibility = View.VISIBLE
                true
            }
            prepareAsync()
        }
    }

    private fun applySoundSetting() {
        val currentPlayer = player ?: return
        val volume = if (settings.soundEnabled) 1f else 0f
        runCatching { currentPlayer.setVolume(volume, volume) }
    }

    private fun applyTransform() {
        if (!textureView.isAvailable || viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return

        val rotate = when (settings.orientationMode) {
            OrientationMode.AUTO -> false
            OrientationMode.VERTICAL -> videoWidth > videoHeight
            OrientationMode.HORIZONTAL -> videoHeight > videoWidth
        }

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

        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        if (rotate) matrix.postRotate(90f, viewW / 2f, viewH / 2f)
        textureView.setTransform(matrix)
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
