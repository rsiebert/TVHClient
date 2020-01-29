package org.tvheadend.tvhclient.service

import android.content.Intent
import org.tvheadend.data.entity.*
import org.tvheadend.htsp.HtspMessage
import timber.log.Timber
import java.util.*

fun convertMessageToChannelTagModel(tag: ChannelTag, msg: HtspMessage, channels: List<Channel>): ChannelTag {
    if (msg.containsKey("tagId")) {
        tag.tagId = msg.getInteger("tagId")
    }
    if (msg.containsKey("tagName")) {
        tag.tagName = msg.getString("tagName")
    }
    if (msg.containsKey("tagIndex")) {
        if (msg.getInteger("tagIndex") > 0) {
            tag.tagIndex = msg.getInteger("tagIndex")
        }
    }
    if (msg.containsKey("tagIcon")) {
        if (!msg.getString("tagIcon").isNullOrEmpty()) {
            tag.tagIcon = msg.getString("tagIcon")
        }
    }
    if (msg.containsKey("tagTitledIcon")) {
        if (msg.getInteger("tagTitledIcon") > 0) {
            tag.tagTitledIcon = msg.getInteger("tagTitledIcon")
        }
    }
    if (msg.containsKey("members")) {
        val members = msg.getIntegerList("members")
        tag.members = members

        var channelCount = 0
        for (channelId in members) {
            for ((id) in channels) {
                if (id == channelId) {
                    channelCount++
                    break
                }
            }
        }
        tag.channelCount = channelCount
    }
    return tag
}

fun convertMessageToChannelModel(channel: Channel, msg: HtspMessage): Channel {
    if (msg.containsKey("channelId")) {
        channel.id = msg.getInteger("channelId")
    }
    if (msg.containsKey("channelNumber") && msg.containsKey("channelNumberMinor")) {
        val channelNumber = msg.getInteger("channelNumber")
        val channelNumberMinor = msg.getInteger("channelNumberMinor")
        channel.number = channelNumber
        channel.numberMinor = channelNumberMinor
        channel.displayNumber = "$channelNumber.$channelNumberMinor"

    } else if (msg.containsKey("channelNumber")) {
        val channelNumber = msg.getInteger("channelNumber")
        channel.number = channelNumber
        channel.displayNumber = "$channelNumber.0"
    }
    if (msg.containsKey("channelName")) {
        channel.name = msg.getString("channelName")
    }
    if (msg.containsKey("channelIcon")) {
        if (!msg.getString("channelIcon").isNullOrEmpty()) {
            channel.icon = msg.getString("channelIcon")
        }
    }
    if (msg.containsKey("eventId")) {
        if (msg.getInteger("eventId") > 0) {
            channel.eventId = msg.getInteger("eventId")
        }
    }
    if (msg.containsKey("nextEventId")) {
        if (msg.getInteger("nextEventId") > 0) {
            channel.nextEventId = msg.getInteger("nextEventId")
        }
    }
    if (msg.containsKey("tags")) {
        val tags = msg.getIntegerList("tags")
        channel.tags = tags
    }
    return channel
}

