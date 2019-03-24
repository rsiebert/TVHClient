package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.Filter
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface

class ScheduledRecordingListFragment : RecordingListFragment(), SearchRequestInterface, Filter.FilterListener {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(if (TextUtils.isEmpty(searchQuery))
            getString(R.string.scheduled_recordings)
        else
            getString(R.string.search_results))
    }

    override fun onResume() {
        super.onResume()
        // TODO consider duplicate setting for all recording types
        // Start observing the recordings here because the onActivityCreated method is not
        // called when the user has returned from the settings activity. In this case
        // the changes to the recording UI like hiding duplicates would not become active.
        viewModel.scheduledRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            this.handleObservedRecordings(recordings.toMutableList())
        })
    }

    private fun handleObservedRecordings(recordings: MutableList<Recording>) {
        // Remove all recordings from the list that are duplicated
        if (sharedPreferences.getBoolean("hide_duplicate_scheduled_recordings_enabled", resources.getBoolean(R.bool.pref_default_hide_duplicate_scheduled_recordings_enabled))) {
            for (recording in recordings) {
                if (recording.duplicate == 1) {
                    recordings.remove(recording)
                }
            }
        }
        recyclerViewAdapter.addItems(recordings)

        recycler_view.visibility = View.VISIBLE
        progress_bar.visibility = View.GONE

        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        } else {
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.upcoming_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        }

        if (isDualPane && recyclerViewAdapter.itemCount > 0) {
            showRecordingDetails(selectedListPosition)
        }
        // Invalidate the menu so that the search menu item is shown in
        // case the adapter contains items now.
        activity.invalidateOptionsMenu()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_add)?.isVisible = isUnlocked
    }

    override fun onSearchRequested(query: String) {
        searchQuery = query
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

    override fun onFilterComplete(i: Int) {
        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        } else {
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.upcoming_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        }
        // Preselect the first result item in the details screen
        if (isDualPane && recyclerViewAdapter.itemCount > 0) {
            showRecordingDetails(0)
        }
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_scheduled_recordings)
    }
}
