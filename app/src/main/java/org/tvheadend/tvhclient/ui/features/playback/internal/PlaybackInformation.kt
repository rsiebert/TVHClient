package org.tvheadend.tvhclient.ui.features.playback.internal

import org.tvheadend.tvhclient.data.entity.Channel
import org.tvheadend.tvhclient.data.entity.Recording

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
    private var startTime: Long = 0
    private var stopTime: Long = 0

    val remainingTime: String
        get() {
            val millis = (stopTime - System.currentTimeMillis())
            return getTimeString(millis)
        }

    val elapsedTime: String
        get() {
            val millis = (System.currentTimeMillis() - startTime)
            return getTimeString(millis)
        }

    private fun getTimeString(millis: Long): String {
        val hours = millis / 1000 / 60 / 60
        val minutes = millis / 1000 / 60 % 60
        val seconds = millis / 1000 % 60

        val minutesPrefix = if (minutes < 10) "0" else ""
        val secondsPrefix = if (seconds < 10) "0" else ""

        return if (hours > 0) {
            "$hours:$minutesPrefix$minutes:$secondsPrefix$seconds"
        } else {
            "$minutesPrefix$minutes:$secondsPrefix$seconds"
        }
    }
}