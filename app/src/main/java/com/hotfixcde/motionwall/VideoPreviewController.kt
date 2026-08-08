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
    private var renderSurface: Surface? = null
    private var sourceUri: Uri? = null
    private var viewWidth = 0
    private var viewHeight = 0
    private var currentVideoWidth = 0
    private var currentVideoHeight = 0
    private var settings = MotionSettingsStore.load(context)

    init {
        textureView.surfaceTextureListener = this
    }

    fun setSettings(newSettings: MotionSettings) {
        val soundChanged = settings.soundEnabled != newSettings.soundEnabled
        settings = newSettings
        if (soundChanged) applySoundSetting()
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
        placeholderView.visibility = View.VISIBLE
        if (uri != null && textureView.isAvailable) preparePlayer()
    }

    fun resume() {
        val currentPlayer = player
        if (currentPlayer != null) {
            runCatching {
                if (!currentPlayer.isPlaying) currentPlayer.start()
            }
        } else if (sourceUri != null && textureView.isAvailable) {
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
        if (placeholderView.visibility == View.VISIBLE && currentVideoWidth > 0 && currentVideoHeight > 0) {
            placeholderView.visibility = View.GONE
        }
    }

    private fun preparePlayer() {
        val uri = sourceUri ?: return
        val texture = textureView.surfaceTexture ?: return

        releasePlayer()
        val newSurface = Surface(texture)
        renderSurface = newSurface
        placeholderView.visibility = View.VISIBLE

        val newPlayer = MediaPlayer()
        player = newPlayer
        try {
            newPlayer.setDataSource(context, uri)
            newPlayer.setSurface(newSurface)
            newPlayer.isLooping = true
            newPlayer.setOnPreparedListener { preparedPlayer ->
                currentVideoWidth = preparedPlayer.videoWidth
                currentVideoHeight = preparedPlayer.videoHeight
                applySoundSetting()
                applyTransform()
                runCatching { preparedPlayer.start() }
            }
            newPlayer.setOnVideoSizeChangedListener { _, newWidth, newHeight ->
                currentVideoWidth = newWidth
                currentVideoHeight = newHeight
                applyTransform()
            }
            newPlayer.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    placeholderView.visibility = View.GONE
                }
                false
            }
            newPlayer.setOnErrorListener { _, _, _ ->
                placeholderView.visibility = View.VISIBLE
                true
            }
            newPlayer.prepareAsync()
        } catch (_: Exception) {
            placeholderView.visibility = View.VISIBLE
            releasePlayer()
        }
    }

    private fun applySoundSetting() {
        val currentPlayer = player ?: return
        val volume = if (settings.soundEnabled) 1f else 0f
        runCatching { currentPlayer.setVolume(volume, volume) }
    }

    private fun applyTransform() {
        if (!textureView.isAvailable || viewWidth <= 0 || viewHeight <= 0 || currentVideoWidth <= 0 || currentVideoHeight <= 0) return

        val rotate = when (settings.orientationMode) {
            OrientationMode.AUTO -> false
            OrientationMode.VERTICAL -> currentVideoWidth > currentVideoHeight
            OrientationMode.HORIZONTAL -> currentVideoHeight > currentVideoWidth
        }
        val sourceWidth = if (rotate) currentVideoHeight.toFloat() else currentVideoWidth.toFloat()
        val sourceHeight = if (rotate) currentVideoWidth.toFloat() else currentVideoHeight.toFloat()
        val viewW = viewWidth.toFloat()
        val viewH = viewHeight.toFloat()
        val scale = if (settings.scaleMode == ScaleMode.CROP) {
            max(viewW / sourceWidth, viewH / sourceHeight)
        } else {
            min(viewW / sourceWidth, viewH / sourceHeight)
        }

        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate((viewW - sourceWidth * scale) / 2f, (viewH - sourceHeight * scale) / 2f)
        if (rotate) matrix.postRotate(90f, viewW / 2f, viewH / 2f)
        textureView.setTransform(matrix)
    }

    private fun releasePlayer() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        renderSurface?.release()
        renderSurface = null
        currentVideoWidth = 0
        currentVideoHeight = 0
    }
}
