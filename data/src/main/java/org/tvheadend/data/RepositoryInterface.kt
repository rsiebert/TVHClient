package org.tvheadend.data

import org.tvheadend.data.source.*

interface RepositoryInterface {

    val timerRecordingData: TimerRecordingDataSource

    val seriesRecordingData: SeriesRecordingDataSource

    val recordingData: RecordingDataSource

    val channelData: ChannelDataSource

    val channelTagData: ChannelTagDataSource

    val programData: ProgramDataSource

    val connectionData: ConnectionDataSource

    val serverStatusData: ServerStatusDataSource

    val serverProfileData: ServerProfileDataSource

    val tagAndChannelData: TagAndChannelDataSource

    val miscData: MiscDataSource

    val subscriptionData: SubscriptionDataSource

    val inputData: InputDataSource
}
