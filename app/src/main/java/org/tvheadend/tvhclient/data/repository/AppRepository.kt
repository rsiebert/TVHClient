package org.tvheadend.tvhclient.data.repository

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.domain.repository.RepositoryInterface
import org.tvheadend.tvhclient.domain.repository.data_source.*
import org.tvheadend.tvhclient.ui.common.Event
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

    private var isUnlocked = MutableLiveData<Boolean>()
    private var snackbarMessage = MutableLiveData<Event<Intent>>()
    private var networkStatus = MutableLiveData<NetworkStatus>()
    private var connectionToServerAvailable = MutableLiveData<Boolean>()

    init {
        isUnlocked.value = false
        networkStatus.value = NetworkStatus.NETWORK_UNKNOWN
        connectionToServerAvailable.value = false
    }

    fun getSnackbarMessage(): LiveData<Event<Intent>> = snackbarMessage

    fun setSnackbarMessage(msg: Intent) {
        snackbarMessage.value = Event(msg)
    }

    fun getNetworkStatus(): LiveData<NetworkStatus> = networkStatus

    fun setNetworkStatus(status: NetworkStatus) {
        networkStatus.value = status
    }

    fun getIsUnlocked(): LiveData<Boolean> = isUnlocked

    fun setIsUnlocked(unlocked: Boolean) {
        isUnlocked.value = unlocked
    }

    fun getConnectionToServerAvailable(): LiveData<Boolean> = connectionToServerAvailable

    fun setConnectionToServerAvailable(available: Boolean) {
        connectionToServerAvailable.value = available
    }
}
