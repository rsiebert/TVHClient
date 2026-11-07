package org.tvheadend.tvhclient.ui.features.channels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tvheadend.data.entity.Channel

/**
 * Pure JVM tests for [ChannelListDiffCallback]. ListAdapter uses the callback to work out which
 * items can be recycled and which rows have to be redrawn, so both the identity check (the
 * channel id) and the content check (full equality) matter.
 */
class ChannelListDiffCallbackTest {

    @Test
    fun reportsTheOldAndNewListSizes() {
        val callback = ChannelListDiffCallback(
            oldList = listOf(Channel(id = 1)),
            newList = listOf(Channel(id = 1), Channel(id = 2)),
        )

        assertEquals(1, callback.oldListSize)
        assertEquals(2, callback.newListSize)
    }

    @Test
    fun itemsAreTheSameWhenTheChannelIdMatches() {
        val callback = ChannelListDiffCallback(
            oldList = listOf(Channel(id = 7, name = "Old name")),
            newList = listOf(Channel(id = 7, name = "New name")),
        )

        assertTrue("Same id means the same item", callback.areItemsTheSame(0, 0))
        assertFalse("A renamed channel has different content", callback.areContentsTheSame(0, 0))
    }

    @Test
    fun itemsDifferWhenTheChannelIdDiffers() {
        val callback = ChannelListDiffCallback(
            oldList = listOf(Channel(id = 1, name = "Same name")),
            newList = listOf(Channel(id = 2, name = "Same name")),
        )

        assertFalse(callback.areItemsTheSame(0, 0))
    }

    @Test
    fun contentsAreTheSameForEqualChannels() {
        val callback = ChannelListDiffCallback(
            oldList = listOf(Channel(id = 1, name = "Das Erste", number = 1)),
            newList = listOf(Channel(id = 1, name = "Das Erste", number = 1)),
        )

        assertTrue(callback.areItemsTheSame(0, 0))
        assertTrue(callback.areContentsTheSame(0, 0))
    }

    @Test
    fun contentsDifferWhenAnyFieldChanges() {
        val callback = ChannelListDiffCallback(
            oldList = listOf(Channel(id = 1, programTitle = "Old program")),
            newList = listOf(Channel(id = 1, programTitle = "New program")),
        )

        assertTrue("Still the same channel", callback.areItemsTheSame(0, 0))
        assertFalse("The current program changed", callback.areContentsTheSame(0, 0))
    }
}
