package org.tvheadend.htsp

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The IPv4 loopback address. The fake server binds to it and the client is pointed at it,
 * so both the server and the client stay co-located in the same process / on the same device.
 */
private const val LOOPBACK_HOST = "127.0.0.1"

/**
 * A minimal, reusable in-process TVHeadend HTSP server for the TVHClient tests.
 *
 * It implements just enough of the HTSP handshake to satisfy
 * [HtspConnection.authenticate] (HtspConnection.java lines 208-278):
 *
 *  1. The client opens a TCP connection to `Uri.parse(serverUrl).host:port`.
 *  2. The client sends `hello`
 *     { method="hello", clientname="TVHClient", clientversion, htspversion=32, username }.
 *  3. The server replies with a message that carries a `challenge` (a random byte[])
 *     and echoes the client's `seq` so the client can correlate the reply with its
 *     pending request handler.
 *  4. The client sends `authenticate`
 *     { method="authenticate", username, digest = SHA1(passwordBytes + challenge) }.
 *  5. The server replies with `noaccess` (0 = authenticated, 1 = bad credentials) and
 *     echoes `seq` again.
 *
 * HTSP framing is deliberately **not** re-implemented: [HtspMessage.parse] is reused
 * for reading and [HtspMessage.transmit] for writing. Both are package private, which
 * is why this fixture also lives in the `org.tvheadend.htsp` package.
 *
 * The server binds to the loopback address on an ephemeral port so it never collides
 * with a real TVHeadend instance, another test, or the host machine.
 *
 * @param username the username the client must send in the `authenticate` message
 * @param password the cleartext password used to recompute the SHA1 digest
 * @param challenge the challenge bytes returned in the reply to `hello`
 */
class FakeHtspServer @JvmOverloads constructor(
    private val username: String = "*",
    private val password: String = "test",
    private val challenge: ByteArray = ByteArray(16) { (it + 1).toByte() },
) : AutoCloseable {

    private val running = AtomicBoolean(false)
    private val clients = CopyOnWriteArrayList<SocketChannel>()
    private var serverChannel: ServerSocketChannel? = null
    private var acceptThread: Thread? = null

    /** Number of accepted TCP connections. */
    val connectionCount = AtomicInteger()

    /** Number of `hello` messages received. */
    val helloCount = AtomicInteger()

    /** Number of `authenticate` messages received. */
    val authenticateCount = AtomicInteger()

    /** The (ephemeral) port the server is listening on, or -1 before [start]. */
    @Volatile
    var port: Int = -1
        private set

    /**
     * The url the client should be pointed at. [HtspConnection.openConnection] only
     * parses the host and the port out of it, so an `http://` url is fine here.
     */
    val url: String
        get() = "http://$LOOPBACK_HOST:$port"

    /** Starts the accept loop and returns this instance for fluent use. */
    fun start(): FakeHtspServer {
        val channel = ServerSocketChannel.open()
        channel.configureBlocking(true)
        // Port 0 => the OS picks a free ephemeral port. Bind/read the port through the
        // underlying ServerSocket because ServerSocketChannel.bind/localAddress are API 24
        // and this library supports API 23.
        channel.socket().bind(InetSocketAddress(LOOPBACK_HOST, 0))
        port = channel.socket().localPort
        serverChannel = channel
        running.set(true)

        acceptThread = Thread({ acceptLoop(channel) }, "FakeHtspServer-accept").apply {
            isDaemon = true
            start()
        }
        return this
    }

    private fun acceptLoop(server: ServerSocketChannel) {
        while (running.get()) {
            val client = try {
                server.accept()
            } catch (e: IOException) {
                // The server socket was closed, stop accepting connections.
                break
            }
            connectionCount.incrementAndGet()
            clients.add(client)
            Thread({ handleClient(client) }, "FakeHtspServer-client").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun handleClient(channel: SocketChannel) {
        val buffer = ByteBuffer.allocate(1 shl 20)
        // HtspMessage.parse expects the 4 byte length prefix to be read first and then
        // grows the buffer limit to the announced message length.
        buffer.limit(4)
        try {
            while (running.get() && channel.isOpen) {
                val read = channel.read(buffer)
                if (read < 0) {
                    // The client closed the connection.
                    break
                }
                val message = HtspMessage.parse(buffer) ?: continue
                handleMessage(channel, message)
            }
        } catch (_: IOException) {
            // The client closed the connection; only the channel needs cleaning up.
        } finally {
            clients.remove(channel)
            try {
                channel.close()
            } catch (ignore: IOException) {
                // Already closed.
            }
        }
    }

    private fun handleMessage(channel: SocketChannel, message: HtspMessage) {
        when (message.getMethod()) {
            "hello" -> {
                helloCount.incrementAndGet()
                // Handshake step 3: answer with the challenge. The client reads it via
                // getByteArray("challenge") and mixes it into the SHA1 digest.
                val reply = HtspMessage()
                reply.setMethod("serverStatus")
                reply.put("seq", message.getInteger("seq", 0))
                reply.put("challenge", challenge)
                reply.put("servername", "FakeTVHeadend")
                reply.put("htspversion", HtspMessage.HTSP_VERSION)
                send(channel, reply)
            }
            "authenticate" -> {
                authenticateCount.incrementAndGet()
                // Handshake step 5: verify the digest and report the result.
                val authorized = isAuthorized(message)
                val reply = HtspMessage()
                reply.setMethod("authenticate")
                reply.put("seq", message.getInteger("seq", 0))
                reply.put("noaccess", if (authorized) 0 else 1)
                send(channel, reply)
            }
            else -> Unit // Ignore anything that is not part of the handshake.
        }
    }

    /**
     * Recomputes `SHA1(passwordBytes + challenge)` and compares it with the digest the
     * client sent. This mirrors exactly what the client does in HtspConnection.authenticate.
     */
    private fun isAuthorized(message: HtspMessage): Boolean {
        val providedUsername = message.getString("username", "")
        val providedDigest = message.getByteArray("digest") ?: return false
        if (providedUsername != username) {
            return false
        }
        val digest = MessageDigest.getInstance("SHA1")
        digest.update(password.toByteArray())
        digest.update(challenge)
        return digest.digest().contentEquals(providedDigest)
    }

    /** Writes a message, reusing the production HTSP framing in [HtspMessage.transmit]. */
    private fun send(channel: SocketChannel, message: HtspMessage) {
        message.transmit(channel)
    }

    override fun close() {
        running.set(false)
        // Closing the accepted channels unblocks the per-client read loops.
        for (client in clients) {
            try {
                client.close()
            } catch (ignore: IOException) {
                // Already closed.
            }
        }
        clients.clear()
        try {
            serverChannel?.close()
        } catch (ignore: IOException) {
            // Already closed.
        }
        serverChannel = null
        acceptThread?.join(1_000)
        acceptThread = null
    }
}
