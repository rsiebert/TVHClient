package org.tvheadend.htsp

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * JVM (Robolectric) tests for [HtspFileInputStream], the `InputStream` that the internal player
 * uses to stream a recording from a TVHeadend server over HTSP.
 *
 * [HtspFileInputStream] drives a real [HtspConnection]: every `read` is a `fileRead` request and
 * the constructor sends a `fileOpen`. The tests therefore run the real connection against an
 * in-process loopback server ([FakeFileServer]) that speaks the production HTSP framing
 * ([HtspMessage.parse] / [HtspMessage.transmit]) - exactly like [HtspMessageCodecTest.roundTrip]
 * does. The server hands the file out in small chunks so the buffering (`fillBuffer`) and the
 * offset bookkeeping are exercised too.
 *
 * Every byte of the file is delivered exactly once and the first chunk is only ever requested
 * once; the offset sequence asserted below locks that in so the former "first chunk fetched
 * twice" behaviour cannot come back.
 *
 * Robolectric is required because [HtspConnection] resolves host/port through `android.net.Uri`
 * and stores its response handlers in an `android.util.SparseArray`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HtspFileInputStreamTest {

    private lateinit var server: FakeFileServer
    private var connection: HtspConnection? = null
    private var originalResponseTimeoutMs: Long = 0

    private val content = "Hello, TVHClient!".toByteArray()
    private val chunkSize = 5

    @Before
    fun setUp() {
        // Lower the response timeout so the "server never answers" test fails fast instead of
        // waiting the production 10 s. Every other test is answered immediately on loopback, so
        // the lower bound is never reached.
        originalResponseTimeoutMs = HtspFileInputStream.responseTimeoutMs
        HtspFileInputStream.responseTimeoutMs = 250

        // A chunk of five bytes forces the stream to fetch the file several times.
        server = FakeFileServer(content, chunkSize).start()
    }

    @After
    fun tearDown() {
        HtspFileInputStream.responseTimeoutMs = originalResponseTimeoutMs
        connection?.closeConnection()
        server.close()
    }

    @Test
    fun readsAllBytesUntilEof() {
        val stream = openStream()
        try {
            assertThat(readFully(stream)).isEqualTo(content)
            assertEquals("fileOpen", 1, server.openCount.get())
            assertEquals("fileClose", 0, server.closeCount.get())
            // The file is larger than the chunk size, so it must have been fetched repeatedly.
            assertThat(server.readCount.get()).isGreaterThan(1)
        } finally {
            stream.close()
        }
        assertEquals("fileClose", 1, server.closeCount.get())
    }

    @Test
    fun readsOneByteAtATimeAndReturnsMinusOneAtTheEnd() {
        val stream = openStream()
        try {
            val out = ByteArrayOutputStream()
            while (true) {
                val b = stream.read()
                if (b < 0) break
                out.write(b)
            }
            assertThat(out.toByteArray()).isEqualTo(content)
            // Once exhausted the stream keeps reporting EOF.
            assertEquals(-1, stream.read())
        } finally {
            stream.close()
        }
    }

    @Test
    fun readsIntoABufferThatIsSmallerThanAChunk() {
        val stream = openStream()
        try {
            val buffer = ByteArray(3)
            val out = ByteArrayOutputStream()
            while (true) {
                val read = stream.read(buffer, 0, buffer.size)
                if (read < 0) break
                out.write(buffer, 0, read)
            }
            assertThat(out.toByteArray()).isEqualTo(content)
        } finally {
            stream.close()
        }
    }

    @Test
    fun requestsTheFileInChunksWithoutRefetchingTheFirstOne() {
        val stream = openStream()
        try {
            readFully(stream)

            // The offset starts at 0 and advances by the number of bytes that were actually
            // consumed, so each chunk is requested exactly once: 0, 5, 10, 15 and finally 17,
            // where the server reports EOF because the file is only 17 bytes long.
            assertThat(server.requestedOffsets).containsExactly(0L, 5L, 10L, 15L, 17L).inOrder()
        } finally {
            stream.close()
        }
    }

    @Test
    fun availableCountsOnlyTheBytesNotYetConsumed() {
        val stream = openStream()
        try {
            assertEquals("Nothing is buffered before the first read", 0, stream.available())

            val buffer = ByteArray(1)
            assertEquals(1, stream.read(buffer, 0, 1))

            // A five byte chunk was fetched and one byte of it has been handed out, so four
            // remain. This used to report the whole chunk size, ignoring the read position.
            assertEquals(4, stream.available())
        } finally {
            stream.close()
        }
    }

    @Test
    fun reportsThatMarkIsNotSupported() {
        val stream = openStream()
        try {
            assertFalse(stream.markSupported())
        } finally {
            stream.close()
        }
    }

    @Test
    fun resetClearsTheBufferAndRestartsFromTheBeginning() {
        val stream = openStream()
        try {
            val buffer = ByteArray(1)
            assertEquals(1, stream.read(buffer, 0, 1))

            stream.reset()
            assertEquals("The buffer is emptied again", 0, stream.available())

            // The offset was reset as well, so the next read re-fetches from the beginning.
            assertEquals(content[0], stream.read().toByte())
        } finally {
            stream.close()
        }
    }

    @Test
    fun skipMovesTheReadPositionForward() {
        val stream = openStream()
        try {
            assertEquals("Skipping three bytes", 3L, stream.skip(3))
            assertEquals(content[3], stream.read().toByte())
        } finally {
            stream.close()
        }
    }

    @Test
    fun readWithAnArrayOffsetReadsUpToLengthBytes() {
        val stream = openStream()
        try {
            val destination = ByteArray(10)
            val copied = stream.read(destination, 4, 6)

            // Six bytes were requested and five are buffered, so five arrive. This used to copy
            // `outLength - outOffset` = 2, because the offset into the destination array was
            // mistaken for a cap on how much to read.
            assertEquals(5, copied)
            assertThat(destination.copyOfRange(4, 9)).isEqualTo(content.copyOfRange(0, 5))
            // Everything outside [offset, offset + copied) is left untouched.
            assertEquals(0, destination[3].toInt())
            assertEquals(0, destination[9].toInt())
        } finally {
            stream.close()
        }
    }

    @Test
    fun readWithAZeroLengthRequestReturnsZeroInsteadOfEndOfStream() {
        val stream = openStream()
        try {
            // InputStream.read(b, off, 0) reports 0 bytes read, not EOF. This used to fall through
            // to the end-of-stream return and hand back -1.
            assertEquals(0, stream.read(ByteArray(4), 0, 0))
            assertEquals(0, stream.read(ByteArray(4), 2, 0))
        } finally {
            stream.close()
        }
    }

    @Test
    fun openingAMissingFileFails() {
        server.close()
        server = FakeFileServer(content, chunkSize, fileId = 0).start()

        assertThrows(IOException::class.java) { openStream() }
    }

    @Test
    fun openingAFileThatTheServerNeverAnswersThrowsInsteadOfHanging() {
        // The server accepts the connection but never replies to the fileOpen request. The
        // constructor must fail with an IOException once the (lowered) timeout elapses rather
        // than block the calling thread forever.
        server.close()
        server = FakeFileServer(content, chunkSize, answerFileOpen = false).start()

        assertThrows(IOException::class.java) { openStream() }
    }

    private fun openStream(): HtspFileInputStream {
        val connectionData = HtspConnectionData(
            username = "testuser",
            password = "testpassword",
            serverUrl = server.url,
            versionName = "test",
            versionCode = 1,
            connectionTimeout = 5_000,
        )
        val htspConnection = HtspConnection(connectionData, NoOpStateListener, null)
        connection = htspConnection
        htspConnection.openConnection()
        return HtspFileInputStream(htspConnection, "/recordings/test.ts")
    }

    private fun readFully(stream: HtspFileInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4)
        while (true) {
            val read = stream.read(buffer, 0, buffer.size)
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private object NoOpStateListener : ServerConnectionStateListener {
        override fun onAuthenticationStateChange(result: AuthenticationStateResult) = Unit
        override fun onConnectionStateChange(result: ConnectionStateResult) = Unit
    }

    /**
     * A minimal in-process HTSP server that answers the three file transfer methods that
     * [HtspFileInputStream] uses. It deliberately does not implement the handshake - the stream
     * only needs a connected [HtspConnection]. The file content is served in [chunkSize] blocks
     * so the client's buffering and offset handling are exercised.
     */
    private class FakeFileServer(
        private val content: ByteArray,
        private val chunkSize: Int,
        private val fileId: Long = 42L,
        private val answerFileOpen: Boolean = true,
    ) : AutoCloseable {

        private val serverSocket = ServerSocketChannel.open()
        private val running = AtomicBoolean(false)
        private val clients = CopyOnWriteArrayList<SocketChannel>()
        private var acceptThread: Thread? = null

        val openCount = AtomicInteger()
        val readCount = AtomicInteger()
        val closeCount = AtomicInteger()
        val requestedOffsets = CopyOnWriteArrayList<Long>()

        val url: String
            get() = "http://127.0.0.1:${serverSocket.socket().localPort}"

        fun start(): FakeFileServer {
            serverSocket.socket().bind(InetSocketAddress("127.0.0.1", 0))
            serverSocket.configureBlocking(true)
            running.set(true)
            acceptThread = Thread({ acceptLoop() }, "FakeFileServer-accept").apply {
                isDaemon = true
                start()
            }
            return this
        }

        private fun acceptLoop() {
            while (running.get()) {
                val client = try {
                    serverSocket.accept()
                } catch (_: IOException) {
                    break
                }
                clients.add(client)
                Thread({ handleClient(client) }, "FakeFileServer-client").apply {
                    isDaemon = true
                    start()
                }
            }
        }

        private fun handleClient(channel: SocketChannel) {
            val buffer = ByteBuffer.allocate(1 shl 20)
            buffer.limit(4)
            try {
                while (running.get() && channel.isOpen) {
                    val read = channel.read(buffer)
                    if (read < 0) break
                    val message = HtspMessage.parse(buffer) ?: continue
                    handleMessage(channel, message)
                }
            } catch (_: IOException) {
                // The client closed the connection.
            } finally {
                clients.remove(channel)
                try {
                    channel.close()
                } catch (_: IOException) {
                    // Already closed.
                }
            }
        }

        private fun handleMessage(channel: SocketChannel, message: HtspMessage) {
            when (message.getMethod()) {
                "fileOpen" -> {
                    openCount.incrementAndGet()
                    // A server that "never answers" simply leaves the request unreplied to.
                    if (answerFileOpen) {
                        reply(channel, "fileOpen", message) {
                            it.put("id", fileId)
                            it.put("size", content.size.toLong())
                            it.put("mtime", 1_700_000_000L)
                        }
                    }
                }
                "fileRead" -> {
                    readCount.incrementAndGet()
                    val offset = message.getLong("offset", 0)
                    val size = message.getLong("size", 0)
                    requestedOffsets.add(offset)
                    reply(channel, "fileRead", message) {
                        it.put("data", readChunk(offset, size))
                    }
                }
                "fileClose" -> {
                    closeCount.incrementAndGet()
                    reply(channel, "fileClose", message) { }
                }
                else -> Unit
            }
        }

        private fun reply(channel: SocketChannel, method: String, request: HtspMessage, fill: (HtspMessage) -> Unit) {
            val response = HtspMessage()
            response.setMethod(method)
            response.put("seq", request.getInteger("seq", 0))
            fill(response)
            response.transmit(channel)
        }

        /** Serves at most [chunkSize] bytes honouring the requested [offset]. */
        private fun readChunk(offset: Long, requested: Long): ByteArray {
            if (offset >= content.size) {
                return ByteArray(0)
            }
            val remaining = content.size - offset
            val length = minOf(remaining, requested, chunkSize.toLong()).toInt()
            val start = offset.toInt()
            return content.copyOfRange(start, start + length)
        }

        override fun close() {
            running.set(false)
            for (client in clients) {
                try {
                    client.close()
                } catch (_: IOException) {
                    // Already closed.
                }
            }
            clients.clear()
            try {
                serverSocket.close()
            } catch (_: IOException) {
                // Already closed.
            }
            acceptThread?.join(1_000)
            acceptThread = null
        }
    }

    companion object {
        init {
            // Keep Timber from writing to Logcat while the tests run.
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
            })
        }
    }
}
