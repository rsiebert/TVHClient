package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.ui.common.interfaces.SearchRequestInterface
import org.tvheadend.tvhclient.ui.features.dvr.recordings.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.ui.features.dvr.recordings.download.DownloadRecordingManager
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

abstract class RecordingListFragment : BaseFragment(), RecyclerViewClickInterface, SearchRequestInterface, DownloadPermissionGrantedInterface, Filter.FilterListener {

    lateinit var recordingViewModel: RecordingViewModel
    lateinit var recyclerViewAdapter: RecordingRecyclerViewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recordingViewModel = ViewModelProvider(activity!!).get(RecordingViewModel::class.java)

        arguments?.let {
            recordingViewModel.selectedListPosition = it.getInt("listPosition")
        }

        recyclerViewAdapter = RecordingRecyclerViewAdapter(recordingViewModel, isDualPane, this, htspVersion)
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.adapter = recyclerViewAdapter
        recycler_view.gone()
        search_progress?.visibleOrGone(baseViewModel.isSearchActive)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_add_recording -> addNewRecording(ctx)
            R.id.menu_remove_all_recordings -> showConfirmationToRemoveAllRecordings(ctx, CopyOnWriteArrayList(recyclerViewAdapter.items))
            R.id.menu_genre_color_information -> showGenreColorDialog(ctx)
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.recording_list_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        if (sharedPreferences.getBoolean("delete_all_recordings_menu_enabled", resources.getBoolean(R.bool.pref_default_delete_all_recordings_menu_enabled))
                && recyclerViewAdapter.itemCount > 1
                && isConnectionToServerAvailable) {
            menu.findItem(R.id.menu_remove_all_recordings)?.isVisible = true
        }
        // Hide the casting icon as a default.
        menu.findItem(R.id.media_route_menu_item)?.isVisible = false
        // Hide the sorting menu by default, only the completed recordings can be sorted
        menu.findItem(R.id.menu_recording_sort_order)?.isVisible = false
        // Do not show the search menu when no recordings are available
        menu.findItem(R.id.menu_search)?.isVisible = recyclerViewAdapter.itemCount > 0

