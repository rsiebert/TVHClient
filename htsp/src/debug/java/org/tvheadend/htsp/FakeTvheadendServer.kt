package org.tvheadend.htsp

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

/**
 * A self-contained, in-process fake TVHeadend HTSP server for **debug builds only**.
 *
 * It speaks just enough HTSP for the app to authenticate and perform its initial sync, and
 * always answers with the same demo data (generated relative to "now" so it shows up in the
 * EPG and recordings screens):
 *
 *  - 3 channels in a single "Demo" tag
 *  - a full day of 1-hour EPG events per channel, starting at the top of the current hour
 *  - 3 recordings (one `scheduled`, one `recording`, one `completed`)
 *
 * Because it lives in the `htsp` module's debug source set it can reuse the package-private
 * [HtspMessage.transmit] / [HtspMessage.parse] framing and is never included in release builds.
 */
class FakeTvheadendServer(
    private val host: String = DEFAULT_HOST,
    private val port: Int = DEFAULT_PORT,
    private val hoursOfEpg: Int = 12,
) : AutoCloseable {

    private val running = AtomicBoolean(false)
    private val clients = CopyOnWriteArrayList<SocketChannel>()
    private var serverChannel: ServerSocketChannel? = null
    private var acceptThread: Thread? = null

    /** The port the server is actually listening on, or -1 before [start]. */
    @Volatile
    var boundPort: Int = -1
        private set

    /** The url the app should use for a connection to this server. */
    val url: String
        get() = "http://$host:$boundPort"

    @Synchronized
    fun start(): FakeTvheadendServer {
        if (running.get()) {
            return this
        }
        val channel = ServerSocketChannel.open()
        channel.configureBlocking(true)
        channel.socket().reuseAddress = true
        channel.socket().bind(InetSocketAddress(host, port))
        boundPort = channel.socket().localPort
        serverChannel = channel
        running.set(true)

        acceptThread = Thread({ acceptLoop(channel) }, "FakeTvheadendServer-accept").apply {
            isDaemon = true
            start()
        }
        Timber.i("Fake TVHeadend server listening on port $boundPort")
        return this
    }

    private fun acceptLoop(server: ServerSocketChannel) {
        while (running.get()) {
            val client = try {
                server.accept()
            } catch (e: IOException) {
                break // server socket closed
            }
            clients.add(client)
            Thread({ handleClient(client) }, "FakeTvheadendServer-client").apply {
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
            // client closed the connection
        } finally {
            clients.remove(channel)
            try {
                channel.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun handleMessage(channel: SocketChannel, message: HtspMessage) {
        val now = System.currentTimeMillis() / 1000L
        when (message.getMethod()) {
            "hello" -> {
                val reply = HtspMessage()
                reply.setMethod("serverStatus")
                reply.put("seq", message.getInteger("seq", 0))
                reply.put("challenge", CHALLENGE)
                reply.put("servername", "Fake TVHeadend")
                reply.put("serverversion", "4.3-demo")
                reply.put("htspversion", HtspMessage.HTSP_VERSION)
                reply.put("api_version", 2)
                reply.put("webroot", "/")
                reply.put("servercapability", arrayListOf("timeshift", "tvh-http-api"))
                send(channel, reply)
            }
            "authenticate" -> {
                // Accept anything: this server is only meant to make the UI show data.
                val reply = HtspMessage()
                reply.setMethod("authenticate")
                reply.put("seq", message.getInteger("seq", 0))
                reply.put("noaccess", 0)
                reply.put("admin", 1)
                reply.put("streaming", 1)
                reply.put("dvr", 1)
                reply.put("faileddvr", 0)
                reply.put("anonymous", 1)
                reply.put("limitall", 8)
                reply.put("limitdvr", 4)
                reply.put("limitstreaming", 2)
                reply.put("uilevel", 3)
                reply.put("uilanguage", "en")
                send(channel, reply)
            }
            "enableAsyncMetadata" -> {
                send(channel, ack(message, "enableAsyncMetadata"))
                for (tag in buildTags()) send(channel, tag)
                for (ch in buildChannels()) send(channel, ch)
                for (event in buildEventPayloads(now)) {
                    event.setMethod("eventAdd")
                    send(channel, event)
                }
                for (recording in buildRecordings(now)) send(channel, recording)
                val done = HtspMessage()
                done.setMethod("initialSyncCompleted")
                send(channel, done)
            }
            "getDiskSpace" -> send(channel, withSeq(message, HtspMessage().apply {
                setMethod("getDiskSpace")
                put("freediskspace", 50L * 1024 * 1024 * 1024)
                put("totaldiskspace", 100L * 1024 * 1024 * 1024)
            }))
            "getSysTime" -> send(channel, withSeq(message, HtspMessage().apply {
                setMethod("getSysTime")
                put("time", now)
                put("gmtoffset", 0)
            }))
            "getProfiles" -> send(channel, withSeq(message, HtspMessage().apply {
                setMethod("getProfiles")
                put("profiles", arrayListOf(profile("htsp"), profile("matroska")))
            }))
            "getDvrConfigs" -> send(channel, withSeq(message, HtspMessage().apply {
                setMethod("getDvrConfigs")
                put("dvrconfigs", arrayListOf(dvrConfig()))
            }))
            "getChannel" -> {
                val channelId = message.getInteger("channelId", 0)
                buildChannels().firstOrNull { it.getInteger("channelId", -1) == channelId }?.let {
                    it.setMethod("getChannel")
                    send(channel, withSeq(message, it))
                }
            }
            "getEvent" -> {
                val eventId = message.getInteger("eventId", 0)
                buildEventPayloads(now).firstOrNull { it.getInteger("eventId", -1) == eventId }?.let {
                    it.setMethod("getEvent")
                    send(channel, withSeq(message, it))
                }
            }
            "getEvents" -> send(channel, withSeq(message, HtspMessage().apply {
                setMethod("getEvents")
                put("events", ArrayList(buildEventPayloads(now)))
            }))
            "api" -> {
                if (message.getString("path", "") == "profile/list") {
                    send(channel, withSeq(message, HtspMessage().apply {
                        setMethod("api")
                        put("response", """{"entries":[{"key":"","val":"pass"},{"key":"","val":"matroska"}]}""")
                    }))
                } else {
                    send(channel, ack(message, "api"))
                }
            }
            else -> Unit // Ignore everything else (playback, status, ...).
        }
    }

    // ---- Demo data (relative to "now") ------------------------------------------------

    private fun buildTags(): List<HtspMessage> = listOf(
        HtspMessage().apply {
            setMethod("tagAdd")
            put("tagId", 1)
            put("tagName", "Demo")
            put("tagIndex", 1)
            put("members", arrayListOf(1, 2, 3))
        },
    )

    private fun buildChannels(): List<HtspMessage> = DEMO_CHANNEL_NAMES.mapIndexed { index, name ->
        val channelId = index + 1
        HtspMessage().apply {
            setMethod("channelAdd")
            put("channelId", channelId)
            put("channelNumber", channelId)
            put("channelNumberMinor", 0)
            put("channelName", name)
            put("eventId", channelId * 1000)
            put("nextEventId", channelId * 1000 + 1)
            put("tags", arrayListOf(1))
        }
    }

    /** Event payloads without a method so they can be pushed as `eventAdd` or listed in `getEvents`. */
    private fun buildEventPayloads(now: Long): List<HtspMessage> {
        val base = now - (now % 3600) // top of the current hour
        val events = ArrayList<HtspMessage>(DEMO_CHANNEL_NAMES.size * hoursOfEpg)
        for (channelId in 1..DEMO_CHANNEL_NAMES.size) {
            for (hour in 0 until hoursOfEpg) {
                val start = base + hour * 3600L
                events.add(HtspMessage().apply {
                    put("eventId", channelId * 1000 + hour)
                    put("channelId", channelId)
                    put("start", start)
                    put("stop", start + 3600L)
                    put("title", "${DEMO_CHANNEL_NAMES[channelId - 1]}: Show ${hour + 1}")
                    put("subtitle", if (hour == 0) "Now" else "Upcoming")
                    put("summary", "Demo EPG event ${hour + 1} on ${DEMO_CHANNEL_NAMES[channelId - 1]}")
                    put("description", "Auto-generated demo EPG data.")
                    put("contentType", 16)
                })
            }
        }
        return events
    }

    private fun buildRecordings(now: Long): List<HtspMessage> = listOf(
        recording(1, 1, "Scheduled Demo Recording", "scheduled", now + 3600, now + 7200),
        recording(2, 2, "Running Demo Recording", "recording", now - 600, now + 3000),
        recording(3, 3, "Completed Demo Recording", "completed", now - 7200, now - 3600),
    )

    private fun recording(id: Int, channelId: Int, title: String, state: String, start: Long, stop: Long) =
        HtspMessage().apply {
            setMethod("dvrEntryAdd")
            put("id", id)
            put("channel", channelId) // the client reads the channel id from the "channel" field
            put("start", start)
            put("stop", stop)
            put("startExtra", 0L)
            put("stopExtra", 0L)
            put("priority", 2)
            put("title", title)
            put("summary", "Demo recording generated by the fake server.")
            put("state", state)
            // no "error" field on purpose: keeps the entry in the scheduled/completed tabs
        }

    private fun profile(name: String) = HtspMessage().apply {
        put("name", name)
        put("uuid", "")
        put("comment", "")
    }

    private fun dvrConfig() = HtspMessage().apply {
        put("name", "Default Profile")
        put("uuid", "")
        put("comment", "")
    }

    // ---- Helpers ----------------------------------------------------------------------

    private fun send(channel: SocketChannel, message: HtspMessage) {
        message.transmit(channel)
    }

    private fun withSeq(request: HtspMessage, reply: HtspMessage): HtspMessage {
        reply.put("seq", request.getInteger("seq", 0))
        return reply
    }

    private fun ack(request: HtspMessage, method: String): HtspMessage {
        val reply = HtspMessage()
        reply.setMethod(method)
        reply.put("seq", request.getInteger("seq", 0))
        return reply
    }

    override fun close() {
        running.set(false)
        for (client in clients) {
            try {
                client.close()
            } catch (_: IOException) {
            }
        }
        clients.clear()
        try {
            serverChannel?.close()
        } catch (_: IOException) {
        }
        serverChannel = null
        acceptThread?.join(1_000)
        acceptThread = null
    }

    companion object {
        const val DEFAULT_HOST = "127.0.0.1"
        const val DEFAULT_PORT = 19982

        private val CHALLENGE = ByteArray(16) { (it + 1).toByte() }
        private val DEMO_CHANNEL_NAMES = listOf("Demo One HD", "Demo Two HD", "Demo Three HD")
    }
}
