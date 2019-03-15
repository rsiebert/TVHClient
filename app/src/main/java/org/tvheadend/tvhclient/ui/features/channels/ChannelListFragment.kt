package org.tvheadend.tvhclient.ui.features.channels

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.*
import android.widget.Filter
import android.widget.ProgressBar
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ChannelTag
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.base.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.ui.features.dialogs.ChannelTagSelectionDialog
import org.tvheadend.tvhclient.ui.features.dialogs.GenreColorDialog
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import org.tvheadend.tvhclient.util.menu.PopupMenuUtil
import org.tvheadend.tvhclient.util.menu.SearchMenuUtils
import org.tvheadend.tvhclient.util.tasks.WakeOnLanTask
import timber.log.Timber

class ChannelListFragment : BaseFragment(), RecyclerViewClickCallback, ChannelDisplayOptionListener, SearchRequestInterface, Filter.FilterListener {

    @BindView(R.id.recycler_view)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.progress_bar)
    lateinit var progressBar: ProgressBar

    lateinit var unbinder: Unbinder
    lateinit var recyclerViewAdapter: ChannelRecyclerViewAdapter
    lateinit var viewModel: ChannelViewModel

    private var selectedTimeOffset: Int = 0
    private var selectedListPosition: Int = 0
    private var searchQuery: String? = null

    // Used in the time selection dialog to show a time entry every x hours.
    private val intervalInHours = 2
    private var programIdToBeEditedWhenBeingRecorded = 0
    private var channelTags: List<ChannelTag>? = null
    private var selectedTime: Long = 0

    private var currentTimeUpdateTask: Runnable? = null
    private val currentTimeUpdateHandler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.recyclerview_fragment, container, false)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.adapter = null
        unbinder.unbind()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("listPosition", 0)
            selectedTimeOffset = savedInstanceState.getInt("timeOffset")
            searchQuery = savedInstanceState.getString(SearchManager.QUERY)
        } else {
            selectedListPosition = 0
            selectedTimeOffset = 0
            val bundle = arguments
            if (bundle != null) {
                searchQuery = bundle.getString(SearchManager.QUERY)
            }
        }

        recyclerViewAdapter = ChannelRecyclerViewAdapter(isDualPane, this)
        recyclerView.layoutManager = LinearLayoutManager(activity.applicationContext)
        recyclerView.addItemDecoration(DividerItemDecoration(activity.applicationContext, LinearLayoutManager.VERTICAL))
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = recyclerViewAdapter

        recyclerView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        viewModel = ViewModelProviders.of(activity).get(ChannelViewModel::class.java)

        Timber.d("Observing selected time")
        viewModel.selectedTime.observe(viewLifecycleOwner, Observer { time ->
            Timber.d("View model returned selected time $time")
            if (time != null) {
                selectedTime = time
            }
        })

        Timber.d("Observing channel tags")
        viewModel.channelTags.observe(viewLifecycleOwner, Observer{ tags ->
            if (tags != null) {
                Timber.d("View model returned ${tags.size} channel tags")
                channelTags = tags
            }
        })

        Timber.d("Observing channels")
        viewModel.channels.observe(viewLifecycleOwner, Observer{ channels ->
            if (channels != null) {
                Timber.d("View model returned ${channels.size} channels")
                recyclerViewAdapter.addItems(channels)
            }

            recyclerView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE

            showChannelTagOrChannelCount()

            if (isDualPane && recyclerViewAdapter.itemCount > 0) {
                showChannelDetails(selectedListPosition)
            }
        })

        // Get all recordings for the given channel to check if it belongs to a certain program
        // so the recording status of the particular program can be updated. This is required
        // because the programs are not updated automatically when recordings change.
        Timber.d("Observing recordings")
        viewModel.allRecordings.observe(viewLifecycleOwner, Observer{ recordings ->
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
                        activity.startActivity(intent)
                        break
                    }
                }
            }
        })

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        currentTimeUpdateTask = Runnable {
            val currentTime = System.currentTimeMillis()
            Timber.d("Checking if selected time $selectedTime is past current time $currentTime")
            if (selectedTime < currentTime) {
                Timber.d("Updated selected time to current time")
                viewModel.setSelectedTime(currentTime)
            }
            currentTimeUpdateHandler.postDelayed(currentTimeUpdateTask, 60000)
        }
        currentTimeUpdateHandler.post(currentTimeUpdateTask)
    }

    private fun showChannelTagOrChannelCount() {
        // Show either all channels or the name of the selected
        // channel tag and the channel count in the toolbar
        val toolbarTitle = viewModel.getSelectedChannelTagName(activity)
        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setTitle(toolbarTitle)
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items,
                    recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        } else {
            toolbarInterface.setTitle(getString(R.string.search_results))
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.channels,
                    recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("listPosition", selectedListPosition)
        outState.putInt("timeOffset", selectedTimeOffset)
        outState.putString(SearchManager.QUERY, searchQuery)
    }

    override fun onResume() {
        super.onResume()
        // When the user returns from the settings only the onResume method is called, not the
        // onActivityCreated, so we need to check if any values that affect the representation
        // of the channel list have changed.

        viewModel.setChannelSortOrder(Integer.valueOf(sharedPreferences.getString("channel_sort_order", resources.getString(R.string.pref_default_channel_sort_order))!!))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.channel_list_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", resources.getBoolean(R.bool.pref_default_genre_colors_for_channels_enabled))
        val showChannelTagMenu = sharedPreferences.getBoolean("channel_tag_menu_enabled", resources.getBoolean(R.bool.pref_default_channel_tag_menu_enabled))

        if (TextUtils.isEmpty(searchQuery)) {
            menu.findItem(R.id.menu_genre_color_info_channels).isVisible = showGenreColors
            menu.findItem(R.id.menu_timeframe).isVisible = isUnlocked
            menu.findItem(R.id.menu_search).isVisible = recyclerViewAdapter.itemCount > 0

            // Prevent the channel tag menu item from going into the overlay menu
            if (showChannelTagMenu) {
                menu.findItem(R.id.menu_tags).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        } else {
            menu.findItem(R.id.menu_genre_color_info_channels).isVisible = false
            menu.findItem(R.id.menu_timeframe).isVisible = false
            menu.findItem(R.id.menu_search).isVisible = false
            menu.findItem(R.id.menu_tags).isVisible = false
        }

        menu.findItem(R.id.menu_wol).isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_tags -> ChannelTagSelectionDialog.showDialog(activity, channelTags, appRepository.channelData.getItems().size, this)
            R.id.menu_timeframe -> menuUtils.handleMenuTimeSelection(selectedTimeOffset, intervalInHours, 12, this)
            R.id.menu_genre_color_info_channels -> GenreColorDialog.showDialog(activity)
            R.id.menu_sort_order -> menuUtils.handleMenuChannelSortOrderSelection(this)
            R.id.menu_wol -> {
                val connection = appRepository.connectionData.activeItem
                WakeOnLanTask(activity, connection).execute()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onTimeSelected(which: Int) {
        selectedTimeOffset = which
        recyclerView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        // Add the selected list index as extra hours to the current time.
        // If the first index was selected then use the current time.
        var timeInMillis = System.currentTimeMillis()
        timeInMillis += (1000 * 60 * 60 * which * intervalInHours).toLong()
        viewModel.setSelectedTime(timeInMillis)
    }

    override fun onChannelTagIdsSelected(ids: Set<Int>) {
        recyclerView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        viewModel.setSelectedChannelTagIds(ids)
    }

    override fun onChannelSortOrderSelected(id: Int) {
        viewModel.setChannelSortOrder(id)
    }

    /**
     * Show the program list when a channel was selected. In single pane mode a separate
     * activity is called. In dual pane mode a list fragment will be shown in the right side.
     *
     * @param position The selected position in the list
     */
    private fun showChannelDetails(position: Int) {
        selectedListPosition = position
        recyclerViewAdapter.setPosition(position)
        val channel = recyclerViewAdapter.getItem(position)
        if (channel == null || !isVisible
                || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }

        val fm = activity.supportFragmentManager
        if (!isDualPane) {
            // Show the fragment to display the program list of the selected channel.
            val bundle = Bundle()
            bundle.putString("channelName", channel.name)
            bundle.putInt("channelId", channel.id)
            bundle.putLong("selectedTime", selectedTime)

            val fragment = ProgramListFragment()
            fragment.arguments = bundle
            fm.beginTransaction()
                    .replace(R.id.main, fragment)
                    .addToBackStack(null)
                    .commit()
        } else {
            // Check if an instance of the program list fragment for the selected channel is
            // already available. If an instance exist already then update the selected time
            // that was selected from the channel list.
            var fragment = fm.findFragmentById(R.id.details)
            if (fragment !is ProgramListFragment || fragment.shownChannelId != channel.id) {
                fragment = ProgramListFragment.newInstance(channel.name, channel.id, selectedTime)
                fm.beginTransaction()
                        .replace(R.id.details, fragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit()
            } else {
                fragment.updatePrograms(selectedTime)
            }
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val channel = recyclerViewAdapter.getItem(position)
        if (activity == null || channel == null) {
            return
        }

        val program = appRepository.programData.getItemById(channel.programId)
        val recording = appRepository.recordingData.getItemByEventId(channel.programId)

        val popupMenu = PopupMenu(activity, view)
        popupMenu.menuInflater.inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)

        PopupMenuUtil.prepareMenu(activity, popupMenu.menu, program, recording, isNetworkAvailable, htspVersion, isUnlocked)
        PopupMenuUtil.prepareSearchMenu(popupMenu.menu, channel.programTitle, isNetworkAvailable)
        popupMenu.menu.findItem(R.id.menu_play).isVisible = isNetworkAvailable

        popupMenu.setOnMenuItemClickListener { item ->
            if (SearchMenuUtils.onMenuSelected(activity, item.itemId, channel.programTitle)) {
                return@setOnMenuItemClickListener true
            }
            when (item.itemId) {
                R.id.menu_record_stop -> return@setOnMenuItemClickListener menuUtils.handleMenuStopRecordingSelection(recording, null)
                R.id.menu_record_cancel -> return@setOnMenuItemClickListener menuUtils.handleMenuCancelRecordingSelection(recording, null)
                R.id.menu_record_remove -> return@setOnMenuItemClickListener menuUtils.handleMenuRemoveRecordingSelection(recording, null)
                R.id.menu_record_once -> return@setOnMenuItemClickListener menuUtils.handleMenuRecordSelection(channel.programId)
                R.id.menu_record_once_and_edit -> {
                    programIdToBeEditedWhenBeingRecorded = channel.programId
                    return@setOnMenuItemClickListener menuUtils.handleMenuRecordSelection(channel.programId)
                }
                R.id.menu_record_once_custom_profile -> return@setOnMenuItemClickListener menuUtils.handleMenuCustomRecordSelection(channel.programId, channel.id)
                R.id.menu_record_series -> return@setOnMenuItemClickListener menuUtils.handleMenuSeriesRecordSelection(channel.programTitle)
                R.id.menu_play -> return@setOnMenuItemClickListener menuUtils.handleMenuPlayChannel(channel.id)
                R.id.menu_cast -> return@setOnMenuItemClickListener menuUtils.handleMenuCast("channelId", channel.id)
                R.id.menu_add_notification -> if (program != null) {
                    return@setOnMenuItemClickListener menuUtils.handleMenuAddNotificationSelection(program)
                } else {
                    return@setOnMenuItemClickListener false
                }
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    override fun onSearchRequested(query: String) {
        searchQuery = query
        recyclerViewAdapter.filter.filter(query, this)
    }

    override fun onSearchResultsCleared(): Boolean {
        return if (!TextUtils.isEmpty(searchQuery)) {
            Timber.d("Search result not empty, clearing filter and returning true")
            searchQuery = ""
            recyclerViewAdapter.filter.filter("", this)
            true
        } else {
            Timber.d("Search results empty, returning false")
            false
        }
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_channels)
    }

    override fun onClick(view: View, position: Int) {

        if ((view.id == R.id.icon || view.id == R.id.icon_text)
                && Integer.valueOf(sharedPreferences.getString("channel_icon_action", resources.getString(R.string.pref_default_channel_icon_action))!!) > 0
                && recyclerViewAdapter.itemCount > 0
                && isNetworkAvailable) {
            val channel = recyclerViewAdapter.getItem(position)
            menuUtils.handleMenuPlayChannelIcon(channel.id)

        } else {
            showChannelDetails(position)
        }
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        showPopupMenu(view, position)
        return true
    }

    override fun onFilterComplete(count: Int) {
        showChannelTagOrChannelCount()
        // Preselect the first result item in the details screen
        if (isDualPane && recyclerViewAdapter.itemCount > 0) {
            showChannelDetails(0)
        }
    }
}
