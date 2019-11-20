package org.tvheadend.tvhclient.ui.features.dvr

import org.tvheadend.tvhclient.data.entity.Channel

interface RecordingConfigSelectedListener {

    fun onChannelSelected(channel: Channel)

    fun onProfileSelected(which: Int)

    fun onPrioritySelected(which: Int)

    fun onDaysSelected(selectedDays: Int)
}
