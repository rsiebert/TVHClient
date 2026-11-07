package org.tvheadend.data.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure JVM mapping tests for [EpgChannelEntity]. No Robolectric needed: [EpgChannelEntity.from]
 * and [EpgChannelEntity.toEpgChannel] are plain in-memory conversions.
 */
class EpgChannelEntityTest {

    @Test
    fun roundTripsEveryField() {
        val epgChannel = EpgChannel(
                id = 1,
                number = 2,
                numberMinor = 1,
                name = "Das Erste HD",
                icon = "http://example.org/icon.png",
                displayNumber = "2.1")

        assertThat(EpgChannelEntity.from(epgChannel).toEpgChannel()).isEqualTo(epgChannel)
    }

    @Test
    fun roundTripsNullFields() {
        val epgChannel = EpgChannel(
                id = 5,
                number = 0,
                numberMinor = 0,
                name = null,
                icon = null,
                displayNumber = null)

        val restored = EpgChannelEntity.from(epgChannel).toEpgChannel()

        assertThat(restored).isEqualTo(epgChannel)
        assertThat(restored.name).isNull()
        assertThat(restored.icon).isNull()
    }
}
