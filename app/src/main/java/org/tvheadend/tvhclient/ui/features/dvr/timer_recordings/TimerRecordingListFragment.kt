package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.os.Bundle
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.TimerRecording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

class TimerRecordingListFragment : BaseFragment(), RecyclerViewClickCallback, SearchRequestInterface, Filter.FilterListener {

    private lateinit var timerRecordingViewModel: TimerRecordingViewModel
    private lateinit var recyclerViewAdapter: TimerRecordingRecyclerViewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        timerRecordingViewModel = ViewModelProviders.of(activity!!).get(TimerRecordingViewModel::class.java)

        arguments?.let {
            timerRecordingViewModel.selectedListPosition = it.getInt("listPosition")
        }

        recyclerViewAdapter = TimerRecordingRecyclerViewAdapter(isDualPane, this, htspVersion)
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.adapter = recyclerViewAdapter
        recycler_view.gone()

        timerRecordingViewModel.recordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
                observeSearchQuery()
            }

            recycler_view?.visible()
            showStatusInToolbar()
            activity?.invalidateOptionsMenu()

            if (isDualPane && recyclerViewAdapter.itemCount > 0) {
                showRecordingDetails(timerRecordingViewModel.selectedListPosition)
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
        context?.let {
            if (!baseViewModel.isSearchActive) {
                toolbarInterface.setTitle(getString(R.string.timer_recordings))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setTitle(getString(R.string.search_results))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.timer_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_add_recording -> addNewTimerRecording(ctx)
            R.id.menu_remove_all_recordings -> showConfirmationToRemoveAllTimerRecordings(ctx, CopyOnWriteArrayList(recyclerViewAdapter.items))
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.recording_list_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Enable the remove all recordings menu if there are at least 2 recordings available
        if (sharedPreferences.getBoolean("delete_all_recordings_menu_enabled", resources.getBoolean(R.bool.pref_default_delete_all_recordings_menu_enabled))
                && recyclerViewAdapter.itemCount > 1
                && isConnectionToServerAvailable) {
            menu.findItem(R.id.menu_remove_all_recordings)?.isVisible = true
        }
        menu.findItem(R.id.menu_add_recording)?.isVisible = isConnectionToServerAvailable
        menu.findItem(R.id.menu_search)?.isVisible = recyclerViewAdapter.itemCount > 0
        menu.findItem(R.id.media_route_menu_item)?.isVisible = false
    }

    private fun showRecordingDetails(position: Int) {
        timerRecordingViewModel.selectedListPosition = position
        recyclerViewAdapter.setPosition(position)

        val recording = recyclerViewAdapter.getItem(position)
        if (recording == null || !isVisible) {
            return
        }

        val fm = activity?.supportFragmentManager
        if (!isDualPane) {
            val fragment = TimerRecordingDetailsFragment.newInstance(recording.id)
            fm?.beginTransaction()?.also {
                it.replace(R.id.main, fragment)
                it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                it.addToBackStack(null)
                it.commit()
            }
        } else {
            // Check what fragment is currently shown, replace if needed.
            var fragment = activity?.supportFragmentManager?.findFragmentById(R.id.main)
            if (fragment !is TimerRecordingDetailsFragment || timerRecordingViewModel.currentId != recording.id) {
                // Make new fragment to show this selection.
                fragment = TimerRecordingDetailsFragment.newInstance(recording.id)
                fm?.beginTransaction()?.also {
                    it.replace(R.id.details, fragment)
                    it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    it.commit()
                }
            }
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val ctx = context ?: return
        val timerRecording = recyclerViewAdapter.getItem(position) ?: return

        val popupMenu = PopupMenu(ctx, view)
        popupMenu.menuInflater.inflate(R.menu.timer_recordings_popup_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)

        preparePopupOrToolbarSearchMenu(popupMenu.menu, timerRecording.title, isConnectionToServerAvailable)
        popupMenu.menu.findItem(R.id.menu_disable_recording)?.isVisible = htspVersion >= 19 && timerRecording.isEnabled
        popupMenu.menu.findItem(R.id.menu_enable_recording)?.isVisible = htspVersion >= 19 && !timerRecording.isEnabled

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit_recording -> return@setOnMenuItemClickListener editSelectedTimerRecording(ctx, timerRecording.id)
                R.id.menu_remove_recording -> return@setOnMenuItemClickListener showConfirmationToRemoveSelectedTimerRecording(ctx, timerRecording, null)
                R.id.menu_disable_recording -> return@setOnMenuItemClickListener enableTimerRecording(timerRecording, false)
                R.id.menu_enable_recording -> return@setOnMenuItemClickListener enableTimerRecording(timerRecording, true)

                R.id.menu_search_imdb -> return@setOnMenuItemClickListener searchTitleOnImdbWebsite(ctx, timerRecording.title)
                R.id.menu_search_fileaffinity -> return@setOnMenuItemClickListener searchTitleOnFileAffinityWebsite(ctx, timerRecording.title)
                R.id.menu_search_youtube -> return@setOnMenuItemClickListener searchTitleOnYoutube(ctx, timerRecording.title)
                R.id.menu_search_google -> return@setOnMenuItemClickListener searchTitleOnGoogle(ctx, timerRecording.title)
                R.id.menu_search_epg -> return@setOnMenuItemClickListener searchTitleInTheLocalDatabase(activity!!, baseViewModel, timerRecording.title)
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    private fun enableTimerRecording(timerRecording: TimerRecording, enabled: Boolean): Boolean {
        val intent = timerRecordingViewModel.getIntentData(timerRecording)
        intent.action = "updateTimerecEntry"
        intent.putExtra("id", timerRecording.id)
        intent.putExtra("enabled", if (enabled) 1 else 0)
        activity?.startService(intent)
        return true
    }

    override fun onClick(view: View, position: Int) {
        showRecordingDetails(position)
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        showPopupMenu(view, position)
        return true
    }

    override fun onFilterComplete(i: Int) {
        showStatusInToolbar()
        // Preselect the first result item in the details screen
        if (isDualPane && recyclerViewAdapter.itemCount > 0) {
            showRecordingDetails(0)
        }
    }

    override fun onSearchRequested(query: String) {
        recyclerViewAdapter.filter.filter(query, this)
    }

    override fun onSearchResultsCleared() {
        recyclerViewAdapter.filter.filter("", this)
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_timer_recordings)
    }
}
