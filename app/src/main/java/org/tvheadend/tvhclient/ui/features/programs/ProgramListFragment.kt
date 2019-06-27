package org.tvheadend.tvhclient.ui.features.programs

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.ProgramInterface
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.notification.addNotificationProgramIsAboutToStart
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import org.tvheadend.tvhclient.ui.features.search.StartSearchInterface
import timber.log.Timber

class ProgramListFragment : BaseFragment(), RecyclerViewClickCallback, LastProgramVisibleListener, SearchRequestInterface, Filter.FilterListener {

    lateinit var recyclerViewAdapter: ProgramRecyclerViewAdapter
    private lateinit var programViewModel: ProgramViewModel

    private var selectedTime: Long = 0
    private var selectedListPosition: Int = 0
    var shownChannelId: Int = 0

    var channelName: String = ""
    var searchQuery: String = ""

    private var loadingMoreProgramAllowed: Boolean = false
    private var loadingProgramsAllowedTask: Runnable? = null
    private val loadingProgramAllowedHandler = Handler()
    private var programIdToBeEditedWhenBeingRecorded = 0
    private var isSearchActive: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        programViewModel = ViewModelProviders.of(activity!!).get(ProgramViewModel::class.java)

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

        isSearchActive = searchQuery.isNotEmpty()

        if (!isDualPane) {
            toolbarInterface.setTitle(if (isSearchActive) getString(R.string.search_results) else channelName)
        }

        // Show the channel icons when a search is active and all channels shall be searched
        programViewModel.showProgramChannelIcon = isSearchActive && shownChannelId == 0

        recyclerViewAdapter = ProgramRecyclerViewAdapter(programViewModel, this, this)
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        recycler_view.itemAnimator = DefaultItemAnimator()
        recycler_view.adapter = recyclerViewAdapter
        recycler_view.gone()

