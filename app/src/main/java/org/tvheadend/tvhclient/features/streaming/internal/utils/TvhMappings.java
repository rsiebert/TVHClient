package org.tvheadend.tvhclient.features.streaming.internal.utils;

public class TvhMappings {

    private TvhMappings() {
        throw new IllegalAccessError("Utility class");
    }

    private static final int[] mSampleRates = new int[]{
            96000, 88200, 64000, 48000,
            44100, 32000, 24000, 22050,
            16000, 12000, 11025, 8000,
            7350, 0, 0, 0
    };

    public static int sriToRate(int sri) {
        return mSampleRates[sri & 0xf];
    }
}
