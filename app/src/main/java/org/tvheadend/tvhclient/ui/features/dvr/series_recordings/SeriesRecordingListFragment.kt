package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.app.SearchManager
import android.os.Bundle
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.SeriesRecording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import java.util.concurrent.CopyOnWriteArrayList

class SeriesRecordingListFragment : BaseFragment(), RecyclerViewClickCallback, SearchRequestInterface, Filter.FilterListener {

    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel
    private var selectedListPosition: Int = 0
    private lateinit var recyclerViewAdapter: SeriesRecordingRecyclerViewAdapter
    private var searchQuery: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        seriesRecordingViewModel = ViewModelProviders.of(activity!!).get(SeriesRecordingViewModel::class.java)

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("listPosition", 0)
            searchQuery = savedInstanceState.getString(SearchManager.QUERY) ?: ""
        } else {
            selectedListPosition = 0
            searchQuery = arguments?.getString(SearchManager.QUERY) ?: ""
        }

        toolbarInterface.setTitle(if (searchQuery.isEmpty())
            getString(R.string.series_recordings)
        else
            getString(R.string.search_results))

        recyclerViewAdapter = SeriesRecordingRecyclerViewAdapter(isDualPane, this, htspVersion)
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.adapter = recyclerViewAdapter
        recycler_view.gone()

        seriesRecordingViewModel.recordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
            }
            recycler_view?.visible()

            if (searchQuery.isEmpty()) {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.series_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }

            if (isDualPane && recyclerViewAdapter.itemCount > 0) {
                showRecordingDetails(selectedListPosition)
            }
            // Invalidate the menu so that the search menu item is shown in
            // case the adapter contains items now.
            activity?.invalidateOptionsMenu()
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("listPosition", selectedListPosition)
        outState.putString(SearchManager.QUERY, searchQuery)
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
        selectedListPosition = position
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
            // Check what fragment is currently shown, replace if needed.
            var fragment = activity?.supportFragmentManager?.findFragmentById(R.id.details)
            if (fragment !is SeriesRecordingDetailsFragment || fragment.shownId == recording.id) {
                // Make new fragment to show this selection.
                fragment = SeriesRecordingDetailsFragment.newInstance(recording.id)
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
                R.id.menu_search_epg -> return@setOnMenuItemClickListener searchTitleInTheLocalDatabase(ctx, seriesRecording.title)
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
        context?.let {
            if (searchQuery.isEmpty()) {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.series_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun onSearchRequested(query: String) {
        searchQuery = query
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
        return getString(R.string.search_series_recordings)
    }
}
