package org.tvheadend.tvhclient.fragments.recordings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.adapter.SeriesRecordingListAdapter;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class SeriesRecordingListFragment extends ListFragment implements HTSListener, FragmentControlInterface, OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = SeriesRecordingListFragment.class.getSimpleName();

    private AppCompatActivity activity;
    private ToolbarInterface toolbarInterface;
    private FragmentStatusInterface fragmentStatusInterface;
    private SeriesRecordingListAdapter adapter;
    private boolean isDualPane;
    private MenuUtils menuUtils;
    private boolean isUnlocked;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        menuUtils = new MenuUtils(getActivity());

        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = getActivity().findViewById(R.id.right_fragment);
        isDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        adapter = new SeriesRecordingListAdapter(activity, new ArrayList<SeriesRecording>());
        setListAdapter(adapter);
        getListView().setFastScrollEnabled(true);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        setHasOptionsMenu(true);
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
        TVHClientApplication.getInstance().addListener(this);
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        if (!DataStorage.getInstance().isLoading()) {
            populateList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Do not show the remove menu in single mode. No recording
        // is preselected so the behavior would be undefined. In dual pane
        // mode these menus are handled by the recording details details fragment.
        if (!isDualPane || adapter.getCount() == 0) {
            (menu.findItem(R.id.menu_record_remove)).setVisible(false);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean("hideMenuDeleteAllRecordingsPref", false) || adapter.getCount() <= 1) {
            (menu.findItem(R.id.menu_record_remove_all)).setVisible(false);
        }

        // Show the add button only when the application is unlocked
        (menu.findItem(R.id.menu_add)).setVisible(isUnlocked);

        if (!isDualPane || adapter.getCount() == 0 || !isUnlocked) {
            (menu.findItem(R.id.menu_edit)).setVisible(false);
        }
    }

    /**
     * Fills the list with the available recordings. Only the recordings that
     * are scheduled are added to the list.
     */
    private void populateList() {
        // Clear the list and add the recordings
        adapter.clear();
        Object[] trecList = DataStorage.getInstance().getSeriesRecordingsFromArray().values().toArray();
        for (Object trec : trecList) {
            adapter.add((SeriesRecording) trec);
        }
        // Show the newest scheduled recordings first 
        adapter.sort(Constants.RECORDING_SORT_DESCENDING);
        adapter.notifyDataSetChanged();

        // Shows the currently visible number of recordings of the type  
        if (toolbarInterface != null) {
            toolbarInterface.setActionBarTitle(getString(R.string.series_recordings));
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
            DialogFragment newFragment = SeriesRecordingAddFragment.newInstance();
            newFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_edit:
            // Create the fragment and show it as a dialog.
            DialogFragment editFragment = SeriesRecordingAddFragment.newInstance();
            Bundle bundle = new Bundle();
            bundle.putString("id", adapter.getSelectedItem().id);
            editFragment.setArguments(bundle);
            editFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_record_remove:
            SeriesRecording srec = adapter.getSelectedItem();
            menuUtils.handleMenuRemoveSeriesRecordingSelection(srec.id, srec.title);
            return true;

        case R.id.menu_record_remove_all:
            CopyOnWriteArrayList<SeriesRecording> list = new CopyOnWriteArrayList<>(adapter.getAllItems());
            menuUtils.handleMenuRemoveAllSeriesRecordingSelection(list);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.series_recording_menu, menu);
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
            case "autorecEntryAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.add((SeriesRecording) obj);
                        populateList();
                    }
                });
                break;
            case "autorecEntryDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        // Get the position of the recording that is shown before
                        // the one that has been deleted. This recording will then
                        // be selected when the list has been updated.
                        int previousPosition = adapter.getPosition((SeriesRecording) obj);
                        if (--previousPosition < 0) {
                            previousPosition = 0;
                        }
                        adapter.remove((SeriesRecording) obj);
                        populateList();
                        setInitialSelection(previousPosition);
                    }
                });
                break;
            case "autorecEntryUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.update((SeriesRecording) obj);
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
        if (getListView().getCount() > position && position >= 0) {
            getListView().setSelectionFromTop(position, index);
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
                SeriesRecording srec = adapter.getItem(position);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onListItemSelected(position, srec, TAG);
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SeriesRecording srec = adapter.getItem(position);
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListItemSelected(position, srec, TAG);
        }
        adapter.setPosition(position);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.d(TAG, "onItemLongClick() called with: adapterView = [" + adapterView + "], view = [" + view + "], position = [" + position + "], id = [" + id + "]");
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.series_recording_context_menu, popupMenu.getMenu());
        if (!isUnlocked) {
            (popupMenu.getMenu().findItem(R.id.menu_edit)).setVisible(false);
        }
        final SeriesRecording srec = adapter.getItem(position);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_edit:
                    // Create the fragment and show it as a dialog.
                    DialogFragment editFragment = SeriesRecordingAddFragment.newInstance();
                    Bundle bundle = new Bundle();
                    bundle.putString("id", srec.id);
                    editFragment.setArguments(bundle);
                    editFragment.show(activity.getSupportFragmentManager(), "dialog");
                    return true;
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchWebSelection(srec.title);
                    return true;
                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(srec.title);
                    return true;
                case R.id.menu_record_remove:
                    menuUtils.handleMenuRemoveSeriesRecordingSelection(srec.id, srec.title);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
        return true;
    }
}
