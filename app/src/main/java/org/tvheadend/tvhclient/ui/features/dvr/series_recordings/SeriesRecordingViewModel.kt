package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.SeriesRecording
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import timber.log.Timber
import java.util.*

class SeriesRecordingViewModel(application: Application) : BaseViewModel(application) {

    var recording = SeriesRecording()
    val recordings: LiveData<List<SeriesRecording>> = appRepository.seriesRecordingData.getLiveDataItems()
    var recordingProfileNameId: Int = 0

    /**
     * Returns an intent with the recording data
     */
    fun getIntentData(recording: SeriesRecording): Intent {
        val intent = Intent(appContext, HtspService::class.java)
        intent.putExtra("title", recording.title)
        intent.putExtra("name", recording.name)
        intent.putExtra("directory", recording.directory)
        intent.putExtra("minDuration", recording.minDuration * 60)
        intent.putExtra("maxDuration", recording.maxDuration * 60)

        // Assume no start time is specified if 0:00 is selected
        if (isTimeEnabled) {
            Timber.d("Intent Recording start time is ${recording.start}")
            Timber.d("Intent Recording startWindow time is ${recording.startWindow}")
            intent.putExtra("start", recording.start)
            intent.putExtra("startWindow", recording.startWindow)
        } else {
            intent.putExtra("start", (-1).toLong())
            intent.putExtra("startWindow", (-1).toLong())
        }
        intent.putExtra("startExtra", recording.startExtra)
        intent.putExtra("stopExtra", recording.stopExtra)
        intent.putExtra("dupDetect", recording.dupDetect)
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
                startWindowTimeInMillis = Calendar.getInstance().timeInMillis
            }
        }

    fun getRecordingById(id: String): LiveData<SeriesRecording> {
        return appRepository.seriesRecordingData.getLiveDataItemById(id)
    }

    fun loadRecordingByIdSync(id: String) {
        recording = appRepository.seriesRecordingData.getItemById(id) ?: SeriesRecording()
        // In case one of the values is negative the time setting shall be disabled
        isTimeEnabled = recording.start >= 0 && recording.startWindow >= 0
    }

    var startTimeInMillis: Long = 0
        get() {
            Timber.d("Get time in millis is ${recording.startTimeInMillis}, start minutes are ${recording.start}")
            return recording.startTimeInMillis
        }
        set(milliSeconds) {
            field = milliSeconds
            recording.start = getMinutesFromTime(milliSeconds)
        }

    var startWindowTimeInMillis: Long = 0
        get() {
            Timber.d("Get time in millis is ${recording.startWindowTimeInMillis}, start minutes are ${recording.startWindow}")
            return recording.startWindowTimeInMillis
        }
        set(milliSeconds) {
            field = milliSeconds
            recording.startWindow = getMinutesFromTime(milliSeconds)
        }

    /**
     * The start and stop time handling is done in milliseconds within the app, but the
     * server requires and provides minutes instead. In case the start and stop times of
     * a recording need to be updated the milliseconds will be converted to minutes.
     */
    private fun getMinutesFromTime(milliSeconds: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val minutes = (hour * 60 + minute).toLong()
        Timber.d("Set time in millis is $milliSeconds, start minutes are $minutes")
        return minutes
    }

    fun getChannelList(): List<Channel> {
        val defaultChannelSortOrder = appContext.resources.getString(R.string.pref_default_channel_sort_order)
        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder)
                ?: defaultChannelSortOrder)
        return appRepository.channelData.getChannels(channelSortOrder)
    }

    fun getRecordingProfileNames(): Array<String> {
        return appRepository.serverProfileData.recordingProfileNames
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(appRepository.serverStatusData.activeItem.recordingServerProfileId)
    }
}
