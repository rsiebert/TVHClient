package org.tvheadend.tvhclient.ui.features.epg

import android.app.Application
import android.text.format.Time
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.EpgChannel
import org.tvheadend.tvhclient.domain.entity.EpgProgram
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.features.channels.BaseChannelViewModel
import timber.log.Timber
import java.util.*

class EpgViewModel(application: Application) : BaseChannelViewModel(application) {

    val epgChannels: LiveData<List<EpgChannel>>

    var verticalScrollOffset = 0
    var verticalScrollPosition = 0
    var selectedTimeOffset = 0
    var searchQuery = ""

    val hoursToShow: Int
    val daysToShow: Int
    val fragmentCount: Int
    val startTimes = ArrayList<Long>()
    val endTimes = ArrayList<Long>()

    init {
        val trigger = EpgChannelLiveData(channelSortOrder, selectedChannelTagIds)
        epgChannels = Transformations.switchMap(trigger) { value ->
            Timber.d("Loading epg channels because one of the two triggers have changed")

            val first = value.first
            val second = value.second

            if (first == null) {
                Timber.d("Skipping loading of epg channels because channel sort order is not set")
                return@switchMap null
            }
            if (second == null) {
                Timber.d("Skipping loading of epg channels because selected channel tag ids are not set")
                return@switchMap null
            }

            return@switchMap appRepository.channelData.getAllEpgChannels(first, second)
        }

        daysToShow = Integer.parseInt(sharedPreferences.getString("days_of_epg_data", application.resources.getString(R.string.pref_default_days_of_epg_data))!!)
        val hours = Integer.parseInt(sharedPreferences.getString("hours_of_epg_data_per_screen", application.resources.getString(R.string.pref_default_hours_of_epg_data_per_screen))!!)
        // The defined value should not be zero due to checking the value
        // in the settings. Check it anyway to prevent a divide by zero.
        hoursToShow = if (hours == 0) 1 else hours

        // Calculates the number of fragments in the view pager. This depends on how many days
        // shall be shown of the program guide and how many hours shall be visible per fragment.
        fragmentCount = daysToShow * (24 / hoursToShow)
        calculateViewPagerFragmentStartAndEndTimes()
    }

    fun getRecordingsByChannel(channelId: Int): LiveData<List<Recording>> {
        return appRepository.recordingData.getLiveDataItemsByChannelId(channelId)
    }

    fun getProgramsByChannelAndBetweenTimeSync(channelId: Int, startTime: Long, endTime: Long): List<EpgProgram> {
        return appRepository.programData.getItemByChannelIdAndBetweenTime(channelId, startTime, endTime)
    }

    internal inner class EpgChannelLiveData(selectedChannelSortOrder: LiveData<Int>,
                                            selectedChannelTagIds: LiveData<List<Int>?>) : MediatorLiveData<Pair<Int, List<Int>?>>() {

        init {
            addSource(selectedChannelSortOrder) { order ->
                value = Pair.create(order, selectedChannelTagIds.value)
            }
            addSource(selectedChannelTagIds) { integers ->
                value = Pair.create(selectedChannelSortOrder.value, integers)
            }
        }
    }

    /**
     * Calculates the day, start and end hours that are valid for the fragment
     * at the given position. This will be saved in the lists to avoid
     * calculating this for every fragment again and so we can update the data
     * when the settings have changed.
     */
    private fun calculateViewPagerFragmentStartAndEndTimes() {
        // Clear the old arrays and initialize
        // the variables to start fresh
        startTimes.clear()
        endTimes.clear()

        // Get the current time in milliseconds
        val now = Time(Time.getCurrentTimezone())
        now.setToNow()

        // Get the current time in milliseconds without the seconds but in 30
        // minute slots. If the current time is later then 16:30 start from
        // 16:30 otherwise from 16:00.
        val calendarStartTime = Calendar.getInstance()
        val minutes = if (now.minute > 30) 30 else 0
        calendarStartTime.set(now.year, now.month, now.monthDay, now.hour, minutes, 0)
        var startTime = calendarStartTime.timeInMillis

        // Get the offset time in milliseconds without the minutes and seconds
        val offsetTime = (hoursToShow * 60 * 60 * 1000).toLong()

        // Set the start and end times for each fragment
        for (i in 0 until fragmentCount) {
            startTimes.add(startTime)
            endTimes.add(startTime + offsetTime - 1)
            startTime += offsetTime
        }
    }

}
