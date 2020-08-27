package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import timber.log.Timber

class TvhMappings private constructor() {

    companion object {
        private val mSampleRates = intArrayOf(
                96000, 88200, 64000, 48000,
                44100, 32000, 24000, 22050,
                16000, 12000, 11025, 8000,
                7350, 0, 0, 0
        )

        fun sriToRate(sri: Int): Int {
            return mSampleRates[sri and 0xf]
        }

        fun androidSpeedToTvhSpeed(speed: Float): Int {
            // Translate the speed value from what Android uses, to what TVHeadend expects.
            // TVHeadend expects: 0=pause, 100=1x fwd, -100=1x backward)
            val translatedSpeed: Int = when (speed.toInt()) {
                1 -> 100
                -2 -> -200
                -4 -> -300
                -12 -> -400
                -48 -> -500
                2 -> 200
                8 -> 300
                32 -> 400
                128 -> 500
                else -> throw IllegalArgumentException("Unknown speed: $speed")
            }
            Timber.d("Translated android speed $speed to TVH speed $translatedSpeed")
            return translatedSpeed
        }
    }

    init {
        throw IllegalAccessError("Utility class")
    }
}