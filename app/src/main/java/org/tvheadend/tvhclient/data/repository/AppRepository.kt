package org.tvheadend.tvhclient.data.repository

import org.tvheadend.tvhclient.domain.repository.data_source.*
import org.tvheadend.tvhclient.domain.repository.RepositoryInterface
import javax.inject.Inject

class AppRepository @Inject
constructor(override val channelData: ChannelData,
            override val programData: ProgramData,
            override val recordingData: RecordingData,
            override val seriesRecordingData: SeriesRecordingData,
            override val timerRecordingData: TimerRecordingData,
            override val connectionData: ConnectionData,
            override val channelTagData: ChannelTagData,
            override val serverStatusData: ServerStatusData,
            override val serverProfileData: ServerProfileData,
            override val tagAndChannelData: TagAndChannelData,
            override val miscData: MiscData) : RepositoryInterface
