package org.tvheadend.tvhclient.domain.repository

import org.tvheadend.tvhclient.domain.repository.data_source.*

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

    val subscriptionData: SubscriptionData

    val inputData: InputData
}
