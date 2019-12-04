package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.os.Bundle
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.ui.common.interfaces.SearchRequestInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

class SeriesRecordingListFragment : BaseFragment(), RecyclerViewClickInterface, SearchRequestInterface, Filter.FilterListener {

    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel
    private lateinit var recyclerViewAdapter: SeriesRecordingRecyclerViewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        seriesRecordingViewModel = ViewModelProviders.of(activity!!).get(SeriesRecordingViewModel::class.java)

        arguments?.let {
            seriesRecordingViewModel.selectedListPosition = it.getInt("listPosition")
        }

        recyclerViewAdapter = SeriesRecordingRecyclerViewAdapter(isDualPane, this, htspVersion)
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.adapter = recyclerViewAdapter
        recycler_view.gone()
        search_progress?.visibleOrGone(baseViewModel.isSearchActive)

        seriesRecordingViewModel.recordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
                observeSearchQuery()
            }
            recycler_view?.visible()
            showStatusInToolbar()
            activity?.invalidateOptionsMenu()

            if (isDualPane && recyclerViewAdapter.itemCount > 0) {
                showRecordingDetails(seriesRecordingViewModel.selectedListPosition)
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
                toolbarInterface.setTitle(getString(R.string.series_recordings))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setTitle(getString(R.string.search_results))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.series_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_add_recording -> addNewSeriesRecording(ctx)
            R.id.menu_remove_all_recordings -> showConfirmationToRemoveAllSeriesRecordings(ctx, CopyOnWriteArrayList(recyclerViewAdapter.items))
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

        menu.findItem(R.id.menu_add_recording)?.isVisible = isUnlocked && isConnectionToServerAvailable
        menu.findItem(R.id.menu_search)?.isVisible = recyclerViewAdapter.itemCount > 0
        menu.findItem(R.id.media_route_menu_item)?.isVisible = false
    }

    private fun showRecordingDetails(position: Int) {
        seriesRecordingViewModel.selectedListPosition = position
        recyclerViewAdapter.setPosition(position)

        val recording = recyclerViewAdapter.getItem(position)
        if (recording == null || !isVisible) {
            return
        }

        val fm = activity?.supportFragmentManager
        if (!isDualPane) {
            val fragment = SeriesRecordingDetailsFragment.newInstance(recording.id)
            fm?.beginTransaction()?.also {
                it.replace(R.id.main, fragment)
                it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                it.addToBackStack(null)
                it.commit()
            }
        } else {
            var fragment = activity?.supportFragmentManager?.findFragmentById(R.id.details)
            if (fragment !is SeriesRecordingDetailsFragment) {
                fragment = SeriesRecordingDetailsFragment.newInstance(recording.id)
                fm?.beginTransaction()?.also {
                    it.replace(R.id.details, fragment)
                    it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    it.commit()
                }
            } else if (seriesRecordingViewModel.currentId.value != recording.id){
                seriesRecordingViewModel.currentId.value = recording.id
            }
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val ctx = context ?: return
        val seriesRecording = recyclerViewAdapter.getItem(position) ?: return

        val popupMenu = PopupMenu(ctx, view)
        popupMenu.menuInflater.inflate(R.menu.series_recordings_popup_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)

        preparePopupOrToolbarSearchMenu(popupMenu.menu, seriesRecording.title, isConnectionToServerAvailable)
        popupMenu.menu.findItem(R.id.menu_edit_recording)?.isVisible = isUnlocked
        popupMenu.menu.findItem(R.id.menu_disable_recording)?.isVisible = htspVersion >= 19 && isUnlocked && seriesRecording.isEnabled
        popupMenu.menu.findItem(R.id.menu_enable_recording)?.isVisible = htspVersion >= 19 && isUnlocked && !seriesRecording.isEnabled

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit_recording -> return@setOnMenuItemClickListener editSelectedSeriesRecording(ctx, seriesRecording.id)
                R.id.menu_remove_recording -> return@setOnMenuItemClickListener showConfirmationToRemoveSelectedSeriesRecording(ctx, seriesRecording, null)
                R.id.menu_disable_recording -> return@setOnMenuItemClickListener enableSeriesRecording(seriesRecording, false)
                R.id.menu_enable_recording -> return@setOnMenuItemClickListener enableSeriesRecording(seriesRecording, true)

                R.id.menu_search_imdb -> return@setOnMenuItemClickListener searchTitleOnImdbWebsite(ctx, seriesRecording.title)
                R.id.menu_search_fileaffinity -> return@setOnMenuItemClickListener searchTitleOnFileAffinityWebsite(ctx, seriesRecording.title)
                R.id.menu_search_youtube -> return@setOnMenuItemClickListener searchTitleOnYoutube(ctx, seriesRecording.title)
                R.id.menu_search_google -> return@setOnMenuItemClickListener searchTitleOnGoogle(ctx, seriesRecording.title)
                R.id.menu_search_epg -> return@setOnMenuItemClickListener searchTitleInTheLocalDatabase(activity!!, baseViewModel, seriesRecording.title)
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    private fun enableSeriesRecording(seriesRecording: SeriesRecording, enabled: Boolean): Boolean {
        val intent = seriesRecordingViewModel.getIntentData(seriesRecording)
        intent.action = "updateAutorecEntry"
        intent.putExtra("id", seriesRecording.id)
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
        search_progress?.gone()
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
        return getString(R.string.search_series_recordings)
    }
}
