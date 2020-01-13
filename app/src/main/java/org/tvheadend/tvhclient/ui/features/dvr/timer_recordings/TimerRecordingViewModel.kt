package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import timber.log.Timber
import java.util.*

class TimerRecordingViewModel(application: Application) : BaseViewModel(application) {

    var selectedListPosition = 0
    val currentId = MutableLiveData("")
    var recording = TimerRecording()
    var recordingLiveData = MediatorLiveData<TimerRecording>()
    val recordings: LiveData<List<TimerRecording>> = appRepository.timerRecordingData.getLiveDataItems()
    var recordingProfileNameId = 0

    /**
     * Returns an intent with the recording data
     */
    fun getIntentData(recording: TimerRecording): Intent {
        val intent = Intent(appContext, HtspService::class.java)
        intent.putExtra("directory", recording.directory)
        intent.putExtra("title", recording.title)
        intent.putExtra("name", recording.name)

        if (isTimeEnabled) {
            intent.putExtra("start", recording.start)
            intent.putExtra("stop", recording.stop)
        } else {
            intent.putExtra("start", (0).toLong())
            intent.putExtra("stop", (0).toLong())
        }
        intent.putExtra("daysOfWeek", recording.daysOfWeek)
        intent.putExtra("priority", recording.priority)
        intent.putExtra("enabled", if (recording.isEnabled) 1 else 0)

        if (recording.channelId > 0) {
            intent.putExtra("channelId", recording.channelId)
        }
        return intent
    }

    var isTimeEnabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                startTimeInMillis = Calendar.getInstance().timeInMillis
                stopTimeInMillis = Calendar.getInstance().timeInMillis
            }
        }

    init {
        recordingLiveData.addSource(currentId) { value ->
            if (value.isNotEmpty()) {
                recordingLiveData.value = appRepository.timerRecordingData.getItemById(value)
            }
        }
    }

    fun loadRecordingByIdSync(id: String) {
        recording = appRepository.timerRecordingData.getItemById(id) ?: TimerRecording()
        isTimeEnabled = recording.start > 0 && recording.stop > 0
    }

    var startTimeInMillis: Long = 0
        get() {
            return recording.startTimeInMillis
        }
        set(milliSeconds) {
            field = milliSeconds
            recording.start = getMinutesFromTime(milliSeconds)
        }

    var stopTimeInMillis: Long = 0
        get() {
            return recording.stopTimeInMillis
        }
        set(milliSeconds) {
            field = milliSeconds
            recording.stop = getMinutesFromTime(milliSeconds)
        }

    /**
     * The start and stop time handling is done in milliseconds within the app, but the
     * server requires and provides minutes instead. In case the start and stop times of
     * a recording need to be updated the milliseconds will be converted to minutes.
     */
    private fun getMinutesFromTime(milliSeconds : Long) : Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val minutes = (hour * 60 + minute).toLong()
        Timber.d("Time in millis is $milliSeconds, start minutes are $minutes")
        return minutes
    }

    fun getChannelList(): List<Channel> {
        val defaultChannelSortOrder = appContext.resources.getString(R.string.pref_default_channel_sort_order)
        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
        return appRepository.channelData.getChannels(channelSortOrder)
    }

    fun getRecordingProfileNames(): Array<String> {
        return appRepository.serverProfileData.recordingProfileNames
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(appRepository.serverStatusData.activeItem.recordingServerProfileId)
    }
}
