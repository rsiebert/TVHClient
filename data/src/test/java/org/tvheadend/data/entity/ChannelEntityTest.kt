package org.tvheadend.data.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure JVM mapping tests for [ChannelEntity]. No Robolectric needed: [ChannelEntity.from] and
 * [ChannelEntity.toChannel] are plain in-memory conversions.
 */
class ChannelEntityTest {

    @Test
    fun roundTripsEveryFieldIncludingTheIgnoredOnes() {
        val channel = Channel(
                id = 7,
                number = 3,
                numberMinor = 2,
                name = "Das Erste",
                icon = "http://example.org/icon.png",
                eventId = 42,
                nextEventId = 43,
                connectionId = 1,
                programId = 100,
                programTitle = "Tagesschau",
                programSubtitle = "Subtitle",
                programStart = 1_000L,
                programStop = 2_000L,
                programContentType = 4,
                nextProgramId = 101,
                nextProgramTitle = "Next",
                displayNumber = "3.2",
                serverOrder = 9,
                tags = listOf(1, 2, 3),
                recording = Recording(id = 5, channelId = 7, title = "Recording", start = 10L, stop = 20L))

        val entity = ChannelEntity.from(channel)
        val restored = entity.toChannel()

        assertThat(restored).isEqualTo(channel)
        // The @Ignore fields are not persisted by Room, but the pure mapping must keep them.
        assertThat(restored.tags).containsExactly(1, 2, 3).inOrder()
        assertThat(restored.recording).isEqualTo(channel.recording)
    }

    @Test
    fun roundTripsNullFields() {
        val channel = Channel(
                id = 1,
                connectionId = 1,
                name = null,
                icon = null,
                programTitle = null,
                programSubtitle = null,
                displayNumber = null,
                tags = null,
                recording = null)

        val restored = ChannelEntity.from(channel).toChannel()

        assertThat(restored).isEqualTo(channel)
        assertThat(restored.name).isNull()
        assertThat(restored.tags).isNull()
        assertThat(restored.recording).isNull()
    }
}