fun convertMessageToRecordingModel(recording: Recording, msg: HtspMessage): Recording {
    if (msg.containsKey("id")) {
        recording.id = msg.getInteger("id")
    }
    if (msg.containsKey("channel")) {
        if (msg.getInteger("channel") > 0) {
            recording.channelId = msg.getInteger("channel")
        }
    }
    if (msg.containsKey("start")) {
        // The message value is in seconds, convert to milliseconds
        recording.start = msg.getLong("start") * 1000
    }
    if (msg.containsKey("stop")) {
        // The message value is in seconds, convert to milliseconds
        recording.stop = msg.getLong("stop") * 1000
    }
    if (msg.containsKey("startExtra")) {
        recording.startExtra = msg.getLong("startExtra")
    }
    if (msg.containsKey("stopExtra")) {
        recording.stopExtra = msg.getLong("stopExtra")
    }
    if (msg.containsKey("retention")) {
        recording.retention = msg.getLong("retention")
    }
    if (msg.containsKey("priority")) {
        recording.priority = msg.getInteger("priority")
    }
    if (msg.containsKey("eventId")) {
        if (msg.getInteger("eventId") > 0) {
            recording.eventId = msg.getInteger("eventId")
        }
    }
    if (msg.containsKey("autorecId")) {
        if (!msg.getString("autorecId").isNullOrEmpty()) {
            recording.autorecId = msg.getString("autorecId")
        }
    }
    if (msg.containsKey("timerecId")) {
        if (!msg.getString("timerecId").isNullOrEmpty()) {
            recording.timerecId = msg.getString("timerecId")
        }
    }
    if (msg.containsKey("contentType")) {
        if (msg.getInteger("contentType") > 0) {
            recording.contentType = msg.getInteger("contentType")
        }
    }
    if (msg.containsKey("title")) {
        if (!msg.getString("title").isNullOrEmpty()) {
            recording.title = msg.getString("title")
        }
    }
    if (msg.containsKey("subtitle")) {
        if (!msg.getString("subtitle").isNullOrEmpty()) {
            recording.subtitle = msg.getString("subtitle")
        }
    }
    if (msg.containsKey("summary")) {
        if (!msg.getString("summary").isNullOrEmpty()) {
            recording.summary = msg.getString("summary")
        }
    }
    if (msg.containsKey("description")) {
        if (!msg.getString("description").isNullOrEmpty()) {
            recording.description = msg.getString("description")
        }
    }
    if (msg.containsKey("state")) {
        recording.state = msg.getString("state")
    }
    if (msg.containsKey("error")) {
        if (!msg.getString("error").isNullOrEmpty()) {
            recording.error = msg.getString("error")
        }
    }
    if (msg.containsKey("owner")) {
        if (!msg.getString("owner").isNullOrEmpty()) {
            recording.owner = msg.getString("owner")
        }
    }
    if (msg.containsKey("creator")) {
        if (!msg.getString("creator").isNullOrEmpty()) {
            recording.creator = msg.getString("creator")
        }
    }
    if (msg.containsKey("subscriptionError")) {
        if (!msg.getString("subscriptionError").isNullOrEmpty()) {
            recording.subscriptionError = msg.getString("subscriptionError")
        }
    }
    if (msg.containsKey("streamErrors")) {
        if (!msg.getString("streamErrors").isNullOrEmpty()) {
            recording.streamErrors = msg.getString("streamErrors")
        }
    }
    if (msg.containsKey("dataErrors")) {
        if (!msg.getString("dataErrors").isNullOrEmpty()) {
            recording.dataErrors = msg.getString("dataErrors")
        }
    }
    if (msg.containsKey("path")) {
        if (!msg.getString("path").isNullOrEmpty()) {
            recording.path = msg.getString("path")
        }
    }
    if (msg.containsKey("dataSize")) {
        if (msg.getLong("dataSize") > 0) {
            recording.dataSize = msg.getLong("dataSize")
        }
    }
    if (msg.containsKey("enabled")) {
        recording.isEnabled = msg.getInteger("enabled") == 1
    }
    if (msg.containsKey("duplicate")) {
        recording.duplicate = msg.getInteger("duplicate")
    }

    if (msg.containsKey("image")) {
        if (!msg.getString("image").isNullOrEmpty()) {
            recording.image = msg.getString("image")
        }
    }
    if (msg.containsKey("fanart_image")) {
        if (!msg.getString("fanart_image").isNullOrEmpty()) {
            recording.fanartImage = msg.getString("fanart_image")
        }
    }
    if (msg.containsKey("copyright_year")) {
        if (msg.getInteger("copyright_year") > 0) {
            recording.copyrightYear = msg.getInteger("copyright_year")
        }
    }
    if (msg.containsKey("removal")) {
        if (msg.getInteger("removal") > 0) {
            recording.removal = msg.getInteger("removal")
        }
    }

    if (msg.containsKey("start") && msg.containsKey("stop")) {
        val start = msg.getLong("start")
        val stop = msg.getLong("stop")
        recording.duration = ((stop - start) / 60).toInt()
    }

    return recording
}

