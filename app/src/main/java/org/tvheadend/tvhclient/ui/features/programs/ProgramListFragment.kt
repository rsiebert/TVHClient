package org.tvheadend.tvhclient.ui.features.programs

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
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.ui.features.dialogs.GenreColorDialog
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import org.tvheadend.tvhclient.ui.features.search.StartSearchInterface
import org.tvheadend.tvhclient.ui.common.getCastSession
import org.tvheadend.tvhclient.ui.common.onMenuSelected
import org.tvheadend.tvhclient.ui.common.prepareMenu
import org.tvheadend.tvhclient.ui.common.prepareSearchMenu
import timber.log.Timber

class ProgramListFragment : BaseFragment(), RecyclerViewClickCallback, LastProgramVisibleListener, SearchRequestInterface, Filter.FilterListener {

    @BindView(R.id.recycler_view)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.progress_bar)
    lateinit var progressBar: ProgressBar

    lateinit var unbinder: Unbinder
    lateinit var recyclerViewAdapter: ProgramRecyclerViewAdapter
    lateinit var viewModel: ProgramViewModel

    private var selectedTime: Long = 0
    private var selectedListPosition: Int = 0
    var shownChannelId: Int = 0

    lateinit var channelName: String
    lateinit var searchQuery: String

    private var loadingMoreProgramAllowed: Boolean = false
    private var loadingProgramsAllowedTask: Runnable? = null
    private val loadingProgramAllowedHandler = Handler()
    private var programIdToBeEditedWhenBeingRecorded = 0
    private var isSearchActive: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.recyclerview_fragment, container, false)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onDestroyView() {
        recyclerView.adapter = null
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            shownChannelId = savedInstanceState.getInt("channelId", 0)
            channelName = savedInstanceState.getString("channelName", "")
            selectedTime = savedInstanceState.getLong("selectedTime")
            selectedListPosition = savedInstanceState.getInt("listPosition", 0)
            searchQuery = savedInstanceState.getString(SearchManager.QUERY, "")
        } else {
            selectedListPosition = 0
            shownChannelId = arguments?.getInt("channelId", 0) ?: 0
            channelName = arguments?.getString("channelName", "") ?: ""
            selectedTime = arguments?.getLong("selectedTime", System.currentTimeMillis())
                    ?: System.currentTimeMillis()
            searchQuery = arguments?.getString(SearchManager.QUERY) ?: ""
        }

        isSearchActive = !TextUtils.isEmpty(searchQuery)

        if (!isDualPane) {
            toolbarInterface.setTitle(if (isSearchActive) getString(R.string.search_results) else channelName)
        }

        // Show the channel icons when a search is active and all channels shall be searched
        val showProgramChannelIcon = isSearchActive && shownChannelId == 0

        recyclerViewAdapter = ProgramRecyclerViewAdapter(showProgramChannelIcon, this, this)
        recyclerView.layoutManager = LinearLayoutManager(activity.applicationContext)
        recyclerView.addItemDecoration(DividerItemDecoration(activity.applicationContext, LinearLayoutManager.VERTICAL))
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = recyclerViewAdapter

        recyclerView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        viewModel = ViewModelProviders.of(activity).get(ProgramViewModel::class.java)
        if (!isSearchActive) {
            Timber.d("Search is not active, loading programs for channel $channelName from time $selectedTime")
            // A channel id and a channel name was given, load only the programs for the
            // specific channel and from the current time. Also load only those recordings
            // that belong to the given channel
            viewModel.getProgramsByChannelFromTime(shownChannelId, selectedTime).observe(viewLifecycleOwner, Observer<List<Program>> { this.handleObservedPrograms(it) })
            viewModel.getRecordingsByChannelId(shownChannelId).observe(viewLifecycleOwner, Observer<List<Recording>> { this.handleObservedRecordings(it) })

            loadingMoreProgramAllowed = true
            loadingProgramsAllowedTask = Runnable { loadingMoreProgramAllowed = true }

        } else {
            Timber.d("Search is active, loading programs from current time $selectedTime")
            // No channel and channel name was given, load all programs
            // from the current time and all recordings from all channels
            viewModel.getProgramsFromTime(selectedTime).observe(viewLifecycleOwner, Observer<List<Program>> { this.handleObservedPrograms(it) })
            viewModel.recordings?.observe(viewLifecycleOwner, Observer<List<Recording>> { this.handleObservedRecordings(it) })

            loadingMoreProgramAllowed = false
        }
    }

    private fun handleObservedPrograms(programs: List<Program>?) {
        if (programs != null) {
            recyclerViewAdapter.addItems(programs)
        }
        if (isSearchActive) {
            if (activity is StartSearchInterface) {
                (activity as StartSearchInterface).startSearch()
            }
        }
        recyclerView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE

        if (!isDualPane) {
            if (!isSearchActive) {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.programs, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
        // Invalidate the menu so that the search menu item is shown in
        // case the adapter contains items now.
        activity.invalidateOptionsMenu()
    }

    /**
     * Check all recordings for the given channel to see if it belongs to a certain program
     * so the recording status of the particular program can be updated. This is required
     * because the programs are not updated automatically when recordings change.
     *
     * @param recordings The list of recordings
     */
    private fun handleObservedRecordings(recordings: List<Recording>?) {
        if (recordings != null) {
            recyclerViewAdapter.addRecordings(recordings)
            for (recording in recordings) {
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
    }

    override fun onPause() {
        super.onPause()
        loadingProgramAllowedHandler.removeCallbacks(loadingProgramsAllowedTask)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("channelId", shownChannelId)
        outState.putString("channelName", channelName)
        outState.putLong("selectedTime", selectedTime)
        outState.putInt("listPosition", selectedListPosition)
        outState.putString(SearchManager.QUERY, searchQuery)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.program_list_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        // Hide the genre color menu in dual pane mode or if no genre colors shall be shown
        val showGenreColors = sharedPreferences.getBoolean("genre_colors_for_programs_enabled", resources.getBoolean(R.bool.pref_default_genre_colors_for_programs_enabled))

        val genreColorMenu = menu.findItem(R.id.menu_genre_color_info_programs)
        if (genreColorMenu != null) {
            genreColorMenu.isVisible = !isDualPane && showGenreColors
        }

        if (!isSearchActive && isNetworkAvailable) {
            menu.findItem(R.id.menu_play).isVisible = true
            menu.findItem(R.id.menu_cast).isVisible = getCastSession(activity) != null
        } else {
            menu.findItem(R.id.menu_play).isVisible = false
            menu.findItem(R.id.menu_cast).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_play -> {
                menuUtils.handleMenuPlayChannel(shownChannelId)
                true
            }
            R.id.menu_cast -> menuUtils.handleMenuCast("channelId", shownChannelId)
            R.id.menu_genre_color_info_programs -> GenreColorDialog.showDialog(activity)
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showProgramDetails(position: Int) {
        selectedListPosition = position
        val program = recyclerViewAdapter.getItem(position)
        if (program == null
                || !isVisible
                || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }

        val fragment = ProgramDetailsFragment.newInstance(program.eventId, program.channelId)
        val ft = activity.supportFragmentManager.beginTransaction()
        ft.replace(R.id.main, fragment)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        ft.addToBackStack(null)
        ft.commit()
    }

    private fun showPopupMenu(view: View, position: Int) {
        val program = recyclerViewAdapter.getItem(position)
        if (activity == null || program == null) {
            return
        }

        val recording = appRepository.recordingData.getItemByEventId(program.eventId)

        val popupMenu = PopupMenu(activity, view)
        popupMenu.menuInflater.inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)
        prepareMenu(activity, popupMenu.menu, program, program.recording, isNetworkAvailable, htspVersion, isUnlocked)
        prepareSearchMenu(popupMenu.menu, program.title, isNetworkAvailable)

        popupMenu.setOnMenuItemClickListener { item ->
            if (onMenuSelected(activity, item.itemId, program.title, program.channelId)) {
                return@setOnMenuItemClickListener true
            }
            when (item.itemId) {
                R.id.menu_record_stop -> return@setOnMenuItemClickListener menuUtils.handleMenuStopRecordingSelection(recording, null)
                R.id.menu_record_cancel -> return@setOnMenuItemClickListener menuUtils.handleMenuCancelRecordingSelection(recording, null)
                R.id.menu_record_remove -> return@setOnMenuItemClickListener menuUtils.handleMenuRemoveRecordingSelection(recording, null)
                R.id.menu_record_once -> return@setOnMenuItemClickListener menuUtils.handleMenuRecordSelection(program.eventId)
                R.id.menu_record_once_and_edit -> {
                    programIdToBeEditedWhenBeingRecorded = program.eventId
                    return@setOnMenuItemClickListener menuUtils.handleMenuRecordSelection(program.eventId)
                }
                R.id.menu_record_once_custom_profile -> return@setOnMenuItemClickListener menuUtils.handleMenuCustomRecordSelection(program.eventId, shownChannelId)
                R.id.menu_record_series -> return@setOnMenuItemClickListener menuUtils.handleMenuSeriesRecordSelection(program.title)
                R.id.menu_play -> return@setOnMenuItemClickListener menuUtils.handleMenuPlayChannel(shownChannelId)
                R.id.menu_cast -> return@setOnMenuItemClickListener menuUtils.handleMenuCast("channelId", shownChannelId)
                R.id.menu_add_notification -> return@setOnMenuItemClickListener menuUtils.handleMenuAddNotificationSelection(program)
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    override fun onSearchRequested(query: String) {
        recyclerViewAdapter.filter.filter(query, this)
    }

    override fun onSearchResultsCleared(): Boolean {
        return if (!TextUtils.isEmpty(searchQuery)) {
            searchQuery = ""
            recyclerViewAdapter.filter.filter("", this)
            true
        } else {
            false
        }
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_programs)
    }

    override fun onLastProgramVisible(position: Int) {
        // Do not load more programs when a search query was given or all programs were loaded.
        if (isSearchActive || !loadingMoreProgramAllowed || !isNetworkAvailable) {
            return
        }

        loadingMoreProgramAllowed = false
        loadingProgramAllowedHandler.postDelayed(loadingProgramsAllowedTask, 2000)

        val lastProgram = recyclerViewAdapter.getItem(position)
        Timber.d("Loading more programs after " + lastProgram.title!!)

        val intent = Intent(activity, HtspService::class.java)
        intent.action = "getEvents"
        intent.putExtra("eventId", lastProgram.nextEventId)
        intent.putExtra("channelId", lastProgram.channelId)
        intent.putExtra("channelName", channelName)
        intent.putExtra("numFollowing", 25)
        intent.putExtra("showMessage", true)

        if (MainApplication.isActivityVisible()) {
            activity.startService(intent)
        }
    }

    override fun onFilterComplete(count: Int) {
        if (!isDualPane) {
            if (!isSearchActive) {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.programs, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun onClick(view: View, position: Int) {
        showProgramDetails(position)
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        showPopupMenu(view, position)
        return true
    }

    fun updatePrograms(selectedTime: Long) {
        this.selectedTime = selectedTime
        viewModel.getProgramsByChannelFromTime(shownChannelId, selectedTime).observe(viewLifecycleOwner, Observer<List<Program>> { this.handleObservedPrograms(it) })
    }

    companion object {

        fun newInstance(channelName: String, channelId: Int, selectedTime: Long): ProgramListFragment {
            val f = ProgramListFragment()
            val args = Bundle()
            args.putString("channelName", channelName)
            args.putInt("channelId", channelId)
            args.putLong("selectedTime", selectedTime)
            f.arguments = args
            return f
        }
    }
}
