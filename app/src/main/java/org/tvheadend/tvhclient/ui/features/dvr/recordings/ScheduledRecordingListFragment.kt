package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.Observer
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Recording

class ScheduledRecordingListFragment : RecordingListFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(if (searchQuery.isEmpty())
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
        recordingViewModel.scheduledRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            this.handleObservedRecordings(recordings.toMutableList())
        })
    }

    private fun handleObservedRecordings(recordings: MutableList<Recording>) {
        // Remove all recordings from the list that are duplicated
        if (sharedPreferences.getBoolean("hide_duplicate_scheduled_recordings_enabled", resources.getBoolean(R.bool.pref_default_hide_duplicate_scheduled_recordings_enabled))) {
            recordings.removeAll { it.duplicate == 1 }
        }
        recyclerViewAdapter.addItems(recordings)
        updateUI(R.plurals.upcoming_recordings)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_add)?.isVisible = isUnlocked
    }

    override fun onFilterComplete(i: Int) {
        context?.let {
            if (searchQuery.isEmpty()) {
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
