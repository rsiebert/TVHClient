package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.View
import org.tvheadend.tvhclient.R
import timber.log.Timber

class FailedRecordingListFragment : RecordingListFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("Observing recordings")
        recordingViewModel.failedRecordings.observe(viewLifecycleOwner,  { recordings ->
            Timber.d("View model returned ${recordings.size} recordings")
            addRecordingsAndUpdateUI(recordings)
        })
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_failed_recordings)
    }

    override fun showStatusInToolbar() {
        context?.let {
            if (!baseViewModel.isSearchActive) {
                toolbarInterface.setTitle(getString(R.string.failed_recordings))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setTitle(getString(R.string.search_results))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.failed_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }
}
