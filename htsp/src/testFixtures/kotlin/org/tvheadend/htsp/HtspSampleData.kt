package org.tvheadend.htsp

/**
 * The canned metadata that [FakeHtspServer] serves to a client once it has authenticated.
 *
 * This is deliberately small and reviewable: a couple of tags, three channels, a handful of
 * EPG events, five recordings (including two failed ones) and one series recording are enough
 * to exercise the whole post-authentication data flow without needing a real TVHeadend or any
 * network.
 *
 * The key names used by the builders below mirror exactly what TVHClient reads in its
 * `HtspUtils` converters (e.g. `channelId`/`channelNumber`/`channelName`, `tagId`/`members`,
 * `eventId`/`start`/`stop`). The messages produced here never carry a `seq`; the server adds
 * it only to messages that answer a specific client request. The pushed messages
 * (`tagAdd`/`channelAdd`/`eventAdd`/`initialSyncCompleted`) must not have a `seq`, otherwise
 * the client would route them to a response handler instead of the message listeners.
 */
class HtspSampleData(
    val tags: List<SampleTag> = defaultTags(),
    val channels: List<SampleChannel> = defaultChannels(),
    val events: List<SampleEvent> = defaultEvents(),
    val recordings: List<SampleRecording> = defaultRecordings(),
    val seriesRecordings: List<SampleSeriesRecording> = defaultSeriesRecordings(),
    val dvrConfigs: List<SampleDvrConfig> = defaultDvrConfigs(),
    val profiles: List<SampleProfile> = defaultProfiles(),
    val freeDiskSpace: Long = 100L * 1024 * 1024 * 1024,
    val totalDiskSpace: Long = 500L * 1024 * 1024 * 1024,
) {

    // ---- Initial sync pushes (no seq) -------------------------------------------------

    fun tagAddMessages(): List<HtspMessage> = tags.map { it.toMessage("tagAdd") }

    fun channelAddMessages(): List<HtspMessage> = channels.map { it.toMessage("channelAdd") }

    fun eventAddMessages(): List<HtspMessage> = events.map { it.toMessage("eventAdd") }

    fun dvrEntryAddMessages(): List<HtspMessage> = recordings.map { it.toMessage("dvrEntryAdd") }

    fun autorecEntryAddMessages(): List<HtspMessage> = seriesRecordings.map { it.toMessage("autorecEntryAdd") }

    // ---- Replies to explicit requests (server adds the seq) ---------------------------

    fun channel(channelId: Int): HtspMessage? =
        channels.firstOrNull { it.channelId == channelId }?.toMessage("getChannel")

    fun event(eventId: Int): HtspMessage? =
        events.firstOrNull { it.eventId == eventId }?.toMessage("getEvent")

    fun events(): HtspMessage = HtspMessage().apply {
        setMethod("getEvents")
        put("events", ArrayList(events.map { it.toMessage("") }))
    }

    fun profiles(): HtspMessage = HtspMessage().apply {
        setMethod("getProfiles")
        put("profiles", ArrayList(profiles.map { it.toMessage() }))
    }

    fun dvrConfigs(): HtspMessage = HtspMessage().apply {
        setMethod("getDvrConfigs")
        put("dvrconfigs", ArrayList(dvrConfigs.map { it.toMessage() }))
    }

    fun diskSpace(): HtspMessage = HtspMessage().apply {
        setMethod("getDiskSpace")
        put("freediskspace", freeDiskSpace)
        put("totaldiskspace", totalDiskSpace)
    }

    fun systemTime(): HtspMessage = HtspMessage().apply {
        setMethod("getSysTime")
        put("time", System.currentTimeMillis() / 1000L)
        put("gmtoffset", 60)
    }

    /** Answer to an `api` request for `path=profile/list`. */
    fun httpProfiles(): HtspMessage {
        val entries = profiles.joinToString(separator = ",") {
            """{"key":"${it.uuid}","val":"${it.name}"}"""
        }
        return HtspMessage().apply {
            setMethod("api")
            put("response", """{"entries":[$entries]}""")
        }
    }

    // ---- Data holders -----------------------------------------------------------------

    data class SampleTag(
        val tagId: Int,
        val tagName: String,
        val tagIndex: Int = tagId,
        val tagIcon: String = "",
        val tagTitledIcon: Int = 0,
        val members: List<Int> = emptyList(),
    ) {
        fun toMessage(method: String): HtspMessage = HtspMessage().apply {
            setMethod(method)
            put("tagId", tagId)
            put("tagName", tagName)
            put("tagIndex", tagIndex)
            put("tagTitledIcon", tagTitledIcon)
            if (tagIcon.isNotEmpty()) put("tagIcon", tagIcon)
            put("members", ArrayList(members))
        }
    }

    data class SampleChannel(
        val channelId: Int,
        val channelNumber: Int,
        val channelName: String,
        val channelNumberMinor: Int = 0,
        val channelIcon: String = "",
        val eventId: Int = 0,
        val nextEventId: Int = 0,
        val tags: List<Int> = emptyList(),
    ) {
        fun toMessage(method: String): HtspMessage = HtspMessage().apply {
            setMethod(method)
            put("channelId", channelId)
            put("channelNumber", channelNumber)
            put("channelNumberMinor", channelNumberMinor)
            put("channelName", channelName)
            if (channelIcon.isNotEmpty()) put("channelIcon", channelIcon)
            if (eventId > 0) put("eventId", eventId)
            if (nextEventId > 0) put("nextEventId", nextEventId)
            put("tags", ArrayList(tags))
        }
    }

    data class SampleEvent(
        val eventId: Int,
        val channelId: Int,
        val start: Long,
        val stop: Long,
        val title: String,
        val subtitle: String = "",
        val summary: String = "",
        val description: String = "",
        val contentType: Int = 0,
    ) {
        fun toMessage(method: String): HtspMessage = HtspMessage().apply {
            if (method.isNotEmpty()) setMethod(method)
            put("eventId", eventId)
            put("channelId", channelId)
            put("start", start)
            put("stop", stop)
            put("title", title)
            if (subtitle.isNotEmpty()) put("subtitle", subtitle)
            if (summary.isNotEmpty()) put("summary", summary)
            if (description.isNotEmpty()) put("description", description)
            if (contentType > 0) put("contentType", contentType)
        }
    }

    data class SampleRecording(
        val id: Int,
        val channelId: Int,
        val start: Long,
        val stop: Long,
        val title: String,
        val state: String,
        val summary: String = "Sample recording generated by the fake server",
        val startExtra: Long = 0,
        val stopExtra: Long = 0,
        val priority: Int = 2,
        val error: String? = null,
    ) {
        fun toMessage(method: String): HtspMessage = HtspMessage().apply {
            if (method.isNotEmpty()) setMethod(method)
            put("id", id)
            put("channel", channelId) // the client reads the channel id from the "channel" field
            put("start", start)
            put("stop", stop)
            put("startExtra", startExtra)
            put("stopExtra", stopExtra)
            put("priority", priority)
            put("title", title)
            put("summary", summary)
            put("state", state)
            // Only failed entries carry an error: without the field the entry stays in the
            // scheduled/completed tabs.
            if (error != null) put("error", error)
        }
    }

    data class SampleSeriesRecording(
        val id: String,
        val name: String,
        val channelId: Int,
        val title: String? = null,
        val enabled: Int = 1,
        val daysOfWeek: Int = 127,
        val priority: Int = 2,
        val start: Long = 0,
        val startWindow: Long = 0,
        val startExtra: Long = 0,
        val stopExtra: Long = 0,
        val retention: Int = 0,
    ) {
        fun toMessage(method: String): HtspMessage = HtspMessage().apply {
            if (method.isNotEmpty()) setMethod(method)
            put("id", id)
            put("enabled", enabled)
            put("name", name)
            put("title", title ?: name)
            put("channel", channelId)
            put("daysOfWeek", daysOfWeek)
            put("priority", priority)
            put("start", start)
            put("startWindow", startWindow)
            put("startExtra", startExtra)
            put("stopExtra", stopExtra)
            put("retention", retention)
        }
    }

    data class SampleDvrConfig(
        val name: String,
        val uuid: String = "",
        val comment: String = "",
    ) {
        fun toMessage(): HtspMessage = HtspMessage().apply {
            put("name", name)
            put("uuid", uuid)
            put("comment", comment)
        }
    }

    data class SampleProfile(
        val name: String,
        val uuid: String = "",
        val comment: String = "",
    ) {
        fun toMessage(): HtspMessage = HtspMessage().apply {
            put("name", name)
            put("uuid", uuid)
            put("comment", comment)
        }
    }

    companion object {
        /** Fixed reference time (epoch seconds) so the sample EPG is deterministic. */
        const val BASE_TIME = 1_700_000_000L
    }
}

