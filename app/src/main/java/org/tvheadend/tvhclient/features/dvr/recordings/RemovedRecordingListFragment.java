package org.tvheadend.tvhclient.features.dvr.recordings;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.view.View;

import org.tvheadend.tvhclient.R;

public class RemovedRecordingListFragment extends RecordingListFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbarInterface.setTitle(getString(R.string.removed_recordings));

        RecordingViewModel viewModel = ViewModelProviders.of(activity).get(RecordingViewModel.class);
        viewModel.getRemovedRecordings().observe(this, recordings -> {
            recyclerViewAdapter.addItems(recordings);
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));

            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
                showRecordingDetails(selectedListPosition);
            }
        });
    }
}
