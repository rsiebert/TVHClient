package org.tvheadend.tvhclient.fragments;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.model.Recording;

public class FailedRecordingListFragment extends RecordingListFragment implements FragmentControlInterface {

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
        app.addListener(this);
        if (!app.isLoading()) {
            populateList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Do not show this menu in single mode. No recording is
        // preselected which could be removed.
        if (!isDualPane || adapter.getCount() == 0) {
            (menu.findItem(R.id.menu_record_remove)).setVisible(false);
        }

        (menu.findItem(R.id.menu_play)).setVisible(false);
        (menu.findItem(R.id.menu_add)).setVisible(false);
        (menu.findItem(R.id.menu_edit)).setVisible(false);
        (menu.findItem(R.id.menu_download)).setVisible(false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean("hideMenuDeleteAllRecordingsPref", false) || adapter.getCount() <= 1) {
            (menu.findItem(R.id.menu_record_remove_all)).setVisible(false);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Get the selected program from the list where the context menu was opened
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Recording rec = adapter.getItem(info.position);
        if (rec != null && (rec.error != null || rec.state.equals("missed"))) {
            (menu.findItem(R.id.menu_record_remove)).setVisible(true);
        }
    }

    /**
     * Fills the list with the available recordings. Only the recordings that
     * are failed are added to the list.
     */
    private void populateList() {
        // Clear the list and add the recordings
        adapter.clear();
        for (Recording rec : app.getRecordingsByType(Constants.RECORDING_TYPE_FAILED)) {
            adapter.add(rec);
        }

        // Show the newest failed recordings first
        adapter.sort(Constants.RECORDING_SORT_ASCENDING);
        adapter.notifyDataSetChanged();

        // Shows the currently visible number of recordings of the type  
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.failed_recordings));
            String items = getResources().getQuantityString(R.plurals.recordings, adapter.getCount(), adapter.getCount());
            actionBarInterface.setActionBarSubtitle(items);
            actionBarInterface.setActionBarIcon(R.mipmap.ic_launcher);
        }

        // Inform the activity that the channel list has been populated. It will
        // then select a list item if dual pane mode is active.
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
    }
    
    /**
     * This method is part of the HTSListener interface. Whenever a recording
     * was added, updated or removed the view with the recordings will be
     * refreshed. The adding, updating and removing of the recordings in the
     * adapter itself is done in the parent class because the parent class has
     * no access to the methods of the child class.
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

    @Override
    public void reloadData() {
        // NOP
    }

    @Override
    public void setSelection(int position, int index) {
        super.setSelection(position, index);
    }
    
    @Override
    public void setInitialSelection(int position) {
        super.setInitialSelection(position);
    }

    @Override
    public Object getSelectedItem() {
        return adapter.getSelectedItem();
    }

    @Override
    public int getItemCount() {
        return adapter.getCount();
    }
}
