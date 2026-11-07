package org.tvheadend.htsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

/**
 * Tests that the messages with the nested structures that the newer protocol versions use
 * survive a trip over the wire. The `cutpoints` and `servercapability` fields are the
 * interesting ones: a list of maps respectively a list of strings.
 */
class HtspMessageCodecTest {

    @Test
    fun keepsAListOfMaps() {
        val cutpoint = HtspMessage()
        cutpoint.put("start", 1234L)
        cutpoint.put("end", 5678L)
        cutpoint.put("type", 3L)

        val secondCutpoint = HtspMessage()
        secondCutpoint.put("start", 9000L)
        secondCutpoint.put("end", 9500L)
        secondCutpoint.put("type", 0L)

        val message = HtspMessage()
        message.setMethod("getDvrCutpoints")
        message.put("cutpoints", arrayListOf(cutpoint, secondCutpoint))

        val received = roundTrip(message)

        assertEquals("getDvrCutpoints", received.getMethod())
        val cutpoints = received.getList("cutpoints")
        assertNotNull("The list of cut points should have survived the trip", cutpoints)
        assertEquals(2, cutpoints!!.size)

        val first = cutpoints[0] as HtspMessage
        assertEquals(1234L, first.getLong("start"))
        assertEquals(5678L, first.getLong("end"))
        assertEquals(3, first.getInteger("type"))

        val second = cutpoints[1] as HtspMessage
        assertEquals(9000L, second.getLong("start"))
        assertEquals(0, second.getInteger("type"))
    }

    @Test
    fun keepsAListOfStrings() {
        val message = HtspMessage()
        message.setMethod("serverStatus")
        message.put("servercapability", arrayListOf("timeshift", "tvh-http-api"))
        message.put("htspversion", 44L)

        val received = roundTrip(message)

        assertEquals(44L, received.getLong("htspversion"))
        assertEquals(listOf("timeshift", "tvh-http-api"), received.getArrayList("servercapability"))
    }

    /**
     * Serializes the message with the production writer, reads it back through the production
     * parser and returns the parsed message.
     */
    private fun roundTrip(message: HtspMessage): HtspMessage {
        val serverSocket = ServerSocketChannel.open()
        serverSocket.bind(InetSocketAddress("127.0.0.1", 0))
        try {
            val client = SocketChannel.open(InetSocketAddress("127.0.0.1", serverSocket.socket().localPort))
            val server = serverSocket.accept()
            try {
                message.transmit(client)

                val buffer = ByteBuffer.allocate(1 shl 16)
                buffer.limit(4)
                repeat(20) {
                    val parsed = HtspMessage.parse(buffer)
                    if (parsed != null) {
                        return parsed
                    }
                    if (server.read(buffer) < 0) {
                        throw AssertionError("The connection was closed before the message was complete")
                    }
                }
                throw AssertionError("The message did not arrive within the timeout")
            } finally {
                client.close()
                server.close()
            }
        } finally {
            serverSocket.close()
        }
    }
}
