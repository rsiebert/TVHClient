package org.tvheadend.htsp

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import timber.log.Timber

/**
 * A self-contained, in-process fake TVHeadend HTSP server for **debug builds only**.
 *
 * It speaks just enough HTSP for the app to authenticate and perform its initial sync, and
 * always answers with the same demo data (generated relative to "now" so it shows up in the
 * EPG and recordings screens):
 *
 *  - 6 channels in a single "Demo" tag, each with a generated channel icon served over the
 *    HTSP file API (`fileOpen` / `fileRead` / `fileClose`)
 *  - a week of EPG events per channel, starting at the top of the current hour and tiling the
 *    timeline with programs of differing lengths (15 minutes to 2 hours) so the guide looks
 *    realistic instead of a uniform row of one-hour blocks
 *  - 5 recordings (one `scheduled`, one `recording`, one `completed`, two failed). Their content is
 *    the clip handed to the constructor, or a generated silent WAV when there is none
 *  - 1 series recording (an enabled `autorecEntryAdd`)
 *
 * Because it lives in the `htsp` module's debug source set it can reuse the package-private
 * [HtspMessage.transmit] / [HtspMessage.parse] framing and is never included in release builds.
 */
class FakeTvheadendServer(
    private val host: String = DEFAULT_HOST,
    private val port: Int = DEFAULT_PORT,
    private val hoursOfEpg: Int = 168,
    /**
     * Content served for every `dvrfile/...` path. A developer supplied clip makes the internal
     * player stream real video and audio; when nothing is supplied the generated WAV below is used
     * instead, so the demo still reaches a ready state on a machine without any media to hand.
     */
    private val recordingContent: ByteArray? = null,
) : AutoCloseable {

    private val running = AtomicBoolean(false)
    private val clients = CopyOnWriteArrayList<SocketChannel>()
    private var serverChannel: ServerSocketChannel? = null
    private var acceptThread: Thread? = null

    /** Files opened through the HTSP file API, keyed by the id handed out to the client. */
    private val openFiles = ConcurrentHashMap<Int, ByteArray>()

    /** Source of the ids used to track files opened through the HTSP file API. */
    private val nextFileId = AtomicInteger(1)

    /** Generated channel icons, cached per requested path so repeated downloads are cheap. */
    private val iconCache = ConcurrentHashMap<String, ByteArray>()

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
                for (seriesRecording in buildSeriesRecordings()) send(channel, seriesRecording)
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
            "fileOpen" -> {
                // Channel icons and recordings are fetched over the HTSP file API: a non-http url is
                // read through HtspFileInputStream while the negotiated htspversion is above 9.
                // A demo recording ("dvrfile/<id>") yields the supplied clip, or a generated WAV
                // when there is none, so the internal player can reach a ready state. Everything
                // else is an icon.
                val path = message.getString("file", "")
                val data = if (path.startsWith("dvrfile")) recordingBytes() else iconBytes(path)
                val fileId = nextFileId.getAndIncrement()
                openFiles[fileId] = data
                send(channel, withSeq(message, HtspMessage().apply {
                    setMethod("fileOpen")
                    put("id", fileId)
                    put("size", data.size.toLong())
                    put("mtime", now)
                }))
            }
            "fileRead" -> send(channel, withSeq(message, HtspMessage().apply {
                setMethod("fileRead")
                put("data", slice(openFiles[message.getInteger("id", 0)], message.getLong("offset", 0), message.getInteger("size", 0)))
            }))
            "fileClose" -> {
                openFiles.remove(message.getInteger("id", 0))
                send(channel, withSeq(message, HtspMessage().apply { setMethod("fileClose") }))
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
            put("members", arrayListOf(1, 2, 3, 4, 5, 6))
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
            put("channelIcon", DEMO_CHANNEL_ICON_PATHS[index])
            put("eventId", channelId * 1000)
            put("nextEventId", channelId * 1000 + 1)
            put("tags", arrayListOf(1))
        }
    }

    /**
     * Event payloads without a method so they can be pushed as `eventAdd` or listed in `getEvents`.
     *
     * The programs of a channel start at the top of the current hour and tile the timeline without
     * gaps until they cover [hoursOfEpg] hours. The first program always lasts one hour, so it is
     * the program that is currently airing and the channel's `eventId`/`nextEventId` point at the
     * right entries. Every following program takes its length from
     * [DEMO_PROGRAM_DURATIONS_MINUTES], so the guide shows a realistic mix of short and long
     * programs instead of a uniform row of one-hour blocks.
     */
    private fun buildEventPayloads(now: Long): List<HtspMessage> {
        val base = now - (now % 3600) // top of the current hour
        val timelineEnd = base + hoursOfEpg * 3600L
        val events = ArrayList<HtspMessage>()
        for (channelId in 1..DEMO_CHANNEL_NAMES.size) {
            val channelName = DEMO_CHANNEL_NAMES[channelId - 1]
            var start = base
            var index = 0
            while (start < timelineEnd) {
                // The first program covers "now" and is always a whole hour; the rest vary.
                val duration = if (index == 0) {
                    3600L
                } else {
                    DEMO_PROGRAM_DURATIONS_MINUTES[(channelId + index) % DEMO_PROGRAM_DURATIONS_MINUTES.size] * 60L
                }
                val stop = start + duration
                events.add(HtspMessage().apply {
                    put("eventId", channelId * 1000 + index)
                    put("channelId", channelId)
                    put("start", start)
                    put("stop", stop)
                    put("title", "$channelName: Show ${index + 1}")
                    put("subtitle", when {
                        now < start -> "Upcoming"
                        now < stop -> "Now"
                        else -> "Past"
                    })
                    put("summary", "Demo EPG event ${index + 1} on $channelName, " +
                            "${(stop - start) / 60} minutes long")
                    put("description", "Auto-generated demo EPG data.")
                    put("contentType", DEMO_CONTENT_TYPES[index % DEMO_CONTENT_TYPES.size])
                    put("ageRating", DEMO_AGE_RATINGS[index % DEMO_AGE_RATINGS.size])
                })
                start = stop
                index++
            }
        }
        return events
    }

    private fun buildRecordings(now: Long): List<HtspMessage> = listOf(
        recording(1, 1, "Scheduled Demo Recording", "scheduled", now + 3600, now + 7200),
        recording(2, 2, "Running Demo Recording", "recording", now - 600, now + 3000),
        recording(3, 3, "Completed Demo Recording", "completed", now - 7200, now - 3600),
        // Two failed recordings so the DVR "failed" tab is populated too. The client treats an
        // entry as failed when it carries an error and is "missed"/"invalid", or when it was
        // "completed" but aborted by the user (see RecordingDao.loadFailedRecordings).
        recording(4, 4, "Missed Demo Recording", "missed", now - 10_800, now - 7_200, error = "Subscription failed"),
        recording(5, 5, "Aborted Demo Recording", "completed", now - 5_400, now - 1_800, error = "Aborted by user"),
    )

    private fun recording(
        id: Int,
        channelId: Int,
        title: String,
        state: String,
        start: Long,
        stop: Long,
        error: String? = null,
    ) = HtspMessage().apply {
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
        // Only failed entries carry an error: without the field the entry stays in the
        // scheduled/completed tabs.
        if (error != null) put("error", error)
    }

    /**
     * A single, enabled series recording (an `autorecEntryAdd`). The client persists it through
     * its `autorecEntryAdd` handler and shows it on the series recordings screen.
     */
    private fun buildSeriesRecordings(): List<HtspMessage> = listOf(
        HtspMessage().apply {
            setMethod("autorecEntryAdd")
            put("id", "demo-series-1")
            put("enabled", 1)
            put("name", "Demo Series Recording")
            put("title", "Demo Series Recording")
            put("channel", 1)
            put("daysOfWeek", 127) // every day
            put("priority", 2)
            put("start", 0L)
            put("startWindow", 0L)
            put("startExtra", 0L)
            put("stopExtra", 0L)
            put("retention", 0)
            put("minDuration", 0)
            put("maxDuration", 0)
            put("approxTime", 0)
            put("dupDetect", 0)
        },
    )

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

    /**
     * Returns the PNG bytes for the given icon [path], generating and caching them on demand.
     * Unknown paths still yield an empty image so the client never hangs on a missing file.
     */
    private fun iconBytes(path: String): ByteArray = iconCache.getOrPut(path) {
        val index = DEMO_CHANNEL_ICON_PATHS.indexOf(path).takeIf { it >= 0 } ?: (path.hashCode() and Int.MAX_VALUE)
        DemoChannelIconFactory.badge(DEMO_CHANNEL_ICON_COLORS[index % DEMO_CHANNEL_ICON_COLORS.size])
    }

    /**
     * The content of every demo recording: the developer supplied clip when there is one, otherwise
     * the generated WAV below.
     */
    private fun recordingBytes(): ByteArray = recordingContent ?: silentWavBytes()

    /**
     * A minimal, valid mono 8 kHz 8-bit PCM WAV of silence. It is the fallback for when no demo clip
     * is bundled: Media3's WavExtractor parses it, which lets the internal player reach a ready
     * state (with a single audio track) without shipping any real media.
     */
    private fun silentWavBytes(): ByteArray {
        val sampleCount = SAMPLE_RATE
        val buffer = ByteBuffer.allocate(WAV_HEADER_SIZE + sampleCount).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(WAV_HEADER_SIZE - 8 + sampleCount)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)              // fmt chunk size
        buffer.putShort(1.toShort())   // PCM
        buffer.putShort(1.toShort())   // mono
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE)     // byte rate of 8-bit mono audio
        buffer.putShort(1.toShort())   // block align
        buffer.putShort(8.toShort())   // bits per sample
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(sampleCount)
        repeat(sampleCount) { buffer.put(SILENCE_SAMPLE) }
        return buffer.array()
    }

    /** A bounded slice of a file opened through the HTSP file API, or an empty array if unknown. */
    private fun slice(data: ByteArray?, offset: Long, size: Int): ByteArray {
        if (data == null || size <= 0 || offset < 0 || offset >= data.size) {
            return ByteArray(0)
        }
        val start = offset.toInt()
        return data.copyOfRange(start, minOf(start + size, data.size))
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

        /** Sample rate of the generated demo recording audio. */
        private const val SAMPLE_RATE = 8000

        /** Size of a canonical WAV header (RIFF + fmt + data chunks). */
        private const val WAV_HEADER_SIZE = 44

        /** 8-bit PCM silence sits at the midpoint of the unsigned range. */
        private const val SILENCE_SAMPLE: Byte = -128

        private val CHALLENGE = ByteArray(16) { (it + 1).toByte() }
        private val DEMO_CHANNEL_NAMES = listOf(
            "Demo One HD", "Demo Two HD", "Demo Three HD",
            "Demo Four HD", "Demo Five HD", "Demo Six HD",
        )

        /** HTSP paths the app is told to load each channel's icon from. */
        private val DEMO_CHANNEL_ICON_PATHS = DEMO_CHANNEL_NAMES.indices.map { "imagecache/demo${it + 1}.png" }

        /**
         * Lengths, in minutes, used for the demo programs after the first one. The values are
         * rotated per channel so neighbouring programs rarely share a length and each channel's
         * guide looks different. The first program of every channel stays a whole hour so it always
         * covers the current time.
         */
        private val DEMO_PROGRAM_DURATIONS_MINUTES = listOf(
            30, 45, 90, 120, 25, 75, 50, 15, 100, 40, 60, 80, 35, 110, 55, 20, 70, 95, 65, 85,
        )

        /** DVB content codes used to vary the genre (and therefore the genre colour) per program. */
        private val DEMO_CONTENT_TYPES = listOf(0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80)

        /** Minimum age ratings cycled through the demo programs. */
        private val DEMO_AGE_RATINGS = listOf(0, 6, 12, 16, 18)

        /** One distinct badge colour per demo channel. */
        private val DEMO_CHANNEL_ICON_COLORS = listOf(
            0xFF1E88E5.toInt(),
            0xFFE53935.toInt(),
            0xFF43A047.toInt(),
            0xFFFB8C00.toInt(),
            0xFF8E24AA.toInt(),
            0xFF00ACC1.toInt(),
        )
    }
}
