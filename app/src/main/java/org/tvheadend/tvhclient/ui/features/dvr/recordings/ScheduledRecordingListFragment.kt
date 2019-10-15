package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.Observer
import org.tvheadend.tvhclient.R

class ScheduledRecordingListFragment : RecordingListFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(if (recordingViewModel.searchQuery.isEmpty())
            getString(R.string.scheduled_recordings)
        else
            getString(R.string.search_results))

        recordingViewModel.scheduledRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
            }
            updateUI(R.plurals.upcoming_recordings)
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_add_recording)?.isVisible = isUnlocked
    }

    override fun onFilterComplete(i: Int) {
        context?.let {
            if (recordingViewModel.searchQuery.isEmpty()) {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.upcoming_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_scheduled_recordings)
    }
}
