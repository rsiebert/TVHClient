package org.tvheadend.tvhclient.ui.recordings.recordings;

import android.os.Bundle;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.service.HTSListener;

import java.util.Map;

public class RemovedRecordingListFragment extends RecordingListFragment implements HTSListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbarInterface.setTitle(getString(R.string.removed_recordings));
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);

        if (!DataStorage.getInstance().isLoading()) {
            populateList();
            // In dual-pane mode the list of programs of the selected
            // channel will be shown additionally in the details view
            if (isDualPane && adapter.getCount() > 0) {
                showRecordingDetails(selectedListPosition);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
    }

    @Override
    protected void populateList() {
        // Clear the list and add the recordings
        adapter.clear();
        Map<Integer, Recording> map = DataStorage.getInstance().getRecordingsFromArray();
        for (Recording recording : map.values()) {
            if (recording.isRemoved()) {
                adapter.add(recording);
            }
        }
        super.populateList();
    }

    @Override
    public void onMessage(String action, final Object obj) {
        switch (action) {
            case "dvrEntryAdd":
            case "dvrEntryUpdate":
            case "dvrEntryDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Recording recording = (Recording) obj;
                        if (recording.isRemoved()) {
                            handleAdapterChanges(action, recording);
                        }
                    }
                });
                break;
        }
    }
}
