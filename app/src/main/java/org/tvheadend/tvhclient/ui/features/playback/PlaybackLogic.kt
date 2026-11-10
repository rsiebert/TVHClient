package org.tvheadend.tvhclient.ui.features.playback

import org.tvheadend.tvhclient.ui.features.playback.internal.utils.VideoAspect
import java.text.DecimalFormat
import kotlin.math.max

/**
 * Playback logic that depends only on its arguments.
 *
 * Extracted from the two player ViewModels so that it can be unit tested without an ExoPlayer, an
 * HtspConnection or a running activity - neither of which can be built in a JVM test. The view
 * models keep the Android-specific parts (resources, LiveData, the player) and delegate the
 * arithmetic and string assembly to here.
 */
object PlaybackLogic {

    /** The value the server sends when it has no timeshift information for the current stream. */
    private const val NO_TIMESHIFT_VALUE = Long.MIN_VALUE

    /**
     * Translates a relative seek request into an absolute position, in milliseconds, inside the
     * timeshift buffer.
     *
     * The three timeshift values are microseconds, as the server reports them, which is why they are
     * scaled by 1000 while [now] and the result are milliseconds.
     *
     * While the server is reporting timeshift data the position is derived from the server clock -
     * [now] plus [timeshiftOffsetPts] - and `currentPosition` is ignored. When it is not, the
     * player's [currentPosition] is used instead. The result is never allowed to fall before the
     * start of the buffer.
     *
     * [now] and [currentPosition] are parameters rather than being read from
     * System.currentTimeMillis() and the player, which is what makes this testable.
     */
    fun seekPosition(
        timeshiftStartTime: Long,
        timeshiftStartPts: Long,
        timeshiftOffsetPts: Long,
        currentPosition: Long,
        now: Long,
        offsetSeconds: Int
    ): Long {
        val startTime = if (timeshiftStartTime != NO_TIMESHIFT_VALUE) timeshiftStartTime / 1000 else 0
        val currentTime = if (timeshiftOffsetPts != NO_TIMESHIFT_VALUE) now + timeshiftOffsetPts / 1000 else currentPosition
        val time = max(currentTime + offsetSeconds, startTime)
        val seekPts = time * 1000 - timeshiftStartTime
        return max(seekPts, timeshiftStartPts) / 1000
    }

    /**
     * The aspect ratio to display a [width] x [height] video track at.
     *
     * When [forceAspectRatioForSdContent] is set and the track is 5:4, the pixel aspect ratio is
     * recalculated so the picture is shown at 16:9.
     *
     * CHARACTERISATION: the 5:4 test compares the formatted ratio against the literal `"1,25"`, and
     * `DecimalFormat` uses the default locale's decimal separator. The correction therefore only
     * fires in locales that write `1,25`; in an English locale the formatted value is `1.25`, the
     * comparison fails and the pixel aspect ratio is left alone. Recorded here rather than changed,
     * because it is a behaviour fix rather than part of the extraction - see finding 22.
     */
    fun videoAspect(
        width: Int,
        height: Int,
        pixelWidthHeightRatio: Float,
        forceAspectRatioForSdContent: Boolean
    ): VideoAspect {
        var newPixelWidthHeightRatio = pixelWidthHeightRatio
        if (forceAspectRatioForSdContent) {
            val aspectRatio = DecimalFormat("#.##").format(width.toFloat() / height.toFloat())
            if (aspectRatio == "1,25") {
                newPixelWidthHeightRatio = ((16f / 9f) * height.toFloat()) / width.toFloat()
            }
        }
        return VideoAspect((width * newPixelWidthHeightRatio).toInt(), height)
    }

    /**
     * The URL the external player is handed for a stream: the server's base url, the stream path,
     * the one-time ticket and the playback profile's name.
     */
    fun playbackUrl(baseUrl: String, path: String, ticket: String, profileName: String): String {
        return "$baseUrl$path?ticket=$ticket&profile=$profileName"
    }

    /**
     * The MIME type handed to the Cast receiver for a stream produced by [profileName].
     *
     * The HTSP profile entity carries only the profile's name, never its container, so the type is
     * derived from that name with MPEG-TS as the fallback - what the plain `pass` style profiles
     * stream. A value the receiver does not recognise makes it refuse to play, which is why this is
     * a pure function that can be unit tested rather than another literal buried in an activity.
     */
    fun castingContentType(profileName: String?): String = when {
        profileName == null -> "video/mp2t"
        profileName.contains("matroska", ignoreCase = true) -> "video/x-matroska"
        profileName.contains("webm", ignoreCase = true) -> "video/webm"
        profileName.contains("hls", ignoreCase = true) ||
            profileName.contains("m3u8", ignoreCase = true) -> "application/x-mpegURL"
        else -> "video/mp2t"
    }
}
