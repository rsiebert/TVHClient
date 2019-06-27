package org.tvheadend.tvhclient.data.repository

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.domain.repository.RepositoryInterface
import org.tvheadend.tvhclient.domain.repository.data_source.*
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import javax.inject.Inject

class AppRepository @Inject
constructor(
        override val channelData: ChannelData,
        override val programData: ProgramData,
        override val recordingData: RecordingData,
        override val seriesRecordingData: SeriesRecordingData,
        override val timerRecordingData: TimerRecordingData,
        override val connectionData: ConnectionData,
        override val channelTagData: ChannelTagData,
        override val serverStatusData: ServerStatusData,
        override val serverProfileData: ServerProfileData,
        override val tagAndChannelData: TagAndChannelData,
        override val miscData: MiscData,
        override val subscriptionData: SubscriptionData,
        override val inputData: InputData
) : RepositoryInterface {

    var isUnlocked: MutableLiveData<Boolean> = MutableLiveData(false)
    var snackbarMessage: MutableLiveData<Intent> = MutableLiveData()
    var networkStatus: MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.NETWORK_UNKNOWN)
}
