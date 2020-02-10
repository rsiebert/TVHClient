package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.showCompletedRecordingSortOrderSelectionDialog
import timber.log.Timber

class CompletedRecordingListFragment : RecordingListFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Timber.d("Observing recordings")
        recordingViewModel.completedRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            Timber.d("View model returned ${recordings.size} recordings")
            addRecordingsAndUpdateUI(recordings)
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Show the casting icon when finished recordings are available.
        menu.findItem(R.id.media_route_menu_item)?.isVisible = true
        menu.findItem(R.id.menu_recording_sort_order)?.isVisible = true
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_completed_recordings)
    }

    override fun showStatusInToolbar() {
        context?.let {
            if (!baseViewModel.isSearchActive) {
                toolbarInterface.setTitle(getString(R.string.completed_recordings))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setTitle(getString(R.string.search_results))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.completed_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_recording_sort_order -> showCompletedRecordingSortOrderSelectionDialog(ctx)
            else -> super.onOptionsItemSelected(item)
        }
    }
}
