package org.tvheadend.tvhclient.ui.recordings.recordings;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.service.HTSListener;

import java.util.Map;

public class FailedRecordingListFragment extends RecordingListFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbarInterface.setTitle(getString(R.string.failed_recordings));

        RecordingViewModel viewModel = ViewModelProviders.of(this).get(RecordingViewModel.class);
        viewModel.getFailedRecordings().observe(this, recordings -> {
            recyclerViewAdapter.addItems(recordings);
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));

            if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
                showRecordingDetails(selectedListPosition);
            }
        });
    }
}
