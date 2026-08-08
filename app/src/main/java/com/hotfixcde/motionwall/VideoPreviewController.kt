package com.hotfixcde.motionwall

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.core.view.isVisible

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
        sourceUri = uri
        if (uri == null) {
            releasePlayer()
            placeholderView.isVisible = true
            return
        }

        placeholderView.isVisible = false
        if (textureView.isAvailable) {
            preparePlayer()
        }
    }

    fun resume() {
        val currentPlayer = player
        if (currentPlayer != null && !currentPlayer.isPlaying) {
            currentPlayer.start()
        } else if (currentPlayer == null && sourceUri != null && textureView.isAvailable) {
            preparePlayer()
        }
    }

    fun pause() {
        player?.pause()
    }

    fun release() {
        releasePlayer()
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        if (sourceUri != null) {
            preparePlayer()
        } else {
            placeholderView.isVisible = true
        }
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

        placeholderView.isVisible = false
        player = MediaPlayer().apply {
            setDataSource(context, uri)
            setSurface(surface)
            isLooping = true
            setOnPreparedListener { preparedPlayer ->
                videoWidth = preparedPlayer.videoWidth
                videoHeight = preparedPlayer.videoHeight
                applySettingsToPlayer()
                applyTransform()
                preparedPlayer.start()
                placeholderView.isVisible = false
            }
            setOnVideoSizeChangedListener { _, width, height ->
                videoWidth = width
                videoHeight = height
                applyTransform()
            }
            setOnCompletionListener { completedPlayer ->
                completedPlayer.seekTo(0)
                if (!completedPlayer.isPlaying) {
                    completedPlayer.start()
                }
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
        currentPlayer.setVolume(if (settings.soundEnabled) 1f else 0f, if (settings.soundEnabled) 1f else 0f)
        currentPlayer.setVideoScalingMode(
            if (settings.scaleMode == ScaleMode.FIT) {
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
            } else {
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
        )
    }

    private fun applyTransform() {
        val currentVideoWidth = videoWidth
        val currentVideoHeight = videoHeight
        if (!textureView.isAvailable || viewWidth <= 0 || viewHeight <= 0 || currentVideoWidth <= 0 || currentVideoHeight <= 0) {
            return
        }

        val shouldRotate = shouldRotate(currentVideoWidth, currentVideoHeight)
        val bufferWidth = if (shouldRotate) currentVideoHeight.toFloat() else currentVideoWidth.toFloat()
        val bufferHeight = if (shouldRotate) currentVideoWidth.toFloat() else currentVideoHeight.toFloat()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, bufferWidth, bufferHeight)
        val matrix = Matrix()

        matrix.setRectToRect(
            bufferRect,
            viewRect,
            if (settings.scaleMode == ScaleMode.FIT) Matrix.ScaleToFit.CENTER else Matrix.ScaleToFit.FILL,
        )

        if (shouldRotate) {
            matrix.postRotate(90f, viewRect.centerX(), viewRect.centerY())
        }

        textureView.setTransform(matrix)
    }

    private fun shouldRotate(videoWidth: Int, videoHeight: Int): Boolean {
        return when (settings.orientationMode) {
            OrientationMode.AUTO -> false
            OrientationMode.VERTICAL -> videoWidth > videoHeight
            OrientationMode.HORIZONTAL -> videoHeight > videoWidth
        }
    }

    private fun releasePlayer() {
        surface?.release()
        surface = null
        player?.release()
        player = null
        videoWidth = 0
        videoHeight = 0
        if (sourceUri == null) {
            placeholderView.isVisible = true
        }
    }
}
