package org.tvheadend.tvhclient.fragments;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.model.Recording;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Menu;

public class FailedRecordingListFragment extends RecordingListFragment {

    /**
     * Sets the correct tag. This is required for logging and especially for the
     * main activity so it knows what action shall be executed depending on the
     * recording fragment type.
     */
    public FailedRecordingListFragment() {
        TAG = FailedRecordingListFragment.class.getSimpleName();
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

    /**
     * 
     * @param menu
     */
    private void onPrepareToolbarMenu(Menu menu) {
        // Do not show the remove and play menu in single or dual pane mode. No
        // recording is preselected so the behavior is undefined. In dual pane
        // mode these menus are handled by the recording details details fragment.
        (menu.findItem(R.id.menu_record_remove)).setVisible(false);
        (menu.findItem(R.id.menu_play)).setVisible(false);
        (menu.findItem(R.id.menu_add)).setVisible(false);
        (menu.findItem(R.id.menu_record_cancel)).setVisible(false);
        (menu.findItem(R.id.menu_record_cancel_all)).setVisible(false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean("hideMenuDeleteAllRecordingsPref", false) || adapter.getCount() == 0) {
            (menu.findItem(R.id.menu_record_remove_all)).setVisible(false);
        } else {
            (menu.findItem(R.id.menu_record_remove_all)).setVisible(true);
        }
    }

    /**
     * Fills the list with the available recordings. Only the recordings that
     * are failed are added to the list.
     */
    private void populateList() {
        // Clear the list and add the recordings
        adapter.clear();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        for (Recording rec : app.getRecordingsByType(Constants.RECORDING_TYPE_FAILED)) {
            adapter.add(rec);
        }
        // Show the newest failed recordings first
        adapter.sort(Constants.RECORDING_SORT_ASCENDING);
        adapter.notifyDataSetChanged();
        
        // Shows the currently visible number of failed recordings
        if (toolbar != null) {
            onPrepareToolbarMenu(toolbar.getMenu());
            toolbar.setTitle(getString(R.string.failed_recordings));
            if (adapter.getCount() > 0) {
                toolbar.setSubtitle(adapter.getCount() + " " + getString(R.string.items_available));
            } else {
                toolbar.setSubtitle(R.string.no_recordings_available);
            }
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
        } else if (action.equals(Constants.ACTION_DVR_ADD) 
                || action.equals(Constants.ACTION_DVR_DELETE)
                || action.equals(Constants.ACTION_DVR_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    populateList();
                }
            });
        }
    }
}
