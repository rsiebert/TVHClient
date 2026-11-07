package org.tvheadend.tvhclient.ui.features.playback.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Tests for the buffer copy that [HtspFileInputStreamDataSource.read] uses.
 *
 * The number of copied bytes used to be derived from the position of the buffer minus the
 * offset in the destination array, which is wrong for every offset other than zero. The
 * player got a wrong (and for larger offsets even a negative) byte count back.
 */
class CopyBufferedBytesTest {

    private val content = ByteArray(10) { (it + 1).toByte() }

    @Test
    fun copiesAllBytesIntoAnEmptyDestination() {
        val buffer = ByteBuffer.wrap(content)
        val destination = ByteArray(10)

        val copied = copyBufferedBytes(buffer, destination, 0, 10)

        assertEquals(10, copied)
        assertEquals(content.toList(), destination.toList())
        assertEquals(0, buffer.remaining())
    }

    @Test
    fun reportsTheCopiedBytesWhenTheDestinationIsOffset() {
        val buffer = ByteBuffer.wrap(content)
        val destination = ByteArray(10)

        val copied = copyBufferedBytes(buffer, destination, 4, 6)

        assertEquals(6, copied)
        assertEquals(content.take(6), destination.drop(4))
    }

    @Test
    fun copiesOnlyAsMuchAsRequested() {
        val buffer = ByteBuffer.wrap(content)
        val destination = ByteArray(10)

        val copied = copyBufferedBytes(buffer, destination, 0, 4)

        assertEquals(4, copied)
        assertEquals(6, buffer.remaining())
        assertEquals(content.take(4), destination.take(4))
    }

    @Test
    fun copiesOnlyWhatIsLeftInTheBuffer() {
        val buffer = ByteBuffer.wrap(content)
        val destination = ByteArray(10)

        val copied = copyBufferedBytes(buffer, destination, 0, 25)

        assertEquals(10, copied)
        assertEquals(0, buffer.remaining())
    }

    @Test
    fun copiesNothingFromAnEmptyBuffer() {
        val buffer = ByteBuffer.allocate(0)
        val destination = ByteArray(10)

        val copied = copyBufferedBytes(buffer, destination, 0, 10)

        assertEquals(0, copied)
    }

    @Test
    fun copiesNothingWhenNoLengthIsRequested() {
        val buffer = ByteBuffer.wrap(content)
        val destination = ByteArray(10)

        val copied = copyBufferedBytes(buffer, destination, 0, 0)

        assertEquals(0, copied)
        assertEquals(10, buffer.remaining())
    }
}
