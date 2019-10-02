package org.tvheadend.tvhclient.ui.features.epg

import android.app.Application
import android.content.SharedPreferences
import androidx.core.util.Pair
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.EpgChannel
import org.tvheadend.tvhclient.domain.entity.EpgProgram
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.features.channels.BaseChannelViewModel
import timber.log.Timber
import java.util.*

class EpgViewModel(application: Application) : BaseChannelViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {

    val epgChannels: LiveData<List<EpgChannel>>
    var reloadEpgData: LiveData<Boolean> = MutableLiveData()
    var epgData = MutableLiveData<HashMap<Int, List<EpgProgram>>>()

    private val channelSortOrder = MutableLiveData<Int>()
    val showChannelNumber = MutableLiveData<Boolean>()
    var showGenreColor = MutableLiveData<Boolean>()
    var showProgramSubtitle = MutableLiveData<Boolean>()

    private var hoursOfEpgDataPerScreen = MutableLiveData<Int>()
    private var daysOfEpgData = MutableLiveData<Int>()

    var pixelsPerMinute: Float = 0f
    var verticalScrollOffset = 0
    var verticalScrollPosition = 0
    var selectedTimeOffset = 0
    var searchQuery = ""

    var hoursToShow: Int
    val daysToShow: Int
    val fragmentCount: Int

    private val startTimes = ArrayList<Long>()
    private val endTimes = ArrayList<Long>()

    init {
        Timber.d("Initializing")

        daysToShow = Integer.parseInt(sharedPreferences.getString("days_of_epg_data", appContext.resources.getString(R.string.pref_default_days_of_epg_data))!!)
        hoursToShow = Integer.parseInt(sharedPreferences.getString("hours_of_epg_data_per_screen", appContext.resources.getString(R.string.pref_default_hours_of_epg_data_per_screen))!!)

        // The defined value should not be zero due to checking the value
        // in the settings. Check it anyway to prevent a divide by zero.
        hoursToShow = if (hoursToShow == 0) 1 else hoursToShow

        // Calculates the number of fragments in the view pager. This depends on how many days
        // shall be shown of the program guide and how many hours shall be visible per fragment.
        fragmentCount = daysToShow * (24 / hoursToShow)
        calculateViewPagerFragmentStartAndEndTimes()

        epgChannels = Transformations.switchMap(EpgChannelLiveData(channelSortOrder, selectedChannelTagIds)) { value ->
            val sortOrder = value.first
            val tagIds = value.second

            if (sortOrder == null) {
                Timber.d("Not loading epg channels because no channel sort order is set")
                return@switchMap null
            }
            if (tagIds == null) {
                Timber.d("Not loading epg channels because no channel tag id is set")
                return@switchMap null
            }
            Timber.d("Loading epg channels because either the channel sort order or channel tag ids have changed")
            return@switchMap appRepository.channelData.getAllEpgChannels(sortOrder, tagIds)
        }

        reloadEpgData = Transformations.switchMap(EpgProgramDataLiveData(epgChannels, hoursOfEpgDataPerScreen, daysOfEpgData)) { value ->
            val reload = MutableLiveData<Boolean>()
            reload.value = value.first != null && value.second != null && value.third != null
            return@switchMap reload
        }

        onSharedPreferenceChanged(sharedPreferences, "channel_sort_order")
        onSharedPreferenceChanged(sharedPreferences, "channel_number_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_subtitle_enabled")
        onSharedPreferenceChanged(sharedPreferences, "genre_colors_for_program_guide_enabled")
        onSharedPreferenceChanged(sharedPreferences, "hours_of_epg_data_per_screen")
        onSharedPreferenceChanged(sharedPreferences, "days_of_epg_data")

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun loadEpgData() {
        Timber.d("Loading epg data via coroutine")
        viewModelScope.launch {
            val defaultChannelSortOrder = appContext.resources.getString(R.string.pref_default_channel_sort_order)
            val order = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
            var hours = hoursOfEpgDataPerScreen.value!!
            hours = if (hours == 0) 1 else hours
            val days = daysOfEpgData.value!!

            Timber.d("Loading epg data from database with channel order $order, $hours hours per screen and for $days days")
            withContext(Dispatchers.IO) {
                epgData.postValue(loadProgramsBetweenTime(order, hours, days))
            }
        }
        Timber.d("Done loading epg data via coroutine")
    }

    private suspend fun loadProgramsBetweenTime(order: Int, hours: Int, days: Int) = withContext(Dispatchers.IO) {
        appRepository.programData.getEpgItemsBetweenTime(order, hours, days)
    }

    override fun onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.d("Shared preference $key has changed")
        if (sharedPreferences == null) return
        when (key) {
            "channel_sort_order" -> channelSortOrder.value = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder)
                    ?: defaultChannelSortOrder)
            "channel_number_enabled" -> showChannelNumber.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_channel_number_enabled))
            "program_subtitle_enabled" -> showProgramSubtitle.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_program_subtitle_enabled))
            "genre_colors_for_program_guide_enabled" -> showGenreColor.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_genre_colors_for_program_guide_enabled))
            "hours_of_epg_data_per_screen" -> hoursOfEpgDataPerScreen.value = Integer.parseInt(sharedPreferences.getString(key, appContext.resources.getString(R.string.pref_default_hours_of_epg_data_per_screen))!!)
            "days_of_epg_data" -> daysOfEpgData.value = Integer.parseInt(sharedPreferences.getString(key, appContext.resources.getString(R.string.pref_default_days_of_epg_data))!!)
        }
    }

    fun getRecordingsByChannel(channelId: Int): LiveData<List<Recording>> {
        return appRepository.recordingData.getLiveDataItemsByChannelId(channelId)
    }

    fun getProgramsByChannelAndBetweenTimeSync(channelId: Int, fragmentId: Int): List<EpgProgram> {
        return appRepository.programData.getItemByChannelIdAndBetweenTime(channelId, startTimes[fragmentId], endTimes[fragmentId])
    }

    internal inner class EpgProgramDataLiveData(channelList: LiveData<List<EpgChannel>>,
                                                hoursOfEpgDataPerScreen: LiveData<Int>,
                                                daysOfEpgData: LiveData<Int>) : MediatorLiveData<Triple<List<EpgChannel>?, Int?, Int?>>() {

        init {
            addSource(channelList) { channels ->
                value = Triple(channels, hoursOfEpgDataPerScreen.value, daysOfEpgData.value)
            }
            addSource(hoursOfEpgDataPerScreen) { hours ->
                value = Triple(channelList.value, hours, daysOfEpgData.value)
            }
            addSource(daysOfEpgData) { days ->
                value = Triple(channelList.value, hoursOfEpgDataPerScreen.value, days)
            }
        }
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

        // Get the current time in milliseconds without the seconds but in 30
        // minute slots. If the current time is later then 16:30 start from
        // 16:30 otherwise from 16:00.
        val minutes = if (Calendar.getInstance().get(Calendar.MINUTE) > 30) 30 else 0
        val calendarStartTime = Calendar.getInstance()
        calendarStartTime.set(Calendar.MINUTE, minutes)
        calendarStartTime.set(Calendar.SECOND, 0)
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

    fun calcPixelsPerMinute(displayWidth: Int) {
        pixelsPerMinute = (displayWidth - 221).toFloat() / (60.0f * hoursToShow.toFloat())
    }

    fun getStartTime(fragmentId: Int): Long {
        return startTimes[fragmentId]
    }

    fun getEndTime(fragmentId: Int): Long {
        return endTimes[fragmentId]
    }
}
