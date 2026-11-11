package org.tvheadend.tvhclient.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Tests for the pure icon sample-size helper in `BitmapUtils.kt`. */
class BitmapUtilsTest {

    @Test
    fun returnsOneWhenTheImageFitsTheTarget() {
        assertThat(calculateInSampleSize(64, 64, 64, 64)).isEqualTo(1)
    }

    @Test
    fun roundsDownToTheNearestPowerOfTwo() {
        // 128/64 = 2 -> 2, 200/64 = 3 -> 2 (largest power of two <= 3), 400/64 = 6 -> 4.
        assertThat(calculateInSampleSize(128, 128, 64, 64)).isEqualTo(2)
        assertThat(calculateInSampleSize(200, 200, 64, 64)).isEqualTo(2)
        assertThat(calculateInSampleSize(400, 400, 64, 64)).isEqualTo(4)
    }

    @Test
    fun usesTheLargerOfTheTwoRatios() {
        // Width ratio 1, height ratio 4 -> 4.
        assertThat(calculateInSampleSize(64, 256, 64, 64)).isEqualTo(4)
    }

    @Test
    fun returnsOneForAZeroSizedOrUnreadableImage() {
        assertThat(calculateInSampleSize(0, 0, 64, 64)).isEqualTo(1)
        assertThat(calculateInSampleSize(-1, 100, 64, 64)).isEqualTo(1)
    }

    @Test
    fun returnsOneForAZeroSizedTarget() {
        assertThat(calculateInSampleSize(128, 128, 0, 64)).isEqualTo(1)
    }
}
