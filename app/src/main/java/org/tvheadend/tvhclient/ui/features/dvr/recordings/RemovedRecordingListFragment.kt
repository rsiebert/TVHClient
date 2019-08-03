package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import androidx.lifecycle.Observer
import org.tvheadend.tvhclient.R

class RemovedRecordingListFragment : RecordingListFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(if (searchQuery.isEmpty())
            getString(R.string.removed_recordings)
        else
            getString(R.string.search_results))

        recordingViewModel.removedRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
            }
            updateUI(R.plurals.removed_recordings)
        })
    }

    override fun onFilterComplete(i: Int) {
        context?.let {
            if (searchQuery.isEmpty()) {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.removed_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_removed_recordings)
    }
}
