package org.tvheadend.tvhclient.cast

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Pure builders for the [MediaItem]s the cast flow hands to [CastPlaybackService].
 *
 * Extracted from the two casting activities so the uri, MIME type, metadata and live-ness can be
 * unit tested without a MediaController or a running service.
 *
 * Live-ness: Media3's `DefaultMediaItemConverter` maps a [MediaItem] to Cast `STREAM_TYPE_LIVE` only
 * when its [MediaItem.LiveConfiguration] differs from the all-unset `LiveConfiguration.UNSET`
 * sentinel - an *empty* configuration still equals UNSET and is not treated as live. Recordings keep
 * the unset default, so the receiver treats them as VOD.
 */
object CastMediaItems {

    /**
     * A concrete latency target, in milliseconds, behind the live edge. Any value other than
     * `C.TIME_UNSET` makes the configuration differ from `LiveConfiguration.UNSET`, which is the
     * condition the converter uses to emit `STREAM_TYPE_LIVE`.
     */
    private const val LIVE_TARGET_OFFSET_MS = 5_000L

    fun channel(
        url: String,
        mimeType: String,
        title: String,
        subtitle: String,
        artworkUri: Uri?
    ): MediaItem {
        return MediaItem.Builder()
                .setUri(url)
                .setMimeType(mimeType)
                .setMediaMetadata(metadata(title, subtitle, artworkUri))
                .setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(LIVE_TARGET_OFFSET_MS)
                                .build())
                .build()
    }

    fun recording(
        url: String,
        mimeType: String,
        title: String,
        subtitle: String,
        artworkUri: Uri?
    ): MediaItem {
        return MediaItem.Builder()
                .setUri(url)
                .setMimeType(mimeType)
                .setMediaMetadata(metadata(title, subtitle, artworkUri))
                .build()
    }

    private fun metadata(title: String, subtitle: String, artworkUri: Uri?): MediaMetadata {
        return MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtworkUri(artworkUri)
                .build()
    }
}