fun convertMessageToProgramModel(program: Program, msg: HtspMessage): Program {
    if (msg.containsKey("eventId")) {
        program.eventId = msg.getInteger("eventId")
    }
    if (msg.containsKey("channelId")) {
        program.channelId = msg.getInteger("channelId")
    }
    if (msg.containsKey("start")) {
        // The message value is in seconds, convert to milliseconds
        program.start = msg.getLong("start") * 1000
    }
    if (msg.containsKey("stop")) {
        // The message value is in seconds, convert to milliseconds
        program.stop = msg.getLong("stop") * 1000
    }
    if (msg.containsKey("title")) {
        if (!msg.getString("title").isNullOrEmpty()) {
            program.title = msg.getString("title")
        }
    }
    if (msg.containsKey("subtitle")) {
        if (!msg.getString("subtitle").isNullOrEmpty()) {
            program.subtitle = msg.getString("subtitle")
        }
    }
    if (msg.containsKey("summary")) {
        if (!msg.getString("summary").isNullOrEmpty()) {
            program.summary = msg.getString("summary")
        }
    }
    if (msg.containsKey("description")) {
        if (!msg.getString("description").isNullOrEmpty()) {
            program.description = msg.getString("description")
        }
    }
    if (msg.containsKey("serieslinkId")) {
        if (msg.getInteger("serieslinkId") > 0) {
            program.serieslinkId = msg.getInteger("serieslinkId")
        }
    }
    if (msg.containsKey("episodeId")) {
        if (msg.getInteger("episodeId") > 0) {
            program.episodeId = msg.getInteger("episodeId")
        }
    }
    if (msg.containsKey("seasonId")) {
        if (msg.getInteger("seasonId") > 0) {
            program.seasonId = msg.getInteger("seasonId")
        }
    }
    if (msg.containsKey("brandId")) {
        if (msg.getInteger("brandId") > 0) {
            program.brandId = msg.getInteger("brandId")
        }
    }
    if (msg.containsKey("contentType")) {
        if (msg.getInteger("contentType") > 0) {
            program.contentType = msg.getInteger("contentType")
        }
    }
    if (msg.containsKey("ageRating")) {
        if (msg.getInteger("ageRating") > 0) {
            program.ageRating = msg.getInteger("ageRating")
        }
    }
    if (msg.containsKey("starRating")) {
        if (msg.getInteger("starRating") > 0) {
            program.starRating = msg.getInteger("starRating")
        }
    }
    if (msg.containsKey("firstAired")) {
        if (msg.getInteger("firstAired") > 0) {
            program.firstAired = msg.getLong("firstAired")
        }
    }
    if (msg.containsKey("seasonNumber")) {
        if (msg.getInteger("seasonNumber") > 0) {
            program.seasonNumber = msg.getInteger("seasonNumber")
        }
    }
    if (msg.containsKey("seasonCount")) {
        if (msg.getInteger("seasonCount") > 0) {
            program.seasonCount = msg.getInteger("seasonCount")
        }
    }
    if (msg.containsKey("episodeNumber")) {
        if (msg.getInteger("episodeNumber") > 0) {
            program.episodeNumber = msg.getInteger("episodeNumber")
        }
    }
    if (msg.containsKey("episodeCount")) {
        if (msg.getInteger("episodeCount") > 0) {
            program.episodeCount = msg.getInteger("episodeCount")
        }
    }
    if (msg.containsKey("partNumber")) {
        if (msg.getInteger("partNumber") > 0) {
            program.partNumber = msg.getInteger("partNumber")
        }
    }
    if (msg.containsKey("partCount")) {
        if (msg.getInteger("partCount") > 0) {
            program.partCount = msg.getInteger("partCount")
        }
    }
    if (msg.containsKey("episodeOnscreen")) {
        if (!msg.getString("episodeOnscreen").isNullOrEmpty()) {
            program.episodeOnscreen = msg.getString("episodeOnscreen")
        }
    }
    if (msg.containsKey("image")) {
        if (!msg.getString("image").isNullOrEmpty()) {
            program.image = msg.getString("image")
        }
    }
    if (msg.containsKey("dvrId")) {
        if (msg.getInteger("dvrId") > 0) {
            program.dvrId = msg.getInteger("dvrId")
        }
    }
    if (msg.containsKey("nextEventId")) {
        if (msg.getInteger("nextEventId") > 0) {
            program.nextEventId = msg.getInteger("nextEventId")
        }
    }
    if (msg.containsKey("episodeOnscreen")) {
        if (!msg.getString("episodeOnscreen").isNullOrEmpty()) {
            program.episodeOnscreen = msg.getString("episodeOnscreen")
        }
    }
    if (msg.containsKey("serieslinkUri")) {
        if (!msg.getString("serieslinkUri").isNullOrEmpty()) {
            program.serieslinkUri = msg.getString("serieslinkUri")
        }
    }
    if (msg.containsKey("episodeUri")) {
        if (!msg.getString("episodeUri").isNullOrEmpty()) {
            program.episodeUri = msg.getString("episodeUri")
        }
    }
    if (msg.containsKey("copyright_year")) {
        if (msg.getInteger("copyright_year") > 0) {
            program.copyrightYear = msg.getInteger("copyright_year")
        }
    }
    program.modifiedTime = System.currentTimeMillis()
    /*
    if (msg.containsKey("credits")) {
        StringBuilder sb = new StringBuilder();
        for (String credit : msg.getStringArray("credits")) {
            sb.append(credit).append(",");
        }
        // Remove the last separator character
        program.setCredits(sb.substring(0, sb.lastIndexOf(",")));
    }
    if (msg.containsKey("category")) {
        StringBuilder sb = new StringBuilder();
        for (String s : msg.getStringArray("category")) {
            sb.append(s).append(",");
        }
        // Remove the last separator character
        program.setCredits(sb.substring(0, sb.lastIndexOf(",")));

    }
    if (msg.containsKey("keyword")) {
        StringBuilder sb = new StringBuilder();
        for (String s : msg.getStringArray("keyword")) {
            sb.append(s).append(",");
        }
        // Remove the last separator character
        program.setKeyword(sb.substring(0, sb.lastIndexOf(",")));
    }
    */
    return program
}

