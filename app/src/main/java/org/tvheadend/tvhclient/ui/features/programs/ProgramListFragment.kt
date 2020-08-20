package org.tvheadend.tvhclient.ui.features.programs

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.ClearSearchResultsOrPopBackStackInterface
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.ui.common.interfaces.SearchRequestInterface
import org.tvheadend.tvhclient.util.extensions.getCastSession
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber

class ProgramListFragment : BaseFragment(), RecyclerViewClickInterface, LastProgramVisibleListener, SearchRequestInterface, Filter.FilterListener, ClearSearchResultsOrPopBackStackInterface {

    lateinit var recyclerViewAdapter: ProgramRecyclerViewAdapter
    private lateinit var programViewModel: ProgramViewModel
    private var loadingMoreProgramAllowed: Boolean = false
    private var programIdToBeEditedWhenBeingRecorded = 0
    private var lastProgramItemCount = 0
    private var channelId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        programViewModel = ViewModelProvider(requireActivity()).get(ProgramViewModel::class.java)

        arguments?.let {
            programViewModel.channelId = it.getInt("channelId", 0)
            programViewModel.channelIdLiveData.value = it.getInt("channelId", 0)
            programViewModel.selectedTimeLiveData.value = it.getLong("selectedTime", System.currentTimeMillis())
            programViewModel.channelName = it.getString("channelName", "")
        }

        // Show the channel icons when a search is active and all channels shall be searched
        programViewModel.showProgramChannelIcon = baseViewModel.isSearchActive && channelId == 0

        recyclerViewAdapter = ProgramRecyclerViewAdapter(programViewModel, this, this)
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.adapter = recyclerViewAdapter
        recycler_view.gone()
        search_progress?.visibleOrGone(baseViewModel.isSearchActive)

        Timber.d("Observing programs")
        programViewModel.programs.observe(viewLifecycleOwner,  { progs ->
            if (progs != null) {
                Timber.d("View model returned ${progs.size} programs")
                recyclerViewAdapter.addItems(progs.toMutableList())
                observeSearchQuery()
                observeRecordings()
            }

            recycler_view?.visible()
            showStatusInToolbar()
            activity?.invalidateOptionsMenu()
        })

