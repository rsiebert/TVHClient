package org.tvheadend.tvhclient.ui.features.epg

import android.app.Application
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.SparseArray
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import org.tvheadend.data.entity.EpgChannel
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.channels.BaseChannelViewModel
import org.tvheadend.tvhclient.ui.features.programs.ProgramDetailsFragment
import org.tvheadend.tvhclient.util.livedata.LiveEvent
import timber.log.Timber
import java.util.*

class EpgViewModel(application: Application) : BaseChannelViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {

    val registeredEpgFragments = SparseArray<Fragment>()
    val epgChannels: LiveData<List<EpgChannel>>
    private val viewAndEpgDataIsInvalidLiveEvent = LiveEvent<Boolean>()
    val viewAndEpgDataIsInvalid: MediatorLiveData<Boolean> = viewAndEpgDataIsInvalidLiveEvent
    var epgData = MutableLiveData<HashMap<Int, List<EpgProgram>>>()

    private val channelSortOrder = MutableLiveData<Int>()
    val showChannelNumber = MutableLiveData<Boolean>()
    var showGenreColor = MutableLiveData<Boolean>()
    var showProgramSubtitle = MutableLiveData<Boolean>()

    private var hoursOfEpgDataPerScreen = MutableLiveData<Int>()
    private var daysOfEpgData = MutableLiveData<Int>()

    private val defaultShowChannelNumber = application.applicationContext.resources.getBoolean(R.bool.pref_default_channel_number_enabled)
    private val defaultShowProgramSubtitle = application.applicationContext.resources.getBoolean(R.bool.pref_default_program_subtitle_enabled)
    private val defaultDaysOfEpgData = application.applicationContext.resources.getString(R.string.pref_default_days_of_epg_data)
    private val defaultShowGenreColor = application.applicationContext.resources.getBoolean(R.bool.pref_default_genre_colors_for_program_guide_enabled)
    private val defaultHoursOfEpgDataPerScreen = application.applicationContext.resources.getString(R.string.pref_default_hours_of_epg_data_per_screen)
    private val defaultShowAllChannelTags = application.applicationContext.resources.getBoolean(R.bool.pref_default_empty_channel_tags_enabled)

    /**
     * Whenever the display width is set, update the pixels per minute variable
     * to have an up to date value. The calculation must be done in the view model
     * because the required hours and days values are stored here.
     */
    var displayWidth: Int = 0
        set(width) {
            field = width
            updatePixelsPerMinute()
        }
    /**
     * Defines how wide in terms of pixels a minute is in the epg view.
     * This is required to calculate the correct width of the shown programs
     */
    var pixelsPerMinute: Float = 0f

    var verticalScrollOffset = 0
    var verticalScrollPosition = 0
    var selectedTimeOffset = 0

    /**
     * The number of hours of program data that shall be shown in the view pager screen.
     * The getter is overridden just in case to prevent returning the invalid value zero
     */
    var hoursToShow = 1
        get() {
            return if (field == 0) 1 else field
        }

    /**
     * Defines how many days of program data shall be shown in total
     * The getter is overridden just in case to prevent returning the invalid value zero
     */
    var daysToShow = 1
        get() {
            return if (field == 0) 1 else field
        }

    /**
     * The number of screens that the view pager contains
     */
    var viewPagerFragmentCount = MutableLiveData(0)

    private val startTimes = ArrayList<Long>()
    private val endTimes = ArrayList<Long>()

