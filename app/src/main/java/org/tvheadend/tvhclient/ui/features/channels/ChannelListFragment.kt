package org.tvheadend.tvhclient.ui.features.channels

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ChannelTag
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.notification.addNotificationProgramIsAboutToStart
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment
import org.tvheadend.tvhclient.ui.features.programs.ProgramViewModel
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber

// TODO Move the variable programIdToBeEditedWhenBeingRecorded into the viewmodel
// TODO check if the selection of a channel shows the correct program list

class ChannelListFragment : BaseFragment(), RecyclerViewClickCallback, ChannelTimeSelectedInterface, ChannelTagIdsSelectedInterface, SearchRequestInterface, Filter.FilterListener {

    private lateinit var programViewModel: ProgramViewModel
    private lateinit var recyclerViewAdapter: ChannelRecyclerViewAdapter
    private lateinit var channelViewModel: ChannelViewModel

    private var channelCount = 0

    // Used in the time selection dialog to show a time entry every x hours.
    private val intervalInHours = 2
    private var programIdToBeEditedWhenBeingRecorded = 0
    private var channelTags: List<ChannelTag> = ArrayList()
    private var selectedTime: Long = 0
    private lateinit var currentTimeUpdateTask: Runnable
    private val currentTimeUpdateHandler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        channelViewModel = ViewModelProviders.of(activity!!).get(ChannelViewModel::class.java)
        programViewModel = ViewModelProviders.of(activity!!).get(ProgramViewModel::class.java)

        arguments?.let {
            channelViewModel.selectedListPosition = it.getInt("listPosition")
            channelViewModel.selectedTimeOffset = it.getInt("timeOffset")
        }

        recyclerViewAdapter = ChannelRecyclerViewAdapter(channelViewModel, isDualPane, this)
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.adapter = recyclerViewAdapter
        recycler_view.gone()

        Timber.d("Observing selected time")
        channelViewModel.selectedTime.observe(viewLifecycleOwner, Observer { time ->
            Timber.d("View model returned selected time $time")
            if (time != null) {
                selectedTime = time
            }
        })

        Timber.d("Observing channel tags")
        channelViewModel.channelTags.observe(viewLifecycleOwner, Observer { tags ->
            if (tags != null) {
                Timber.d("View model returned ${tags.size} channel tags")
                channelTags = tags
            }
        })

        Timber.d("Observing channels")
        channelViewModel.channels.observe(viewLifecycleOwner, Observer { channels ->
            if (channels != null) {
                Timber.d("View model returned ${channels.size} channels")
                recyclerViewAdapter.addItems(channels.toMutableList())
                observeSearchQuery()
                observeRecordings()
            }

            recycler_view?.visible()
            showStatusInToolbar()
            activity?.invalidateOptionsMenu()

            if (isDualPane && recyclerViewAdapter.itemCount > 0) {
                showProgramListOfSelectedChannelInDualPane(channelViewModel.selectedListPosition)
            }
        })

