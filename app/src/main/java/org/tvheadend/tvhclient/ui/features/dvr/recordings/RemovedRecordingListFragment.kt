package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Filter
import androidx.lifecycle.Observer

import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface

class RemovedRecordingListFragment : RecordingListFragment(), SearchRequestInterface, Filter.FilterListener {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(if (TextUtils.isEmpty(searchQuery))
            getString(R.string.removed_recordings)
        else
            getString(R.string.search_results))

        viewModel.removedRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
            }

            recyclerView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE

            if (TextUtils.isEmpty(searchQuery)) {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.removed_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }

            if (isDualPane && recyclerViewAdapter.itemCount > 0) {
                showRecordingDetails(selectedListPosition)
            }
            // Invalidate the menu so that the search menu item is shown in
            // case the adapter contains items now.
            activity.invalidateOptionsMenu()
        })
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
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.removed_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        }
        // Preselect the first result item in the details screen
        if (isDualPane && recyclerViewAdapter.itemCount > 0) {
            showRecordingDetails(0)
        }
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_removed_recordings)
    }
}