    init {
        Timber.d("Initializing")

        daysToShow = Integer.parseInt(sharedPreferences.getString("days_of_epg_data", defaultDaysOfEpgData)!!)
        hoursToShow = Integer.parseInt(sharedPreferences.getString("hours_of_epg_data_per_screen", defaultHoursOfEpgDataPerScreen)!!)

        daysOfEpgData.value = daysToShow
        hoursOfEpgDataPerScreen.value = hoursToShow

        epgChannels = EpgChannelLiveData(channelSortOrder, selectedChannelTagIds).switchMap { value ->
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

        // In case the live data hours to show has changed due to a shared preference change
        // the properties that depend on that value need to be updated
        viewAndEpgDataIsInvalid.addSource(hoursOfEpgDataPerScreen) { hours ->
            Timber.d("Hours to show have changed from $hoursToShow to $hours")
            if (hours != hoursToShow) {
                hoursToShow = hours
                updateViewProperties()
                viewAndEpgDataIsInvalidLiveEvent.value = true
            }
        }

        // In case the live data days to show has changed due to a shared preference change
        // the properties that depend on that value need to be updated.
        viewAndEpgDataIsInvalid.addSource(daysOfEpgData) { days ->
            Timber.d("Days to show have changed from $daysToShow to $days")
            if (days != daysToShow) {
                daysToShow = days
                updateViewProperties()
                viewAndEpgDataIsInvalidLiveEvent.value = true
            }
        }

        // In case the loaded channels have changed (order or amount) update the cache.
        viewAndEpgDataIsInvalid.addSource(epgChannels) { channels ->
            if (channels != null) {
                Timber.d("Channels count has changed to ${channels.size}")
                viewAndEpgDataIsInvalidLiveEvent.value = true
            }
        }

        // For the first time initialize the required
        // view properties and create an empty cache
        updateViewProperties()

        onSharedPreferenceChanged(sharedPreferences, "channel_sort_order")
        onSharedPreferenceChanged(sharedPreferences, "channel_number_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_subtitle_enabled")
        onSharedPreferenceChanged(sharedPreferences, "genre_colors_for_program_guide_enabled")
        onSharedPreferenceChanged(sharedPreferences, "hours_of_epg_data_per_screen")
        onSharedPreferenceChanged(sharedPreferences, "days_of_epg_data")
        onSharedPreferenceChanged(sharedPreferences, "empty_channel_tags_enabled")

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    private fun updateViewProperties() {
        Timber.d("Updating view pager related properties")
        updateViewPagerFragmentCount()
        updateStartAndEndTimes()
        updatePixelsPerMinute()
    }

    /**
     * Defines how many items the view pager shall contain.
     */
    private fun updateViewPagerFragmentCount() {
        val fragmentCount = daysToShow * (24 / hoursToShow)
        Timber.d("View pager fragment count has changed to $fragmentCount")
        viewPagerFragmentCount.value = fragmentCount
    }

    /**
     * Calculates the start and end times that will be show in each view pager screen.
     * This is done here once to avoid recalculating it every time when scrolling horizontally.
     */
    private fun updateStartAndEndTimes() {
        Timber.d("Updating start and end time arrays")
        startTimes.clear()
        endTimes.clear()

        // Get the current time in milliseconds without the seconds but in 30 minute slots.
        // If the current time is later then 16:30 start from 16:30 otherwise from 16:00.
        val minutes = if (Calendar.getInstance().get(Calendar.MINUTE) > 30) 30 else 0
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MINUTE, minutes)
        calendar.set(Calendar.SECOND, 0)
        var startTime = calendar.timeInMillis

        // Get the offset time in milliseconds without the minutes and seconds
        val offsetTime = (hoursToShow * 60 * 60 * 1000).toLong()

        // Set the start and end times for each page in the view pager
        val pageCount = viewPagerFragmentCount.value ?: 0
        for (i in 0 until pageCount) {
            startTimes.add(startTime)
            endTimes.add(startTime + offsetTime - 1)
            startTime += offsetTime
        }
    }

    /**
     * Defines how many pixels one minute represents on the current screen.
     * This needs to be updated when the hours to show shared preference has changed
     */
    private fun updatePixelsPerMinute() {
        val channelWidth = 221
        pixelsPerMinute = (displayWidth - channelWidth).toFloat() / (60.0f * hoursToShow.toFloat())
        Timber.d("Updated pixels per minute to $pixelsPerMinute")
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
            "channel_number_enabled" -> showChannelNumber.value = sharedPreferences.getBoolean(key, defaultShowChannelNumber)
            "program_subtitle_enabled" -> showProgramSubtitle.value = sharedPreferences.getBoolean(key, defaultShowProgramSubtitle)
            "genre_colors_for_program_guide_enabled" -> showGenreColor.value = sharedPreferences.getBoolean(key, defaultShowGenreColor)
            "hours_of_epg_data_per_screen" -> hoursOfEpgDataPerScreen.value = Integer.parseInt(sharedPreferences.getString(key, defaultHoursOfEpgDataPerScreen)!!)
            "days_of_epg_data" -> daysOfEpgData.value = Integer.parseInt(sharedPreferences.getString(key, defaultDaysOfEpgData)!!)
            "empty_channel_tags_enabled" -> showAllChannelTags.value = sharedPreferences.getBoolean(key, defaultShowAllChannelTags)
        }
    }

    fun getProgramsByChannelAndBetweenTimeSync(channelId: Int, fragmentId: Int): List<EpgProgram> {
        return appRepository.programData.getItemByChannelIdAndBetweenTime(channelId, startTimes[fragmentId], endTimes[fragmentId])
    }

    internal class EpgChannelLiveData(selectedChannelSortOrder: LiveData<Int>,
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

    fun getStartTime(fragmentId: Int): Long {
        return startTimes[fragmentId]
    }

    fun getEndTime(fragmentId: Int): Long {
        return endTimes[fragmentId]
    }

    /**
     * Returns the activity from the view context so that
     * stuff like the fragment manager can be accessed
     *
     * @param view The view to retrieve the activity from
     * @return Activity or null if none was found
     */
    private fun getActivity(view: View): AppCompatActivity? {
        var context = view.context
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    fun onClick(view: View, program: EpgProgram) {
        Timber.d("Clicked on program ${program.title}")
        val activity = getActivity(view) ?: return
        val fragment = ProgramDetailsFragment.newInstance(program.eventId, program.channelId)
        val ft = activity.supportFragmentManager.beginTransaction()
        ft.replace(R.id.main, fragment)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        ft.addToBackStack(null)
        ft.commit()
    }

    fun onLongClick(view: View, program: EpgProgram): Boolean {
        Timber.d("Long clicked on program ${program.title}")
        val activity = getActivity(view) ?: return false
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is EpgFragment
                && fragment.isAdded
                && fragment.isResumed) {
            fragment.showPopupMenu(view, program)
        }
        return true
    }
}