fun convertMessageToSeriesRecordingModel(seriesRecording: SeriesRecording, msg: HtspMessage): SeriesRecording {
    if (msg.containsKey("id")) {
        seriesRecording.id = msg.getString("id")
    }
    if (msg.containsKey("enabled")) {
        seriesRecording.isEnabled = msg.getInteger("enabled") == 1
    }
    if (msg.containsKey("name")) {
        seriesRecording.name = msg.getString("name")
    }
    if (msg.containsKey("minDuration")) {
        seriesRecording.minDuration = msg.getInteger("minDuration")
    }
    if (msg.containsKey("maxDuration")) {
        seriesRecording.maxDuration = msg.getInteger("maxDuration")
    }
    if (msg.containsKey("retention")) {
        seriesRecording.retention = msg.getInteger("retention")
    }
    if (msg.containsKey("daysOfWeek")) {
        seriesRecording.daysOfWeek = msg.getInteger("daysOfWeek")
    }
    if (msg.containsKey("priority")) {
        seriesRecording.priority = msg.getInteger("priority")
    }
    if (msg.containsKey("approxTime")) {
        seriesRecording.approxTime = msg.getInteger("approxTime")
    }
    if (msg.containsKey("start")) {
        // The message value is in minutes
        seriesRecording.start = msg.getLong("start")
    }
    if (msg.containsKey("startWindow")) {
        // The message value is in minutes
        seriesRecording.startWindow = msg.getLong("startWindow")
    }
    if (msg.containsKey("startExtra")) {
        seriesRecording.startExtra = msg.getLong("startExtra")
    }
    if (msg.containsKey("stopExtra")) {
        seriesRecording.stopExtra = msg.getLong("stopExtra")
    }
    if (msg.containsKey("title")) {
        if (!msg.getString("title").isNullOrEmpty()) {
            seriesRecording.title = msg.getString("title")
        }
    }
    if (msg.containsKey("fulltext")) {
        if (!msg.getString("fulltext").isNullOrEmpty()) {
            seriesRecording.fulltext = msg.getInteger("fulltext")
        }
    }
    if (msg.containsKey("directory")) {
        if (!msg.getString("directory").isNullOrEmpty()) {
            seriesRecording.directory = msg.getString("directory")
        }
    }
    if (msg.containsKey("channel")) {
        if (msg.getInteger("channel") > 0) {
            seriesRecording.channelId = msg.getInteger("channel")
        }
    }
    if (msg.containsKey("owner")) {
        if (!msg.getString("owner").isNullOrEmpty()) {
            seriesRecording.owner = msg.getString("owner")
        }
    }
    if (msg.containsKey("creator")) {
        if (!msg.getString("creator").isNullOrEmpty()) {
            seriesRecording.creator = msg.getString("creator")
        }
    }
    if (msg.containsKey("dupDetect")) {
        if (msg.getInteger("dupDetect") > 0) {
            seriesRecording.dupDetect = msg.getInteger("dupDetect")
        }
    }
    if (msg.containsKey("maxCount")) {
        if (msg.getInteger("maxCount") > 0) {
            seriesRecording.maxCount = msg.getInteger("maxCount")
        }
    }
    if (msg.containsKey("removal")) {
        if (msg.getInteger("removal") > 0) {
            seriesRecording.removal = msg.getInteger("removal")
        }
    }
    return seriesRecording
}