        if (!isSearchActive) {
            Timber.d("Search is not active, loading programs for channel $channelName from time $selectedTime")
            // A channel id and a channel name was given, load only the programs for the
            // specific channel and from the current time. Also load only those recordings
            // that belong to the given channel
            programViewModel.getProgramsByChannelFromTime(shownChannelId, selectedTime).observe(viewLifecycleOwner, Observer { this.handleObservedPrograms(it) })
            programViewModel.getRecordingsByChannelId(shownChannelId).observe(viewLifecycleOwner, Observer { this.handleObservedRecordings(it) })

            loadingMoreProgramAllowed = true
            loadingProgramsAllowedTask = Runnable { loadingMoreProgramAllowed = true }

        } else {
            Timber.d("Search is active, loading programs from current time $selectedTime")
            // No channel and channel name was given, load all programs
            // from the current time and all recordings from all channels
            programViewModel.getProgramsFromTime(selectedTime).observe(viewLifecycleOwner, Observer { this.handleObservedPrograms(it) })
            programViewModel.recordings?.observe(viewLifecycleOwner, Observer { this.handleObservedRecordings(it) })

            loadingMoreProgramAllowed = false
        }
    }

    private fun handleObservedPrograms(programs: List<ProgramInterface>?) {
        if (programs != null) {
            recyclerViewAdapter.addItems(programs.toMutableList())
        }
        if (isSearchActive) {
            if (activity is StartSearchInterface) {
                (activity as StartSearchInterface).startSearch()
            }
        }
        recycler_view?.visible()

        if (!isDualPane) {
            if (!isSearchActive) {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.programs, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
        // Invalidate the menu so that the search menu item is shown in
        // case the adapter contains items now.
        activity?.invalidateOptionsMenu()
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
                    activity?.startActivity(intent)
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
        val ctx = context ?: return
        val showGenreColors = sharedPreferences.getBoolean("genre_colors_for_programs_enabled", resources.getBoolean(R.bool.pref_default_genre_colors_for_programs_enabled))
        // Hide the genre color menu in dual pane mode or if no genre colors shall be shown
        menu.findItem(R.id.menu_genre_color_information)?.isVisible = !isDualPane && showGenreColors

        if (!isSearchActive && isNetworkAvailable) {
            menu.findItem(R.id.menu_play)?.isVisible = true
            menu.findItem(R.id.menu_cast)?.isVisible = getCastSession(ctx) != null
        } else {
            menu.findItem(R.id.menu_play)?.isVisible = false
            menu.findItem(R.id.menu_cast)?.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_play -> playSelectedChannel(ctx, shownChannelId, isUnlocked)
            R.id.menu_cast -> castSelectedChannel(ctx, shownChannelId)
            R.id.menu_genre_color_information -> showGenreColorDialog(ctx)
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
        activity?.supportFragmentManager?.beginTransaction()?.also {
            it.replace(R.id.main, fragment)
            it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            it.addToBackStack(null)
            it.commit()
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val ctx = context ?: return
        val program = recyclerViewAdapter.getItem(position) ?: return

        val popupMenu = PopupMenu(ctx, view)
        popupMenu.menuInflater.inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)

        preparePopupOrToolbarRecordingMenu(ctx, popupMenu.menu, program.recording, isNetworkAvailable, htspVersion, isUnlocked)
        preparePopupOrToolbarSearchMenu(popupMenu.menu, program.title, isNetworkAvailable)
        preparePopupOrToolbarMiscMenu(ctx, popupMenu.menu, program, isNetworkAvailable, isUnlocked)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_stop_recording -> return@setOnMenuItemClickListener showConfirmationToStopSelectedRecording(ctx, program.recording, null)
                R.id.menu_cancel_recording -> return@setOnMenuItemClickListener showConfirmationToCancelSelectedRecording(ctx, program.recording, null)
                R.id.menu_remove_recording -> return@setOnMenuItemClickListener showConfirmationToRemoveSelectedRecording(ctx, program.recording, null)
                R.id.menu_record_program -> return@setOnMenuItemClickListener recordSelectedProgram(ctx, program.eventId, programViewModel.getRecordingProfile(), htspVersion)
                R.id.menu_record_program_and_edit -> {
                    programIdToBeEditedWhenBeingRecorded = program.eventId
                    return@setOnMenuItemClickListener recordSelectedProgram(ctx, program.eventId, programViewModel.getRecordingProfile(), htspVersion)
                }
                R.id.menu_record_program_with_custom_profile -> return@setOnMenuItemClickListener recordSelectedProgramWithCustomProfile(ctx, program.eventId, shownChannelId, programViewModel.getRecordingProfileNames(), programViewModel.getRecordingProfile())
                R.id.menu_record_program_as_series_recording -> return@setOnMenuItemClickListener recordSelectedProgramAsSeriesRecording(ctx, program.title, programViewModel.getRecordingProfile(), htspVersion)
                R.id.menu_play -> return@setOnMenuItemClickListener playSelectedChannel(ctx, shownChannelId, isUnlocked)
                R.id.menu_cast -> return@setOnMenuItemClickListener castSelectedChannel(ctx, shownChannelId)

                R.id.menu_search_imdb -> return@setOnMenuItemClickListener searchTitleOnImdbWebsite(ctx, program.title)
                R.id.menu_search_fileaffinity -> return@setOnMenuItemClickListener searchTitleOnFileAffinityWebsite(ctx, program.title)
                R.id.menu_search_youtube -> return@setOnMenuItemClickListener searchTitleOnYoutube(ctx, program.title)
                R.id.menu_search_google -> return@setOnMenuItemClickListener searchTitleOnGoogle(ctx, program.title)
                R.id.menu_search_epg -> return@setOnMenuItemClickListener searchTitleInTheLocalDatabase(ctx, program.title, program.channelId)

                R.id.menu_add_notification -> return@setOnMenuItemClickListener addNotificationProgramIsAboutToStart(ctx, program, programViewModel.getRecordingProfile())
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    override fun onSearchRequested(query: String) {
        recyclerViewAdapter.filter.filter(query, this)
    }

    override fun onSearchResultsCleared(): Boolean {
        return if (searchQuery.isNotEmpty()) {
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
        lastProgram?.let {
            Timber.d("Loading more programs after ${lastProgram.title}")

            val intent = Intent(activity, HtspService::class.java)
            intent.action = "getEvents"
            intent.putExtra("eventId", lastProgram.nextEventId)
            intent.putExtra("channelId", lastProgram.channelId)
            intent.putExtra("channelName", channelName)
            intent.putExtra("numFollowing", 25)
            intent.putExtra("showMessage", true)

            activity?.startService(intent)
        }
    }

    override fun onFilterComplete(count: Int) {
        if (!isDualPane) {
            context?.let {
                if (!isSearchActive) {
                    toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
                } else {
                    toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.programs, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
                }
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
        programViewModel.getProgramsByChannelFromTime(shownChannelId, selectedTime).observe(viewLifecycleOwner, Observer<List<Program>> { this.handleObservedPrograms(it) })
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
