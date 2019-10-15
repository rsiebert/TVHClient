package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.Observer
import org.tvheadend.tvhclient.R

class CompletedRecordingListFragment : RecordingListFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(if (recordingViewModel.searchQuery.isEmpty())
            getString(R.string.completed_recordings)
        else
            getString(R.string.search_results))

        recordingViewModel.completedRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
            }
            updateUI(R.plurals.completed_recordings)
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Show the casting icon when finished recordings are available.
        menu.findItem(R.id.media_route_menu_item)?.isVisible = true
    }

    override fun onFilterComplete(i: Int) {
        context?.let {
            if (recordingViewModel.searchQuery.isEmpty()) {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.completed_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_completed_recordings)
    }
}
