package org.tvheadend.tvhclient.ui.features.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tvheadend.data.entity.EpgProgram

/**
 * Pure JVM tests for [EpgProgramListDiffCallback]: the identity of an EPG entry is its event id,
 * the content check is the full equality of the [EpgProgram].
 */
class EpgProgramListDiffCallbackTest {

    @Test
    fun reportsTheOldAndNewListSizes() {
        val callback = EpgProgramListDiffCallback(
            oldList = listOf(EpgProgram(eventId = 1)),
            newList = listOf(EpgProgram(eventId = 1), EpgProgram(eventId = 2), EpgProgram(eventId = 3)),
        )

        assertEquals(1, callback.oldListSize)
        assertEquals(3, callback.newListSize)
    }

    @Test
    fun itemsAreTheSameWhenTheEventIdMatches() {
        val callback = EpgProgramListDiffCallback(
            oldList = listOf(EpgProgram(eventId = 1001, title = "Old")),
            newList = listOf(EpgProgram(eventId = 1001, title = "New")),
        )

        assertTrue("Same event id means the same item", callback.areItemsTheSame(0, 0))
        assertFalse("A re-titled event has different content", callback.areContentsTheSame(0, 0))
    }

    @Test
    fun itemsDifferWhenTheEventIdDiffers() {
        val callback = EpgProgramListDiffCallback(
            oldList = listOf(EpgProgram(eventId = 1001, title = "Same")),
            newList = listOf(EpgProgram(eventId = 1002, title = "Same")),
        )

        assertFalse(callback.areItemsTheSame(0, 0))
    }

    @Test
    fun contentsAreTheSameForEqualEpgPrograms() {
        val program = EpgProgram(eventId = 1001, channelId = 1, start = 100, stop = 200, title = "Tagesschau")
        val callback = EpgProgramListDiffCallback(
            oldList = listOf(program),
            newList = listOf(program.copy()),
        )

        assertTrue(callback.areItemsTheSame(0, 0))
        assertTrue(callback.areContentsTheSame(0, 0))
    }

    @Test
    fun contentsDifferWhenAnyFieldChanges() {
        val callback = EpgProgramListDiffCallback(
            oldList = listOf(EpgProgram(eventId = 1001, contentType = 1)),
            newList = listOf(EpgProgram(eventId = 1001, contentType = 2)),
        )

        assertTrue("Still the same event", callback.areItemsTheSame(0, 0))
        assertFalse("The content type changed", callback.areContentsTheSame(0, 0))
    }
}
