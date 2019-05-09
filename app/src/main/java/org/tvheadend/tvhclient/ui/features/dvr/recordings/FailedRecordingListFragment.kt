package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import androidx.lifecycle.Observer
import org.tvheadend.tvhclient.R

class FailedRecordingListFragment : RecordingListFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(if (searchQuery.isEmpty())
            getString(R.string.failed_recordings)
        else
            getString(R.string.search_results))

        viewModel.failedRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
            }
            updateUI(R.plurals.failed_recordings)
        })
    }

    override fun onFilterComplete(i: Int) {
        context?.let {
            if (searchQuery.isEmpty()) {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.failed_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_failed_recordings)
    }
}
