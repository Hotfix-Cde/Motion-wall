package com.example.motionwall

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

/** Shared streaming configuration for the preview and the live wallpaper. */
object MediaPlaybackFactory {
    private const val USER_AGENT =
        "MotionWall/2.5 (Android) Mozilla/5.0 AppleWebKit/537.36"

    fun createPlayer(context: Context): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    // Do not cap the source to a lower adaptive representation.
                    // The network/server may still refuse a representation.
                    .setForceHighestSupportedBitrate(true)
                    .build()
            )
        }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    /**
     * Supplies an explicit MIME type for common manifest URLs. Progressive
     * video remains content-sniffed by Media3, which also supports URLs with
     * query strings and no file extension when the server reports the type.
     */
    fun mediaItem(uri: Uri): MediaItem {
        val value = uri.toString().lowercase()
        val builder = MediaItem.Builder().setUri(uri)
        when {
            value.contains(".m3u8") -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            value.contains(".mpd") -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
        }
        return builder.build()
    }
}
