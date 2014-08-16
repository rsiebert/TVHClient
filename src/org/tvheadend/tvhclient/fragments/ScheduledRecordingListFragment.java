package org.tvheadend.tvhclient.fragments;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.model.Recording;

import android.view.Menu;

public class ScheduledRecordingListFragment extends RecordingListFragment {

    /**
     * Sets the correct tag. This is required for logging and especially for the
     * main activity so it knows what action shall be executed depending on the
     * recording fragment type.
     */
    public ScheduledRecordingListFragment() {
        TAG = ScheduledRecordingListFragment.class.getSimpleName();
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        if (!app.isLoading()) {
            populateList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Only show the cancel all recordings menu if the correct tab is
        // selected and recordings are available that can be canceled.
        (menu.findItem(R.id.menu_record_remove_all)).setVisible(false);
        // Playing a scheduled recording is not possible 
        (menu.findItem(R.id.menu_play)).setVisible(false);
    }

    /**
     * Fills the list with the available recordings. Only the recordings that
     * are scheduled are added to the list.
     */
    private void populateList() {
        // Clear the list and add the recordings
        adapter.clear();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        for (Recording rec : app.getRecordings()) {
            if (rec.error == null
                    && (rec.state.equals("scheduled") 
                            || rec.state.equals("recording") 
                            || rec.state.equals("autorec"))) {
                adapter.add(rec);
            }
        }
        adapter.sort();
        adapter.notifyDataSetChanged();
        
        // Shows the currently visible number of recordings of the type  
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.recordings), TAG);
            actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.upcoming), TAG);
        }
        // Inform the listeners that the channel list is populated.
        // They could then define the preselected list item.
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
    }
    
    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    if (loading) {
                        adapter.clear();
                        adapter.notifyDataSetChanged();
                    } else {
                        populateList();
                    }
                }
            });
        }
    }
}
