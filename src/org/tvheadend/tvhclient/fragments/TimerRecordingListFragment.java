package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.TimerRecordingListAdapter;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.TimerRecording;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

@SuppressWarnings("deprecation")
public class TimerRecordingListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    public static String TAG = TimerRecordingListFragment.class.getSimpleName();

    protected ActionBarActivity activity;
    protected ActionBarInterface actionBarInterface;
    protected FragmentStatusInterface fragmentStatusInterface;
    protected TimerRecordingListAdapter adapter;
    private ListView listView;

    // This is the default view for the channel list adapter. Other views can be
    // passed to the adapter to show less information. This is used in the
    // program guide where only the channel icon is relevant.
    private int adapterLayout = R.layout.timer_recording_list_widget;

    protected boolean isDualPane;

    private TVHClientApplication app;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // If the view group does not exist, the fragment would not be shown. So
        // we can return anyway.
        if (container == null) {
            return null;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            isDualPane  = bundle.getBoolean(Constants.BUNDLE_DUAL_PANE, false);
        }

        View v = inflater.inflate(R.layout.list_layout, container, false);
        listView = (ListView) v.findViewById(R.id.item_list);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }

        adapter = new TimerRecordingListAdapter(activity, new ArrayList<TimerRecording>(), adapterLayout);
        listView.setAdapter(adapter);

        // Set the listener to show the recording details activity when the user
        // has selected a recording
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TimerRecording trec = (TimerRecording) adapter.getItem(position);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onListItemSelected(position, trec, TAG);
                }
                adapter.setPosition(position);
                adapter.notifyDataSetChanged();
            }
        });

        setHasOptionsMenu(true);
        registerForContextMenu(listView);
    }

    @Override
    public void onDetach() {
        fragmentStatusInterface = null;
        actionBarInterface = null;
        super.onDetach();
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
        // Do not show the remove menu in single mode. No recording
        // is preselected so the behavior would be undefined. In dual pane
        // mode these menus are handled by the recording details details fragment.
        if (!isDualPane || adapter.getCount() == 0) {
            (menu.findItem(R.id.menu_record_remove)).setVisible(false);
            (menu.findItem(R.id.menu_edit)).setVisible(false);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean("hideMenuDeleteAllRecordingsPref", false) || adapter.getCount() == 0) {
            (menu.findItem(R.id.menu_record_remove_all)).setVisible(false);
        }
    }

    /**
     * Fills the list with the available recordings. Only the recordings that
     * are scheduled are added to the list.
     */
    private void populateList() {
        // Clear the list and add the recordings
        adapter.clear();
        for (TimerRecording trec : app.getTimerRecordings()) {
            adapter.add(trec);
        }
        // Show the newest scheduled recordings first 
        adapter.sort(Constants.RECORDING_SORT_DESCENDING);
        adapter.notifyDataSetChanged();
        
        // Shows the currently visible number of recordings of the type  
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.timer_recordings));
            String items = getResources().getQuantityString(R.plurals.items, adapter.getCount(), adapter.getCount());
            actionBarInterface.setActionBarSubtitle(items);
            actionBarInterface.setActionBarIcon(R.drawable.ic_launcher);
        }
        // Inform the listeners that the channel list is populated.
        // They could then define the preselected list item.
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
        activity.supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
            // Create the fragment and show it as a dialog.
            DialogFragment newFragment = TimerRecordingAddFragment.newInstance(null);
            newFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_edit:
            // Create the fragment and show it as a dialog.
            DialogFragment editFragment = TimerRecordingAddFragment.newInstance(null);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.BUNDLE_TIMER_RECORDING_ID, adapter.getSelectedItem().id);
            editFragment.setArguments(bundle);
            editFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_record_remove:
            Utils.confirmRemoveRecording(activity, adapter.getSelectedItem());
            return true;

        case R.id.menu_record_remove_all:
            // Show a confirmation dialog before deleting all recordings
            new MaterialDialog.Builder(activity)
                    .title(R.string.record_remove_all)
                    .content(R.string.remove_all_recordings)
                    .positiveText(getString(R.string.delete))
                    .negativeText(getString(R.string.cancel))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            removeAllRecordings();
                        }
                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            // NOP
                        }
                    }).show();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Calls the service to remove the timer recordings. The service is
     * called in a certain interval to prevent too many calls to the interface.
     */
    private void removeAllRecordings() {
        new Thread() {
            public void run() {
                for (int i = 0; i < adapter.getCount(); ++i) {
                    Utils.removeRecording(activity, adapter.getItem(i).id,
                            Constants.ACTION_DELETE_TIMER_REC_ENTRY, false);
                    try {
                        sleep(Constants.THREAD_SLEEPING_TIME);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Error removing all timer recordings, " + e.getLocalizedMessage());
                    }
                }
            }
        }.start();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.timer_recording_menu, menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        activity.getMenuInflater().inflate(R.menu.timer_recording_context_menu, menu);

        // Get the currently selected program from the list where the context
        // menu has been triggered
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        TimerRecording srec = adapter.getItem(info.position);
        menu.setHeaderTitle(srec.title);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The context menu is triggered for all fragments that are in a
        // fragment pager. Do nothing for invisible fragments.
        if (!getUserVisibleHint()) {
            return super.onContextItemSelected(item);
        }
        // Get the currently selected program from the list where the context
        // menu has been triggered
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // Check for a valid adapter size and objects
        if (info == null || adapter == null || adapter.getCount() <= info.position) {
            return super.onContextItemSelected(item);
        }

        final TimerRecording trec = adapter.getItem(info.position);

        switch (item.getItemId()) {
        case R.id.menu_edit:
            // Create the fragment and show it as a dialog.
            DialogFragment editFragment = TimerRecordingAddFragment.newInstance(null);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.BUNDLE_TIMER_RECORDING_ID, trec.id);
            editFragment.setArguments(bundle);
            editFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, trec.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, trec.title));
            return true;

        case R.id.menu_record_remove:
            Utils.confirmRemoveRecording(activity, trec);
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the specified action will be executed here.
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
        } else if (action.equals(Constants.ACTION_TIMER_DVR_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.add((TimerRecording) obj);
                    populateList();
                }
            });
        } else if (action.equals(Constants.ACTION_TIMER_DVR_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    // Get the position of the recording that is shown before
                    // the one that has been deleted. This recording will then
                    // be selected when the list has been updated.
                    int previousPosition = adapter.getPosition((TimerRecording) obj);
                    if (--previousPosition < 0) {
                        previousPosition = 0;
                    }
                    adapter.remove((TimerRecording) obj);
                    populateList();
                    setInitialSelection(previousPosition);
                }
            });
        } else if (action.equals(Constants.ACTION_TIMER_DVR_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((TimerRecording) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void reloadData() {
        populateList();
    }

    @Override
    public void setSelection(int position, int index) {
        if (listView != null && listView.getCount() > position && position >= 0) {
            listView.setSelectionFromTop(position, index);
        }
    }

    @Override
    public void setInitialSelection(int position) {
        setSelection(position, 0);

        // Set the position in the adapter so that we can show the selected
        // recording in the theme with the arrow.
        if (adapter != null && adapter.getCount() > position) {
            adapter.setPosition(position);

            // Simulate a click in the list item to inform the activity
            // It will then show the details fragment if dual pane is active
            if (isDualPane) {
                TimerRecording trec = (TimerRecording) adapter.getItem(position);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onListItemSelected(position, trec, TAG);
                }
            }
        }
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