private fun defaultTags(): List<HtspSampleData.SampleTag> = listOf(
    HtspSampleData.SampleTag(tagId = 1, tagName = "Free TV", tagIndex = 1, members = listOf(1, 2, 3)),
    HtspSampleData.SampleTag(tagId = 2, tagName = "Radio", tagIndex = 2, members = listOf(3)),
)

private fun defaultChannels(): List<HtspSampleData.SampleChannel> = listOf(
    HtspSampleData.SampleChannel(
        channelId = 1, channelNumber = 1, channelName = "Das Erste HD",
        eventId = 1001, nextEventId = 1002, tags = listOf(1),
    ),
    HtspSampleData.SampleChannel(
        channelId = 2, channelNumber = 2, channelName = "ZDF HD",
        eventId = 1011, nextEventId = 1012, tags = listOf(1),
    ),
    HtspSampleData.SampleChannel(
        channelId = 3, channelNumber = 3, channelName = "Radio Bremen",
        eventId = 1021, nextEventId = 1022, tags = listOf(1, 2),
    ),
)

private fun defaultEvents(): List<HtspSampleData.SampleEvent> {
    val base = HtspSampleData.BASE_TIME
    return listOf(
        HtspSampleData.SampleEvent(1001, channelId = 1, start = base, stop = base + 3600, title = "Tagesschau", subtitle = "Nachrichten", summary = "Die Nachrichten des Tages"),
        HtspSampleData.SampleEvent(1002, channelId = 1, start = base + 3600, stop = base + 7200, title = "Tatort", subtitle = "Krimireihe"),
        HtspSampleData.SampleEvent(1011, channelId = 2, start = base, stop = base + 1800, title = "heute", subtitle = "Nachrichten"),
        HtspSampleData.SampleEvent(1012, channelId = 2, start = base + 1800, stop = base + 5400, title = "Bares fuer Rares"),
        HtspSampleData.SampleEvent(1021, channelId = 3, start = base, stop = base + 3600, title = "Buten un Binnen", subtitle = "Regionalmagazin"),
        HtspSampleData.SampleEvent(1022, channelId = 3, start = base + 3600, stop = base + 7200, title = "Sportblitz"),
    )
}

