package org.tvheadend.tvhclient.ui.features.playback.internal.utils;

import android.text.TextUtils;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.Locale;

import timber.log.Timber;

class ExoPlayerUtils {

    private ExoPlayerUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static float androidSpeedToExoPlayerSpeed(float speed) {
        // Translate the speed value from what Android uses, to what ExoPlayer expects. Must be
        // greater than zero, and cannot be used for rewind.
        float translatedSpeed = switch ((int) speed) {
            case 1 -> // Normal Playback
                    1.0f;
            case 2 -> // 2x Fast forward
                    2.0f;
            case 8 -> // 3x Fast forward
                    3.0f;
            case 32 -> // 4x Fast forward
                    4.0f;
            case 128 -> // 5x Fast forward
                    5.0f;
            default -> throw new IllegalArgumentException("Unknown speed: " + speed);
        };
        Timber.d("Translated android speed " + speed + " to ExoPlayer speed " + translatedSpeed);
        return translatedSpeed;
    }

    /**
     * Builds a track name for display.
     *
     * @param format {@link Format} of the track.
     * @return a generated name specific to the track.
     */
    static String buildTrackName(Format format) {
        String trackName;
        if (MimeTypes.isVideo(format.sampleMimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(
                    buildResolutionString(format), buildBitrateString(format)), buildTrackIdString(format)),
                    buildSampleMimeTypeString(format));
        } else if (MimeTypes.isAudio(format.sampleMimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(joinWithSeparator(
                    buildLanguageString(format), buildAudioPropertyString(format)),
                    buildBitrateString(format)), buildTrackIdString(format)),
                    buildSampleMimeTypeString(format));
        } else {
            trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(buildLanguageString(format),
                    buildBitrateString(format)), buildTrackIdString(format)),
                    buildSampleMimeTypeString(format));
        }
        return trackName.length() == 0 ? "unknown" : trackName;
    }

    private static String buildResolutionString(Format format) {
        return format.width == Format.NO_VALUE || format.height == Format.NO_VALUE
                ? "" : format.width + "x" + format.height;
    }

    private static String buildAudioPropertyString(Format format) {
        return format.channelCount == Format.NO_VALUE || format.sampleRate == Format.NO_VALUE
                ? "" : format.channelCount + "ch, " + format.sampleRate + "Hz";
    }

    private static String buildLanguageString(Format format) {
        return TextUtils.isEmpty(format.language) || "und".equals(format.language) ? ""
                : format.language;
    }

    private static String buildBitrateString(Format format) {
        return format.bitrate == Format.NO_VALUE ? ""
                : String.format(Locale.US, "%.2fMbit", format.bitrate / 1000000f);
    }

    private static String joinWithSeparator(String first, String second) {
        return first.length() == 0 ? second : (second.length() == 0 ? first : first + ", " + second);
    }

    private static String buildTrackIdString(Format format) {
        return format.id == null ? "" : ("id:" + format.id);
    }

    private static String buildSampleMimeTypeString(Format format) {
        return format.sampleMimeType == null ? "" : format.sampleMimeType;
    }
}