        Timber.d("Observing channel id")
        programViewModel.channelIdLiveData.observe(viewLifecycleOwner,  { id ->
            if (id != null) {
                Timber.d("View model returned channel id $id")
                channelId = id
            }
        })
    }

    private fun observeRecordings() {
        Timber.d("Observing recordings")
        programViewModel.recordings.observe(viewLifecycleOwner,  { recs ->
            if (recs != null) {
                Timber.d("View model returned ${recs.size} recordings")
                handleObservedRecordings(recs)
            }
        })
    }

    private fun observeSearchQuery() {
        Timber.d("Observing search query")
        baseViewModel.searchQueryLiveData.observe(viewLifecycleOwner,  { query ->
            loadingMoreProgramAllowed = if (query.isNotEmpty()) {
                Timber.d("View model returned search query '$query'")
                onSearchRequested(query)
                false

            } else {
                Timber.d("View model returned empty search query")
                onSearchResultsCleared()
                true
            }
        })
    }

    /**
     * Check all recordings for the given channel to see if it belongs to a certain program
     * so the recording status of the particular program can be updated. This is required
     * because the programs are not updated automatically when recordings change.
     *
     * @param recordings The list of recordings
     */
    private fun handleObservedRecordings(recordings: List<Recording>) {
        recyclerViewAdapter.addRecordings(recordings)
        for (recording in recordings) {
            if (recording.eventId == programIdToBeEditedWhenBeingRecorded && programIdToBeEditedWhenBeingRecorded > 0) {
                programIdToBeEditedWhenBeingRecorded = 0
                editSelectedRecording(requireActivity(), recording.id)
                break
            }
        }
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

        if (!baseViewModel.isSearchActive && isConnectionToServerAvailable) {
            menu.findItem(R.id.menu_play)?.isVisible = true
            menu.findItem(R.id.menu_cast)?.isVisible = ctx.getCastSession() != null
        } else {
            menu.findItem(R.id.menu_play)?.isVisible = false
            menu.findItem(R.id.menu_cast)?.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_play -> playSelectedChannel(ctx, channelId, isUnlocked)
            R.id.menu_cast -> castSelectedChannel(ctx, channelId)
            R.id.menu_genre_color_information -> showGenreColorDialog(ctx)
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showProgramDetails(position: Int) {
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

        preparePopupOrToolbarRecordingMenu(ctx, popupMenu.menu, program.recording, isConnectionToServerAvailable, htspVersion, isUnlocked)
        preparePopupOrToolbarSearchMenu(popupMenu.menu, program.title, isConnectionToServerAvailable)
        preparePopupOrToolbarMiscMenu(ctx, popupMenu.menu, program, isConnectionToServerAvailable, isUnlocked)

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
                R.id.menu_record_program_with_custom_profile -> return@setOnMenuItemClickListener recordSelectedProgramWithCustomProfile(ctx, program.eventId, program.channelId, programViewModel.getRecordingProfileNames(), programViewModel.getRecordingProfile())
                R.id.menu_record_program_as_series_recording -> return@setOnMenuItemClickListener recordSelectedProgramAsSeriesRecording(ctx, program.title, programViewModel.getRecordingProfile(), htspVersion)
                R.id.menu_play -> return@setOnMenuItemClickListener playSelectedChannel(ctx, channelId, isUnlocked)
                R.id.menu_cast -> return@setOnMenuItemClickListener castSelectedChannel(ctx, channelId)

                R.id.menu_search_imdb -> return@setOnMenuItemClickListener searchTitleOnImdbWebsite(ctx, program.title)
                R.id.menu_search_fileaffinity -> return@setOnMenuItemClickListener searchTitleOnFileAffinityWebsite(ctx, program.title)
                R.id.menu_search_youtube -> return@setOnMenuItemClickListener searchTitleOnYoutube(ctx, program.title)
                R.id.menu_search_google -> return@setOnMenuItemClickListener searchTitleOnGoogle(ctx, program.title)
                R.id.menu_search_epg -> return@setOnMenuItemClickListener searchTitleInTheLocalDatabase(requireActivity(), baseViewModel, program.title, program.channelId)

                R.id.menu_add_notification -> return@setOnMenuItemClickListener addNotificationProgramIsAboutToStart(ctx, program, programViewModel.getRecordingProfile())
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

    override fun onLastProgramVisible(position: Int) {
        // Do not load more programs when a search query was given or all programs were loaded.
        if (baseViewModel.isSearchActive
                || !loadingMoreProgramAllowed
                || !isConnectionToServerAvailable
                || recyclerViewAdapter.itemCount == lastProgramItemCount) {
            return
        }
        lastProgramItemCount = recyclerViewAdapter.itemCount

        val lastProgram = recyclerViewAdapter.getItem(position)
        lastProgram?.let {
            Timber.d("Loading more programs after ${lastProgram.title}")

            val intent = Intent(activity, HtspService::class.java)
            intent.action = "getEvents"
            intent.putExtra("eventId", lastProgram.nextEventId)
            intent.putExtra("channelId", lastProgram.channelId)
            intent.putExtra("channelName", programViewModel.channelName)
            intent.putExtra("numFollowing", 25)
            intent.putExtra("showMessage", true)

            activity?.startService(intent)
        }
    }

    override fun onFilterComplete(count: Int) {
        showStatusInToolbar()
        search_progress?.gone()
    }

    private fun showStatusInToolbar() {
        context?.let {
            if (!isDualPane) {
                if (!baseViewModel.isSearchActive) {
                    toolbarInterface.setTitle(programViewModel.channelName)
                    toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.programs, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
                } else {
                    toolbarInterface.setTitle(getString(R.string.search_results))
                    toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.programs, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
                }
            } else {
                val toolbarTitle = "${programViewModel.channelName}: ${it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount)}"
                toolbarInterface.setSubtitle(toolbarTitle)
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

    companion object {

        fun newInstance(channelName: String = "", channelId: Int = 0, selectedTime: Long = System.currentTimeMillis()): ProgramListFragment {
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