private fun defaultRecordings(): List<HtspSampleData.SampleRecording> {
    val base = HtspSampleData.BASE_TIME
    return listOf(
        HtspSampleData.SampleRecording(
            1, channelId = 1, start = base + 3600, stop = base + 7200,
            title = "Scheduled Demo Recording", state = "scheduled",
        ),
        HtspSampleData.SampleRecording(
            2, channelId = 2, start = base - 600, stop = base + 3000,
            title = "Running Demo Recording", state = "recording",
        ),
        HtspSampleData.SampleRecording(
            3, channelId = 3, start = base - 7200, stop = base - 3600,
            title = "Completed Demo Recording", state = "completed",
        ),
        // Two failed recordings so the DVR "failed" tab is populated too. The client treats an
        // entry as failed when it carries an error and is "missed"/"invalid", or when it was
        // "completed" but aborted by the user (see RecordingDao.loadFailedRecordings).
        HtspSampleData.SampleRecording(
            4, channelId = 1, start = base - 10_800, stop = base - 7_200,
            title = "Missed Demo Recording", state = "missed", error = "Subscription failed",
        ),
        HtspSampleData.SampleRecording(
            5, channelId = 2, start = base - 5_400, stop = base - 1_800,
            title = "Aborted Demo Recording", state = "completed", error = "Aborted by user",
        ),
    )
}

private fun defaultSeriesRecordings(): List<HtspSampleData.SampleSeriesRecording> = listOf(
    HtspSampleData.SampleSeriesRecording(
        id = "demo-series-1",
        name = "Demo Series Recording",
        channelId = 1,
    ),
)

private fun defaultDvrConfigs(): List<HtspSampleData.SampleDvrConfig> = listOf(
    HtspSampleData.SampleDvrConfig(name = "Default Profile", comment = "Default recording profile"),
)

private fun defaultProfiles(): List<HtspSampleData.SampleProfile> = listOf(
    HtspSampleData.SampleProfile(name = "htsp", comment = "HTSP streaming profile"),
    HtspSampleData.SampleProfile(name = "matroska", comment = "HTTP matroska profile"),
)
