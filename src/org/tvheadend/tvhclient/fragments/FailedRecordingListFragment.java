package org.tvheadend.tvhclient.fragments;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.model.Recording;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class FailedRecordingListFragment extends RecordingListFragment implements HTSListener {

    public FailedRecordingListFragment() {
        TAG = FailedRecordingListFragment.class.getSimpleName();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.recording_menu, menu);

        // Only show the cancel all recordings menu if the correct tab is
        // selected and recordings are available that can be canceled.
        MenuItem menuItem = menu.findItem(R.id.menu_record_cancel_all);
        if (menuItem != null) {
            menuItem.setVisible(false);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        setLoading(app.isLoading());
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    /**
     * Show that either no connection (and no data) is available, the data is
     * loaded or calls the method to display it.
     * 
     * @param loading
     */
    private void setLoading(boolean loading) {

        if (loading) {
            adapter.clear();
            adapter.notifyDataSetChanged();
            if (actionBarInterface != null) {
                actionBarInterface.setActionBarSubtitle(getString(R.string.loading), TAG);
            }
        } else {
            populateList();
        }
    }

    /**
     * Fills the list with the available recordings.
     */
    private void populateList() {
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        adapter.clear();
        
        // Show only the recordings that belong to the tab
        for (Recording rec : app.getRecordings()) {
            if ((rec.error != null || (rec.state.equals("missed") || rec.state.equals("invalid")))) {
                adapter.add(rec);
            }
        }
        adapter.sort();
        adapter.notifyDataSetChanged();
        
        // Shows the currently visible number of recordings of the type  
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.recordings), TAG);
            actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.failed), TAG);
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
        Log.d(TAG, "onMessage, action " + action);
        if (action.equals(TVHClientApplication.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    setLoading(loading);
                }
            });
        }
    }
}
