package org.tvheadend.tvhclient.data.repository

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.data.source.*
import org.tvheadend.tvhclient.ui.common.Event
import org.tvheadend.tvhclient.ui.common.NetworkStatus
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
