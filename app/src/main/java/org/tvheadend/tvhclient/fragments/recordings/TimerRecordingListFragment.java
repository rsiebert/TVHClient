package org.tvheadend.tvhclient.fragments.recordings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
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

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.adapter.TimerRecordingListAdapter;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.model.TimerRecording2;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class TimerRecordingListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    private static final String TAG = TimerRecordingListFragment.class.getSimpleName();

    private AppCompatActivity activity;
    private ToolbarInterface toolbarInterface;
    private FragmentStatusInterface fragmentStatusInterface;
    private TimerRecordingListAdapter adapter;
    private ListView listView;
    private boolean isDualPane;

    private TVHClientApplication app;
    private DataStorage dataStorage;
    private MenuUtils menuUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // If the view group does not exist, the fragment would not be shown. So
        // we can return anyway.
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.list_layout, container, false);
        listView = v.findViewById(R.id.item_list);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        app = TVHClientApplication.getInstance();
        dataStorage = DataStorage.getInstance();
        menuUtils = new MenuUtils(getActivity());

        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            isDualPane  = bundle.getBoolean("dual_pane", false);
        }

        adapter = new TimerRecordingListAdapter(activity, new ArrayList<>());
        listView.setAdapter(adapter);

        // Set the listener to show the recording details activity when the user
        // has selected a recording
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TimerRecording2 trec = adapter.getItem(position);
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
    public void onDestroy() {
        fragmentStatusInterface = null;
        toolbarInterface = null;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        app.addListener(this);
        if (!dataStorage.isLoading()) {
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
        if (prefs.getBoolean("hideMenuDeleteAllRecordingsPref", false) || adapter.getCount() <= 1) {
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
        Object[] trecList = dataStorage.getTimerRecordingsFromArray().values().toArray();
        for (Object trec : trecList) {
            adapter.add((TimerRecording2) trec);
        }
        // Show the newest scheduled recordings first 
        adapter.sort(Constants.RECORDING_SORT_DESCENDING);
        adapter.notifyDataSetChanged();
        
        // Shows the currently visible number of recordings of the type  
        if (toolbarInterface != null) {
            toolbarInterface.setActionBarTitle(getString(R.string.timer_recordings));
            String items = getResources().getQuantityString(R.plurals.items, adapter.getCount(), adapter.getCount());
            toolbarInterface.setActionBarSubtitle(items);
            toolbarInterface.setActionBarIcon(R.mipmap.ic_launcher);
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
            DialogFragment newFragment = TimerRecordingAddFragment.newInstance();
            newFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_edit:
            // Create the fragment and show it as a dialog.
            DialogFragment editFragment = TimerRecordingAddFragment.newInstance();
            Bundle bundle = new Bundle();
            bundle.putString("id", adapter.getSelectedItem().id);
            editFragment.setArguments(bundle);
            editFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_record_remove:
            TimerRecording2 trec = adapter.getSelectedItem();
            final String name = (trec.name != null && trec.name.length() > 0) ? trec.name : "";
            final String title = trec.title != null ? trec.title : "";
            menuUtils.handleMenuRemoveTimerRecordingSelection(trec.id, (name.length() > 0 ? name : title));
            return true;

        case R.id.menu_record_remove_all:
            CopyOnWriteArrayList<TimerRecording2> list = new CopyOnWriteArrayList<>(adapter.getAllItems());
            menuUtils.handleMenuRemoveAllTimerRecordingSelection(list);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
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
        TimerRecording2 trec = adapter.getItem(info.position);
        if (trec != null) {
            menu.setHeaderTitle(trec.title);
        }
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

        final TimerRecording2 trec = adapter.getItem(info.position);
        if (trec == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.menu_edit:
            // Create the fragment and show it as a dialog.
            DialogFragment editFragment = TimerRecordingAddFragment.newInstance();
            Bundle bundle = new Bundle();
            bundle.putString("id", trec.id);
            editFragment.setArguments(bundle);
            editFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_search_imdb:
            menuUtils.handleMenuSearchWebSelection(trec.title);
            return true;

        case R.id.menu_search_epg:
            menuUtils.handleMenuSearchEpgSelection(trec.title);
            return true;

        case R.id.menu_record_remove:
            final String name = (trec.name != null && trec.name.length() > 0) ? trec.name : "";
            final String title = trec.title != null ? trec.title : "";
            menuUtils.handleMenuRemoveTimerRecordingSelection(trec.id, (name.length() > 0 ? name : title));
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
        switch (action) {
            case Constants.ACTION_LOADING:
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
                break;
            case "timerecEntryAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.add((TimerRecording2) obj);
                        populateList();
                    }
                });
                break;
            case "timerecEntryDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        // Get the position of the recording that is shown before
                        // the one that has been deleted. This recording will then
                        // be selected when the list has been updated.
                        int previousPosition = adapter.getPosition((TimerRecording2) obj);
                        if (--previousPosition < 0) {
                            previousPosition = 0;
                        }
                        adapter.remove((TimerRecording2) obj);
                        populateList();
                        setInitialSelection(previousPosition);
                    }
                });
                break;
            case "timerecEntryUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.update((TimerRecording2) obj);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
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
                TimerRecording2 trec = adapter.getItem(position);
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
