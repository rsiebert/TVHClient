package org.tvheadend.data

import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.source.ChannelDataSource
import org.tvheadend.data.source.ChannelTagDataSource
import org.tvheadend.data.source.ConnectionDataSource
import org.tvheadend.data.source.InputDataSource
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.data.source.ProgramDataSource
import org.tvheadend.data.source.RecordingDataSource
import org.tvheadend.data.source.SeriesRecordingDataSource
import org.tvheadend.data.source.ServerProfileDataSource
import org.tvheadend.data.source.ServerStatusDataSource
import org.tvheadend.data.source.SubscriptionDataSource
import org.tvheadend.data.source.TagAndChannelDataSource
import org.tvheadend.data.source.TimerRecordingDataSource
import javax.inject.Inject

class AppRepository @Inject
constructor(
        override val channelData: ChannelDataSource,
        override val programData: ProgramDataSource,
        override val recordingData: RecordingDataSource,
        override val seriesRecordingData: SeriesRecordingDataSource,
        override val timerRecordingData: TimerRecordingDataSource,
        override val connectionData: ConnectionDataSource,
        override val channelTagData: ChannelTagDataSource,
        override val serverStatusData: ServerStatusDataSource,
        override val serverProfileData: ServerProfileDataSource,
        override val tagAndChannelData: TagAndChannelDataSource,
        override val miscData: MiscDataSource,
        override val subscriptionData: SubscriptionDataSource,
        override val inputData: InputDataSource
) : RepositoryInterface {

    private var isUnlockedLiveData = MutableLiveData<Boolean>()

    init {
        isUnlockedLiveData.value = false
    }
}