fun convertMessageToTimerRecordingModel(timerRecording: TimerRecording, msg: HtspMessage): TimerRecording {
    if (msg.containsKey("id")) {
        timerRecording.id = msg.getString("id")
    }
    if (msg.containsKey("title")) {
        timerRecording.title = msg.getString("title")
    }
    if (msg.containsKey("directory")) {
        if (!msg.getString("directory").isNullOrEmpty()) {
            timerRecording.directory = msg.getString("directory")
        }
    }
    if (msg.containsKey("enabled")) {
        timerRecording.isEnabled = msg.getInteger("enabled") == 1
    }
    if (msg.containsKey("name")) {
        timerRecording.name = msg.getString("name")
    }
    if (msg.containsKey("configName")) {
        timerRecording.configName = msg.getString("configName")
    }
    if (msg.containsKey("channel")) {
        timerRecording.channelId = msg.getInteger("channel")
    }
    if (msg.containsKey("daysOfWeek")) {
        if (msg.getInteger("daysOfWeek") > 0) {
            timerRecording.daysOfWeek = msg.getInteger("daysOfWeek")
        }
    }
    if (msg.containsKey("priority")) {
        if (msg.getInteger("priority") > 0) {
            timerRecording.priority = msg.getInteger("priority")
        }
    }
    if (msg.containsKey("start")) {
        // The message value is in minutes
        timerRecording.start = msg.getLong("start")
    }
    if (msg.containsKey("stop")) {
        // The message value is in minutes
        timerRecording.stop = msg.getLong("stop")
    }
    if (msg.containsKey("retention")) {
        if (msg.getInteger("retention") > 0) {
            timerRecording.retention = msg.getInteger("retention")
        }
    }
    if (msg.containsKey("owner")) {
        if (!msg.getString("owner").isNullOrEmpty()) {
            timerRecording.owner = msg.getString("owner")
        }
    }
    if (msg.containsKey("creator")) {
        if (!msg.getString("creator").isNullOrEmpty()) {
            timerRecording.creator = msg.getString("creator")
        }
    }
    if (msg.containsKey("removal")) {
        if (msg.getInteger("removal") > 0) {
            timerRecording.removal = msg.getInteger("removal")
        }
    }
    return timerRecording
}

fun convertMessageToServerStatusModel(serverStatus: ServerStatus, msg: HtspMessage): ServerStatus {
    if (msg.containsKey("htspversion")) {
        serverStatus.htspVersion = msg.getInteger("htspversion", 13)
    }
    if (msg.containsKey("servername")) {
        serverStatus.serverName = msg.getString("servername")
    }
    if (msg.containsKey("serverversion")) {
        serverStatus.serverVersion = msg.getString("serverversion")
    }
    if (msg.containsKey("webroot")) {
        val webroot = msg.getString("webroot")
        serverStatus.webroot = webroot ?: ""
    }
    if (msg.containsKey("servercapability")) {
        for (capabilitiy in msg.getArrayList("servercapability")) {
            Timber.d("Server supports $capabilitiy")
        }
    }
    return serverStatus
}

