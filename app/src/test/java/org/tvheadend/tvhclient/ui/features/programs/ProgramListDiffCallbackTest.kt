package org.tvheadend.tvhclient.ui.features.programs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.ProgramInterface

/**
 * Pure JVM tests for [ProgramListDiffCallback]. It works on the [ProgramInterface] abstraction,
 * so the identity is the event id while the content check is the full data class equality.
 */
class ProgramListDiffCallbackTest {

    @Test
    fun reportsTheOldAndNewListSizes() {
        val oldList: List<ProgramInterface> = listOf(EpgProgram(eventId = 1))
        val newList: List<ProgramInterface> = listOf(EpgProgram(eventId = 1), EpgProgram(eventId = 2))

        val callback = ProgramListDiffCallback(oldList, newList)

        assertEquals(1, callback.oldListSize)
        assertEquals(2, callback.newListSize)
    }

    @Test
    fun itemsAreTheSameWhenTheEventIdMatches() {
        val callback = ProgramListDiffCallback(
            oldList = listOf(EpgProgram(eventId = 42, title = "Old title")),
            newList = listOf(EpgProgram(eventId = 42, title = "New title")),
        )

        assertTrue("Same event id means the same item", callback.areItemsTheSame(0, 0))
        assertFalse("A re-titled event has different content", callback.areContentsTheSame(0, 0))
    }

    @Test
    fun itemsDifferWhenTheEventIdDiffers() {
        val callback = ProgramListDiffCallback(
            oldList = listOf(EpgProgram(eventId = 1, title = "Same title")),
            newList = listOf(EpgProgram(eventId = 2, title = "Same title")),
        )

        assertFalse(callback.areItemsTheSame(0, 0))
    }

    @Test
    fun contentsAreTheSameForEqualPrograms() {
        val program = EpgProgram(eventId = 1, channelId = 3, title = "Tagesschau", start = 100, stop = 200)
        val callback = ProgramListDiffCallback(
            oldList = listOf(program),
            newList = listOf(program.copy()),
        )

        assertTrue(callback.areItemsTheSame(0, 0))
        assertTrue(callback.areContentsTheSame(0, 0))
    }

    @Test
    fun contentsDifferWhenAnyFieldChanges() {
        val callback = ProgramListDiffCallback(
            oldList = listOf(EpgProgram(eventId = 1, start = 100, stop = 200)),
            newList = listOf(EpgProgram(eventId = 1, start = 150, stop = 250)),
        )

        assertTrue("Still the same event", callback.areItemsTheSame(0, 0))
        assertFalse("The broadcast time changed", callback.areContentsTheSame(0, 0))
    }
}
