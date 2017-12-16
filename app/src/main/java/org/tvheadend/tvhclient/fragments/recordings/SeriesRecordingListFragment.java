package org.tvheadend.tvhclient.fragments.recordings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
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
import org.tvheadend.tvhclient.activities.DetailsActivity;
import org.tvheadend.tvhclient.activities.RecordingAddEditActivity;
import org.tvheadend.tvhclient.adapter.SeriesRecordingListAdapter;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class SeriesRecordingListFragment extends ListFragment implements HTSListener, OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = SeriesRecordingListFragment.class.getSimpleName();

    private AppCompatActivity activity;
    private ToolbarInterface toolbarInterface;
    private SeriesRecordingListAdapter adapter;
    private boolean isDualPane;
    private MenuUtils menuUtils;
    private boolean isUnlocked;
    protected int selectedListPosition;
    private SharedPreferences sharedPreferences;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
            toolbarInterface.setActionBarTitle(getString(R.string.series_recordings));
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        menuUtils = new MenuUtils(getActivity());
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();

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

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("list_position", 0);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("list_position", selectedListPosition);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                Intent addIntent = new Intent(getActivity(), RecordingAddEditActivity.class);
                getActivity().startActivity(addIntent);
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
        inflater.inflate(R.menu.recording_list_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!sharedPreferences.getBoolean("hideMenuDeleteAllRecordingsPref", false) && adapter.getCount() > 1) {
            menu.findItem(R.id.menu_record_remove_all).setVisible(true);
        }
        menu.findItem(R.id.menu_add).setVisible(isUnlocked);
    }

    private void populateList() {
        // Clear the list and add the recordings
        adapter.clear();
        adapter.addAll(DataStorage.getInstance().getSeriesRecordingsFromArray().values());
        // Show the newest recordings first
        adapter.sort(Constants.RECORDING_SORT_DESCENDING);
        adapter.notifyDataSetChanged();
        // Show the number of recordings
        String items = getResources().getQuantityString(R.plurals.recordings, adapter.getCount(), adapter.getCount());
        toolbarInterface.setActionBarSubtitle(items);
    }

    @Override
    public void onMessage(String action, final Object obj) {
        switch (action) {
            case Constants.ACTION_LOADING:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        boolean loading = (Boolean) obj;
                        setListShown(!loading);
                        if (!loading) {
                            populateList();
                        }
                    }
                });
                break;
            case "autorecEntryAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        SeriesRecording recording = (SeriesRecording) obj;
                        adapter.add(recording);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
            case "autorecEntryDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        SeriesRecording recording = (SeriesRecording) obj;
                        // Get the position of the recording that is to be
                        // deleted so the previous one can be selected
                        if (--selectedListPosition < 0) {
                            selectedListPosition = 0;
                        }
                        adapter.remove(recording);
                        adapter.notifyDataSetChanged();
                        // Update the number of recordings
                        String items = getResources().getQuantityString(R.plurals.recordings, adapter.getCount(), adapter.getCount());
                        toolbarInterface.setActionBarSubtitle(items);
                        // Select the previous recording to show its details
                        if (isDualPane) {
                            showRecordingDetails(selectedListPosition);
                        }
                    }
                });
                break;
            case "autorecEntryUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        SeriesRecording recording = (SeriesRecording) obj;
                        adapter.update(recording);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        showRecordingDetails(position);
    }

    protected void showRecordingDetails(int position) {
        selectedListPosition = position;
        SeriesRecording recording = adapter.getItem(position);
        if (recording == null) {
            return;
        }
        if (!isDualPane) {
            // Launch a new activity to display the program list of the selected channel.
            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            intent.putExtra("id", recording.id);
            intent.putExtra("type", "series_recording");
            activity.startActivity(intent);
        } else {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the program details fragment.
            getListView().setItemChecked(position, true);
            // Check what fragment is currently shown, replace if needed.
            SeriesRecordingDetailsFragment recordingDetailsFragment = (SeriesRecordingDetailsFragment) getFragmentManager().findFragmentById(R.id.right_fragment);
            if (recordingDetailsFragment == null || !recordingDetailsFragment.getShownId().equals(recording.id)) {
                // Make new fragment to show this selection.
                recordingDetailsFragment = SeriesRecordingDetailsFragment.newInstance(recording.id);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.right_fragment, recordingDetailsFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        final SeriesRecording seriesRecording = adapter.getItem(position);
        if (getActivity() == null || seriesRecording == null) {
            return true;
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.series_recording_context_menu, popupMenu.getMenu());
        (popupMenu.getMenu().findItem(R.id.menu_edit)).setVisible(isUnlocked);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_edit:
                    // Create the fragment and show it as a dialog.
                    DialogFragment editFragment = SeriesRecordingAddFragment.newInstance();
                    Bundle bundle = new Bundle();
                    bundle.putString("id", seriesRecording.id);
                    editFragment.setArguments(bundle);
                    editFragment.show(activity.getSupportFragmentManager(), "dialog");
                    return true;
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchWebSelection(seriesRecording.title);
                    return true;
                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(seriesRecording.title);
                    return true;
                case R.id.menu_record_remove:
                    menuUtils.handleMenuRemoveSeriesRecordingSelection(seriesRecording.id, seriesRecording.title);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
        return true;
    }
}
