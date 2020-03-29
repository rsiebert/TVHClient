package org.tvheadend.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.source.*
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

    fun getIsUnlockedLiveData(): LiveData<Boolean> = isUnlockedLiveData

    fun getIsUnlocked(): Boolean = isUnlockedLiveData.value ?: false

    fun setIsUnlocked(unlocked: Boolean) {
        isUnlockedLiveData.value = unlocked
    }
}