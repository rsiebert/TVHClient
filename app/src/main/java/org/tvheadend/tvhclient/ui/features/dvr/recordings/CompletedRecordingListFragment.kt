package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.gone
import org.tvheadend.tvhclient.ui.common.visible

class CompletedRecordingListFragment : RecordingListFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(if (searchQuery.isEmpty())
            getString(R.string.completed_recordings)
        else
            getString(R.string.search_results))

        viewModel.completedRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
            }

            recycler_view?.visible()
            progress_bar?.gone()

            if (searchQuery.isEmpty()) {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.completed_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }

            if (isDualPane && recyclerViewAdapter.itemCount > 0) {
                showRecordingDetails(selectedListPosition)
            }
            // Invalidate the menu so that the search menu item is shown in
            // case the adapter contains items now.
            activity.invalidateOptionsMenu()
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Show the casting icon when finished recordings are available.
        menu.findItem(R.id.media_route_menu_item)?.isVisible = true
    }

    override fun onFilterComplete(i: Int) {
        context?.let {
            if (searchQuery.isEmpty()) {
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