fun convertIntentToAutorecMessage(intent: Intent, htspVersion: Int): HtspMessage {
    val enabled = intent.getIntExtra("enabled", 1).toLong()
    val title = intent.getStringExtra("title")
    val fulltext = intent.getStringExtra("fulltext")
    val directory = intent.getStringExtra("directory")
    val name = intent.getStringExtra("name")
    val configName = intent.getStringExtra("configName")
    val channelId = intent.getIntExtra("channelId", 0).toLong()
    val minDuration = intent.getIntExtra("minDuration", 0).toLong()
    val maxDuration = intent.getIntExtra("maxDuration", 0).toLong()
    val daysOfWeek = intent.getIntExtra("daysOfWeek", 127).toLong()
    val priority = intent.getIntExtra("priority", 2).toLong()
    val start = intent.getLongExtra("start", -1)
    val startWindow = intent.getLongExtra("startWindow", -1)
    val startExtra = intent.getLongExtra("startExtra", 0)
    val stopExtra = intent.getLongExtra("stopExtra", 0)
    val dupDetect = intent.getIntExtra("dupDetect", 0).toLong()
    val comment = intent.getStringExtra("comment")

    val request = HtspMessage()
    if (htspVersion >= 19) {
        request["enabled"] = enabled
    }
    request["title"] = title
    if (fulltext != null && htspVersion >= 20) {
        request["fulltext"] = fulltext
    }
    if (directory != null) {
        request["directory"] = directory
    }
    if (name != null) {
        request["name"] = name
    }
    if (configName != null) {
        request["configName"] = configName
    }
    // Don't add the channel id if none was given.
    // Assume the user wants to record on all channels
    if (channelId > 0) {
        request["channelId"] = channelId
    }
    // Minimal duration in seconds (0 = Any)
    request["minDuration"] = minDuration
    // Maximal duration in seconds (0 = Any)
    request["maxDuration"] = maxDuration
    request["daysOfWeek"] = daysOfWeek
    request["priority"] = priority

    // Minutes from midnight (up to 24*60) (window +- 15 minutes) (Obsoleted from version 18)
    // Do not send the value if the default of -1 (no time specified) was set
    if (start >= 0 && htspVersion < 18) {
        request["approxTime"] = start
    }
    // Minutes from midnight (up to 24*60) for the start of the time window.
    // Do not send the value if the default of -1 (no time specified) was set
    if (start >= 0 && htspVersion >= 18) {
        request["start"] = start
    }
    // Minutes from midnight (up to 24*60) for the end of the time window (including, cross-noon allowed).
    // Do not send the value if the default of -1 (no time specified) was set
    if (startWindow >= 0 && htspVersion >= 18) {
        request["startWindow"] = startWindow
    }
    request["startExtra"] = startExtra
    request["stopExtra"] = stopExtra

    if (htspVersion >= 20) {
        request["dupDetect"] = dupDetect
    }
    if (comment != null) {
        request["comment"] = comment
    }
    return request
}

fun convertIntentToDvrMessage(intent: Intent, htspVersion: Int): HtspMessage {
    val eventId = intent.getIntExtra("eventId", 0).toLong()
    val channelId = intent.getIntExtra("channelId", 0).toLong()
    val start = intent.getLongExtra("start", 0)
    val stop = intent.getLongExtra("stop", 0)
    val retention = intent.getLongExtra("retention", 0)
    val priority = intent.getIntExtra("priority", 2).toLong()
    val startExtra = intent.getLongExtra("startExtra", 0)
    val stopExtra = intent.getLongExtra("stopExtra", 0)
    val title = intent.getStringExtra("title")
    val subtitle = intent.getStringExtra("subtitle")
    val description = intent.getStringExtra("description")
    val configName = intent.getStringExtra("configName")
    val enabled = intent.getIntExtra("enabled", 1).toLong()
    // Controls that certain fields will only be added when the recording
    // is only scheduled and not being recorded
    val isRecording = intent.getBooleanExtra("isRecording", false)

    val request = HtspMessage()
    // If the eventId is set then an existing program from the program guide
    // shall be recorded. The server will then ignore the other fields
    // automatically.
    if (eventId > 0) {
        request["eventId"] = eventId
    }
    if (channelId > 0 && htspVersion >= 22) {
        request["channelId"] = channelId
    }
    if (!isRecording && start > 0) {
        request["start"] = start
    }
    if (stop > 0) {
        request["stop"] = stop
    }
    if (!isRecording && retention > 0) {
        request["retention"] = retention
    }
    if (!isRecording && priority > 0) {
        request["priority"] = priority
    }
    if (!isRecording && startExtra > 0) {
        request["startExtra"] = startExtra
    }
    if (stopExtra > 0) {
        request["stopExtra"] = stopExtra
    }
    // Only add the text fields if no event id was given
    if (eventId == 0L) {
        if (title != null) {
            request["title"] = title
        }
        if (subtitle != null && htspVersion >= 21) {
            request["subtitle"] = subtitle
        }
        if (description != null) {
            request["description"] = description
        }
    }
    if (configName != null) {
        request["configName"] = configName
    }
    if (htspVersion >= 23) {
        request["enabled"] = enabled
    }
    return request
}

