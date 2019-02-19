package org.tvheadend.tvhclient.domain.repository

import org.tvheadend.tvhclient.domain.data_sources.*

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
