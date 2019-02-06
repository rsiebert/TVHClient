package org.tvheadend.tvhclient.data.repository

import org.tvheadend.tvhclient.data.source.*

interface RepositoryInterface {

    val timerRecordingData: TimerRecordingData

    val seriesRecordingData: SeriesRecordingData

    val recordingData: RecordingData

    val channelData: ChannelData

    val channelTagData: ChannelTagData

    val programData: ProgramData

    val connectionData: ConnectionData

    val serverStatusData: ServerStatusData

    val serverProfileData: ServerProfileData

    val tagAndChannelData: TagAndChannelData

    val miscData: MiscData
}