fun convertIntentToTimerecMessage(intent: Intent, htspVersion: Int): HtspMessage {
    val enabled = intent.getIntExtra("enabled", 1).toLong()
    val title = intent.getStringExtra("title")
    val directory = intent.getStringExtra("directory")
    val name = intent.getStringExtra("name")
    val configName = intent.getStringExtra("configName")
    val channelId = intent.getIntExtra("channelId", 0).toLong()
    val daysOfWeek = intent.getIntExtra("daysOfWeek", 0).toLong()
    val priority = intent.getIntExtra("priority", 2).toLong()
    val start = intent.getLongExtra("start", -1)
    val stop = intent.getLongExtra("stop", -1)
    val retention = intent.getIntExtra("retention", -1).toLong()
    val comment = intent.getStringExtra("comment")

    val request = HtspMessage()
    if (htspVersion >= 19) {
        request["enabled"] = enabled
    }
    request["title"] = title
    if (directory != null) {
        request["directory"] = directory
    }
    if (name != null) {
        request["name"] = name
    }
    if (configName != null) {
        request["configName"] = configName
    }
    if (channelId > 0) {
        request["channelId"] = channelId
    }
    request["daysOfWeek"] = daysOfWeek
    request["priority"] = priority

    if (start >= 0) {
        request["start"] = start
    }
    if (stop >= 0) {
        request["stop"] = stop
    }
    if (retention > 0) {
        request["retention"] = retention
    }
    if (comment != null) {
        request["comment"] = comment
    }
    return request
}

fun convertIntentToEventMessage(intent: Intent): HtspMessage {
    val eventId = intent.getIntExtra("eventId", 0)
    val channelId = intent.getIntExtra("channelId", 0)
    val numFollowing = intent.getIntExtra("numFollowing", 0)
    val maxTime = intent.getLongExtra("maxTime", 0)

    val request = HtspMessage()
    request["method"] = "getEvents"
    if (eventId > 0) {
        request["eventId"] = eventId
    }
    if (channelId > 0) {
        request["channelId"] = channelId
    }
    if (numFollowing > 0) {
        request["numFollowing"] = numFollowing
    }
    if (maxTime > 0) {
        request["maxTime"] = maxTime
    }
    return request
}

fun convertIntentToEpgQueryMessage(intent: Intent): HtspMessage {
    val query = intent.getStringExtra("query")
    val channelId = intent.getIntExtra("channelId", 0).toLong()
    val tagId = intent.getIntExtra("tagId", 0).toLong()
    val contentType = intent.getIntExtra("contentType", 0)
    val minDuration = intent.getIntExtra("minduration", 0)
    val maxDuration = intent.getIntExtra("maxduration", 0)
    val language = intent.getStringExtra("language")
    val full = intent.getBooleanExtra("full", false)

    val request = HtspMessage()
    request["method"] = "epgQuery"
    request["query"] = query

    if (channelId > 0) {
        request["channelId"] = channelId
    }
    if (tagId > 0) {
        request["tagId"] = tagId
    }
    if (contentType > 0) {
        request["contentType"] = contentType
    }
    if (minDuration > 0) {
        request["minDuration"] = minDuration
    }
    if (maxDuration > 0) {
        request["maxDuration"] = maxDuration
    }
    if (language != null) {
        request["language"] = language
    }
    request["full"] = full
    return request
}

// Current timezone and date
val daylightSavingOffset: Int
    get() {
        val timeZone = TimeZone.getDefault()
        val nowDate = Date()
        val offsetFromUtc = timeZone.getOffset(nowDate.time)
        Timber.d("Offset from UTC is $offsetFromUtc")

        if (timeZone.useDaylightTime()) {
            Timber.d("Daylight saving is used")
            val dstOffset = timeZone.dstSavings
            if (timeZone.inDaylightTime(nowDate)) {
                Timber.d("Daylight saving offset is $dstOffset")
                return dstOffset
            }
        }
        Timber.d("Daylight saving is not used")
        return 0
    }