        val showGenreColors = sharedPreferences.getBoolean("genre_colors_for_recordings_enabled", resources.getBoolean(R.bool.pref_default_genre_colors_for_recordings_enabled))
        if (!baseViewModel.isSearchActive) {
            menu.findItem(R.id.menu_genre_color_information)?.isVisible = showGenreColors
        }
    }

    private fun showRecordingDetails(position: Int) {
        recordingViewModel.selectedListPosition = position
        recyclerViewAdapter.setPosition(position)

        val recording = recyclerViewAdapter.getItem(position)
        if (recording == null || !isVisible) {
            return
        }

        val fm = activity?.supportFragmentManager
        if (!isDualPane) {
            val fragment = RecordingDetailsFragment.newInstance(recording.id)
            fm?.beginTransaction()?.also {
                it.replace(R.id.main, fragment)
                it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                it.addToBackStack(null)
                it.commit()
            }
        } else {
            var fragment = activity?.supportFragmentManager?.findFragmentById(R.id.details)
            if (fragment !is RecordingDetailsFragment) {
                fragment = RecordingDetailsFragment.newInstance(recording.id)

                // Check the lifecycle state to avoid committing the transaction
                // after the onSaveInstance method was already called which would
                // trigger an illegal state exception.
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    fm?.beginTransaction()?.also {
                        it.replace(R.id.details, fragment)
                        it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        it.commit()
                    }
                }
            } else if (recordingViewModel.currentId.value != recording.id) {
                recordingViewModel.currentId.value = recording.id
            }
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val ctx = context ?: return
        val recording = recyclerViewAdapter.getItem(position) ?: return

        val popupMenu = PopupMenu(ctx, view)
        popupMenu.menuInflater.inflate(R.menu.recordings_popup_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)

        preparePopupOrToolbarMiscMenu(ctx, popupMenu.menu, null, isConnectionToServerAvailable, isUnlocked)
        preparePopupOrToolbarRecordingMenu(ctx, popupMenu.menu, recording, isConnectionToServerAvailable, htspVersion, isUnlocked)
        preparePopupOrToolbarSearchMenu(popupMenu.menu, recording.title, isConnectionToServerAvailable)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_stop_recording -> return@setOnMenuItemClickListener showConfirmationToStopSelectedRecording(ctx, recording, null)
                R.id.menu_cancel_recording -> return@setOnMenuItemClickListener showConfirmationToCancelSelectedRecording(ctx, recording, null)
                R.id.menu_remove_recording -> return@setOnMenuItemClickListener showConfirmationToRemoveSelectedRecording(ctx, recording, null)
                R.id.menu_edit_recording -> return@setOnMenuItemClickListener editSelectedRecording(ctx, recording.id)
                R.id.menu_play -> return@setOnMenuItemClickListener playSelectedRecording(ctx, recording.id, isUnlocked)
                R.id.menu_cast -> return@setOnMenuItemClickListener castSelectedRecording(ctx, recording.id)

                R.id.menu_search_imdb -> return@setOnMenuItemClickListener searchTitleOnImdbWebsite(ctx, recording.title)
                R.id.menu_search_fileaffinity -> return@setOnMenuItemClickListener searchTitleOnFileAffinityWebsite(ctx, recording.title)
                R.id.menu_search_youtube -> return@setOnMenuItemClickListener searchTitleOnYoutube(ctx, recording.title)
                R.id.menu_search_google -> return@setOnMenuItemClickListener searchTitleOnGoogle(ctx, recording.title)
                R.id.menu_search_epg -> return@setOnMenuItemClickListener searchTitleInTheLocalDatabase(activity!!, baseViewModel, recording.title)

                R.id.menu_disable_recording -> return@setOnMenuItemClickListener enableScheduledRecording(recording, false)
                R.id.menu_enable_recording -> return@setOnMenuItemClickListener enableScheduledRecording(recording, true)
                R.id.menu_download_recording -> {
                    DownloadRecordingManager(activity, connection, recording)
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    override fun onClick(view: View, position: Int) {
        recordingViewModel.selectedListPosition = position
        if (view.id == R.id.icon || view.id == R.id.icon_text) {
            recyclerViewAdapter.getItem(position)?.let {
                playOrCastRecording(view.context, it.id, isUnlocked)
            }
        } else {
            showRecordingDetails(position)
        }
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        showPopupMenu(view, position)
        return true
    }

    fun addRecordingsAndUpdateUI(recordings: List<Recording>?) {
        // Prevent updating the recording list and any calls to the filter in the adapter.
        // Without this the active search view would be closed before the user has a chance
        // to enter a complete search term or submit the query.
        if (baseViewModel.searchViewHasFocus || baseViewModel.isSearchActive) {
            Timber.d("Skipping updating UI, search view has focus or search results are already being shown")
            return
        }

        if (recordings != null) {
            recyclerViewAdapter.addItems(recordings)
            observeSearchQuery()
        }
        recycler_view?.visible()
        showStatusInToolbar()
        activity?.invalidateOptionsMenu()

        if (isDualPane && recyclerViewAdapter.itemCount > 0) {
            showRecordingDetails(recordingViewModel.selectedListPosition)
        }
    }

    abstract fun showStatusInToolbar()

    override fun downloadRecording() {
        DownloadRecordingManager(activity, connection, recyclerViewAdapter.getItem(recordingViewModel.selectedListPosition))
    }

    override fun onFilterComplete(i: Int) {
        search_progress?.gone()
        showStatusInToolbar()

        if (isDualPane) {
            when {
                recyclerViewAdapter.itemCount > recordingViewModel.selectedListPosition -> {
                    showRecordingDetails(recordingViewModel.selectedListPosition)
                }
                recyclerViewAdapter.itemCount <= recordingViewModel.selectedListPosition -> {
                    showRecordingDetails(0)
                }
                recyclerViewAdapter.itemCount == 0 -> {
                    removeDetailsFragment()
                }
            }
        }
    }

    override fun onSearchRequested(query: String) {
        recyclerViewAdapter.filter.filter(query, this)
    }

    override fun onSearchResultsCleared() {
        recyclerViewAdapter.filter.filter("", this)
    }

    abstract override fun getQueryHint(): String

    private fun enableScheduledRecording(recording: Recording, enabled: Boolean): Boolean {
        val intent = recordingViewModel.getIntentData(recording)
        intent.action = "updateDvrEntry"
        intent.putExtra("id", recording.id)
        intent.putExtra("enabled", if (enabled) 1 else 0)
        activity?.startService(intent)
        return true
    }
}
