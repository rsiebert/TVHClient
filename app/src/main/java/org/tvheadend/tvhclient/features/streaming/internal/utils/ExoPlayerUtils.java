package org.tvheadend.tvhclient.features.streaming.internal.utils;

import timber.log.Timber;

public class ExoPlayerUtils {

    private ExoPlayerUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static float androidSpeedToExoPlayerSpeed(float speed) {
        // Translate the speed value from what Android uses, to what ExoPlayer expects. Must be
        // greater than zero, and cannot be used for rewind.
        float translatedSpeed;
        switch ((int) speed) {
            case 1: // Normal Playback
                translatedSpeed = 1.0f;
                break;
            case 2: // 2x Fast forward
                translatedSpeed = 2.0f;
                break;
            case 8: // 3x Fast forward
                translatedSpeed = 3.0f;
                break;
            case 32: // 4x Fast forward
                translatedSpeed = 4.0f;
                break;
            case 128: // 5x Fast forward
                translatedSpeed = 5.0f;
                break;
            default:
                throw new IllegalArgumentException("Unknown speed: " + speed);
        }
        Timber.d("Translated android speed " + speed + " to ExoPlayer speed " + translatedSpeed);
        return translatedSpeed;
    }
}
