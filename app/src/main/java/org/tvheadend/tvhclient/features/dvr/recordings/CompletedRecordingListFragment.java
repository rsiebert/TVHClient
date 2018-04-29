package org.tvheadend.tvhclient.features.dvr.recordings;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.tvheadend.tvhclient.R;

public class CompletedRecordingListFragment extends RecordingListFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbarInterface.setTitle(getString(R.string.completed_recordings));

        RecordingViewModel viewModel = ViewModelProviders.of(activity).get(RecordingViewModel.class);
        viewModel.getCompletedRecordings().observe(this, recordings -> {
            recyclerViewAdapter.addItems(recordings);
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));

            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
                showRecordingDetails(selectedListPosition);
            }
        });
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Show the casting icon when finished recordings are available.
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        if (mediaRouteMenuItem != null && recyclerViewAdapter.getItemCount() > 0) {
            mediaRouteMenuItem.setVisible(true);
        }
    }
}