        channelViewModel.channelCount.observe(viewLifecycleOwner, Observer { count ->
            channelCount = count
        })

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        currentTimeUpdateTask = Runnable {
            val currentTime = System.currentTimeMillis()
            Timber.d("Checking if selected time $selectedTime is past current time $currentTime")
            if (!baseViewModel.isSearchActive && selectedTime < currentTime) {
                Timber.d("Updated selected time to current time")
                channelViewModel.setSelectedTime(currentTime)
            }
            currentTimeUpdateHandler.postDelayed(currentTimeUpdateTask, 60000)
        }
    }

    private fun observeRecordings() {
        // Get all recordings for the given channel to check if it belongs to a certain program
        // so the recording status of the particular program can be updated. This is required
        // because the programs are not updated automatically when recordings change.
        Timber.d("Observing recordings")
        channelViewModel.recordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                Timber.d("View model returned ${recordings.size} recordings")
                recyclerViewAdapter.addRecordings(recordings)
                for (recording in recordings) {
                    // Show the edit recording screen of the scheduled recording
                    // in case the user has selected the record and edit menu item.
                    if (recording.eventId == programIdToBeEditedWhenBeingRecorded && programIdToBeEditedWhenBeingRecorded > 0) {
                        programIdToBeEditedWhenBeingRecorded = 0
                        val intent = Intent(activity, RecordingAddEditActivity::class.java)
                        intent.putExtra("id", recording.id)
                        intent.putExtra("type", "recording")
                        activity?.startActivity(intent)
                        break
                    }
                }
            }
        })
    }

    private fun observeSearchQuery() {
        Timber.d("Observing search query")
        baseViewModel.searchQuery.observe(viewLifecycleOwner, Observer { query ->
            if (query.isNotEmpty()) {
                Timber.d("View model returned search query '$query'")
                onSearchRequested(query)
            } else {
                Timber.d("View model returned empty search query")
                onSearchResultsCleared()
            }
        })
    }

    private fun showStatusInToolbar() {
        // Show either all channels or the name of the selected
        // channel tag and the channel count in the toolbar
        context?.let {
            val toolbarTitle = channelViewModel.getSelectedChannelTagName(it)
            if (!baseViewModel.isSearchActive) {
                toolbarInterface.setTitle(toolbarTitle)
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setTitle(getString(R.string.search_results))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.channels, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        currentTimeUpdateHandler.post(currentTimeUpdateTask)
    }

    override fun onPause() {
        super.onPause()
        currentTimeUpdateHandler.removeCallbacks(currentTimeUpdateTask)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.channel_list_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", resources.getBoolean(R.bool.pref_default_genre_colors_for_channels_enabled))
        val showChannelTagMenu = sharedPreferences.getBoolean("channel_tag_menu_enabled", resources.getBoolean(R.bool.pref_default_channel_tag_menu_enabled))

        if (!baseViewModel.isSearchActive) {
            menu.findItem(R.id.menu_genre_color_information)?.isVisible = showGenreColors
            menu.findItem(R.id.menu_program_timeframe)?.isVisible = isUnlocked
            menu.findItem(R.id.menu_search)?.isVisible = recyclerViewAdapter.itemCount > 0
            menu.findItem(R.id.menu_search_channels)?.isVisible = recyclerViewAdapter.itemCount > 0

            // Prevent the channel tag menu item from going into the overlay menu
            if (showChannelTagMenu) {
                menu.findItem(R.id.menu_channel_tags)?.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        } else {
            menu.findItem(R.id.menu_genre_color_information)?.isVisible = false
            menu.findItem(R.id.menu_program_timeframe)?.isVisible = false
            menu.findItem(R.id.menu_search)?.isVisible = false
            menu.findItem(R.id.menu_search_channels)?.isVisible = false
            menu.findItem(R.id.menu_channel_tags)?.isVisible = false
        }

        menu.findItem(R.id.menu_send_wake_on_lan_packet)?.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_channel_tags -> showChannelTagSelectionDialog(ctx, channelTags.toMutableList(), channelCount, this)
            R.id.menu_program_timeframe -> showProgramTimeframeSelectionDialog(ctx, channelViewModel.selectedTimeOffset, intervalInHours, 12, this)
            R.id.menu_genre_color_information -> showGenreColorDialog(ctx)
            R.id.menu_channel_sort_order -> showChannelSortOrderSelectionDialog(ctx)
            R.id.menu_send_wake_on_lan_packet -> sendWakeOnLanPacket(ctx, connection)
            R.id.menu_search_channels -> showSearchForChannelsDialog(ctx)
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSearchForChannelsDialog(context: Context): Boolean {
        MaterialDialog(context).show {
            title(res = R.string.search_for_channels)
            input(hintRes = R.string.enter_channel_name) { _, text ->
                baseViewModel.startSearchQuery(text.toString())
            }
            positiveButton(R.string.search)
        }
        return true
    }

    override fun onTimeSelected(which: Int) {
        channelViewModel.selectedTimeOffset = which
        recycler_view?.gone()

        // Add the selected list index as extra hours to the current time.
        // If the first index was selected then use the current time.
        var timeInMillis = System.currentTimeMillis()
        timeInMillis += (1000 * 60 * 60 * which * intervalInHours).toLong()
        channelViewModel.setSelectedTime(timeInMillis)
    }

    override fun onChannelTagIdsSelected(ids: Set<Int>) {
        recycler_view?.gone()
        channelViewModel.setSelectedChannelTagIds(ids)
    }

    private fun showChannelDetails(position: Int) {
        if (!isDualPane) {
            showProgramListOfSelectedChannelInSinglePane(position)
        } else {
            showProgramListOfSelectedChannelInDualPane(position)
        }
    }

    private fun showProgramListOfSelectedChannelInSinglePane(position: Int) {
        channelViewModel.selectedListPosition = position
        recyclerViewAdapter.setPosition(position)
        val channel = recyclerViewAdapter.getItem(position)
        if (channel == null || !isVisible) {
            return
        }

        // Show the fragment to display the program list of the selected channel.
        val fragment = ProgramListFragment.newInstance(channel.name ?: "", channel.id, selectedTime)
        activity?.supportFragmentManager?.beginTransaction()?.also {
            it.replace(R.id.main, fragment)
            it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            it.addToBackStack(null)
            it.commit()
        }
    }

    private fun showProgramListOfSelectedChannelInDualPane(position: Int) {
        channelViewModel.selectedListPosition = position
        recyclerViewAdapter.setPosition(position)
        val channel = recyclerViewAdapter.getItem(position)
        if (channel == null || !isVisible) {
            return
        }

        Timber.d("Showing program list for selected channel")
        // Check if an instance of the program list fragment for the selected channel is
        // already available. If an instance exist already then update the selected time
        // that was selected from the channel list.
        val fm = activity?.supportFragmentManager
        var fragment = fm?.findFragmentById(R.id.details)
        if (fragment !is ProgramListFragment
                || programViewModel.channelId != channel.id) {
            Timber.d("Channel has changed, showing new program list")
            fragment = ProgramListFragment.newInstance(channel.name ?: "", channel.id, selectedTime)
            fm?.beginTransaction()?.also {
                it.replace(R.id.details, fragment)
                it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                it.commit()
            }
        } else {
            Timber.d("Channel is the same, updating only time in the program list")
            programViewModel.selectedTime = selectedTime
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val channel = recyclerViewAdapter.getItem(position) ?: return
        val ctx = context ?: return

        val program = channelViewModel.getProgramById(channel.programId)
        val recording = channelViewModel.getRecordingById(channel.programId)

        val popupMenu = PopupMenu(ctx, view)
        popupMenu.menuInflater.inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)

        preparePopupOrToolbarRecordingMenu(ctx, popupMenu.menu, recording, isConnectionToServerAvailable, htspVersion, isUnlocked)
        preparePopupOrToolbarSearchMenu(popupMenu.menu, channel.programTitle, isConnectionToServerAvailable)
        preparePopupOrToolbarMiscMenu(ctx, popupMenu.menu, program, isConnectionToServerAvailable, isUnlocked)

        // If no program data is available for the channel, hide all menu items except
        // playing the channel. This is the only option possible
        if (program == null && isConnectionToServerAvailable) {
            popupMenu.menu.children.forEach { it.isVisible = false }
            popupMenu.menu.findItem(R.id.menu_play)?.isVisible = true
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_stop_recording -> return@setOnMenuItemClickListener showConfirmationToStopSelectedRecording(ctx, recording, null)
                R.id.menu_cancel_recording -> return@setOnMenuItemClickListener showConfirmationToCancelSelectedRecording(ctx, recording, null)
                R.id.menu_remove_recording -> return@setOnMenuItemClickListener showConfirmationToRemoveSelectedRecording(ctx, recording, null)
                R.id.menu_record_program -> return@setOnMenuItemClickListener recordSelectedProgram(ctx, channel.programId, channelViewModel.getRecordingProfile(), htspVersion)
                R.id.menu_record_program_and_edit -> {
                    programIdToBeEditedWhenBeingRecorded = channel.programId
                    return@setOnMenuItemClickListener recordSelectedProgram(ctx, channel.programId, channelViewModel.getRecordingProfile(), htspVersion)
                }
                R.id.menu_record_program_with_custom_profile -> return@setOnMenuItemClickListener recordSelectedProgramWithCustomProfile(ctx, channel.programId, channel.id, channelViewModel.getRecordingProfileNames(), channelViewModel.getRecordingProfile())
                R.id.menu_record_program_as_series_recording -> return@setOnMenuItemClickListener recordSelectedProgramAsSeriesRecording(ctx, channel.programTitle, channelViewModel.getRecordingProfile(), htspVersion)
                R.id.menu_play -> return@setOnMenuItemClickListener playSelectedChannel(ctx, channel.id, isUnlocked)
                R.id.menu_cast -> return@setOnMenuItemClickListener castSelectedChannel(ctx, channel.id)

                R.id.menu_search_imdb -> return@setOnMenuItemClickListener searchTitleOnImdbWebsite(ctx, channel.programTitle)
                R.id.menu_search_fileaffinity -> return@setOnMenuItemClickListener searchTitleOnFileAffinityWebsite(ctx, channel.programTitle)
                R.id.menu_search_youtube -> return@setOnMenuItemClickListener searchTitleOnYoutube(ctx, channel.programTitle)
                R.id.menu_search_google -> return@setOnMenuItemClickListener searchTitleOnGoogle(ctx, channel.programTitle)
                R.id.menu_search_epg -> return@setOnMenuItemClickListener searchTitleInTheLocalDatabase(activity!!, baseViewModel, channel.programTitle, channel.id)

                R.id.menu_add_notification -> return@setOnMenuItemClickListener addNotificationProgramIsAboutToStart(ctx, program, channelViewModel.getRecordingProfile())
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    override fun onSearchRequested(query: String) {
        recyclerViewAdapter.filter.filter(query, this)
    }

    override fun onSearchResultsCleared() {
        recyclerViewAdapter.filter.filter("", this)
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_programs)
    }

    override fun onClick(view: View, position: Int) {
        if ((view.id == R.id.icon || view.id == R.id.icon_text)
                && Integer.valueOf(sharedPreferences.getString("channel_icon_action", resources.getString(R.string.pref_default_channel_icon_action))!!) > 0
                && isConnectionToServerAvailable) {
            recyclerViewAdapter.getItem(position)?.let {
                playOrCastChannel(view.context, it.id, isUnlocked)
            }
        } else {
            showChannelDetails(position)
        }
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        showPopupMenu(view, position)
        return true
    }

    override fun onFilterComplete(count: Int) {
        showStatusInToolbar()
        // Show the first search result item in the details screen
        if (isDualPane && recyclerViewAdapter.itemCount > 0) {
            showChannelDetails(0)
        }
    }
}
