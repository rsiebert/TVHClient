package org.tvheadend.tvhclient.ui.features.playback.internal

import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.Recording
import java.text.SimpleDateFormat
import java.util.*

class PlaybackInformation {

    constructor(channel: Channel?) {
        channelIcon = channel?.icon ?: ""
        channelName = channel?.name ?: ""
        title = channel?.programTitle ?: ""
        subtitle = channel?.programSubtitle ?: ""
        nextTitle = channel?.nextProgramTitle ?: ""
        startTime = channel?.programStart ?: 0
        stopTime = channel?.programStop ?: 0
    }

    constructor(recording: Recording?) {
        channelIcon = recording?.channelIcon ?: ""
        channelName = recording?.channelName ?: ""
        title = recording?.title ?: ""
        subtitle = recording?.subtitle ?: ""
        startTime = recording?.start ?: 0
        stopTime = recording?.stop ?: 0
    }

    var channelIcon: String = ""
    var channelName: String = ""
    var title: String = ""
    var subtitle: String = ""
    var nextTitle: String = ""
    var startTime: Long = 0
    var stopTime: Long = 0

    val remainingTime: String
        get() {
            val sdf = SimpleDateFormat("mm:ss", Locale.US)
            return sdf.format(stopTime - System.currentTimeMillis())
        }

    val elapsedTime: String
        get() {
            val sdf = SimpleDateFormat("mm:ss", Locale.US)
            return sdf.format(System.currentTimeMillis() - startTime)
        }
}