package org.tvheadend.tvhclient.ui.recordings;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.ui.search.SearchActivity;
import org.tvheadend.tvhclient.ui.search.SearchRequestInterface;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingListFragment extends ListFragment implements OnItemClickListener, AdapterView.OnItemLongClickListener, SearchRequestInterface {

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected RecordingListAdapter adapter;
    boolean isUnlocked;
    protected boolean isDualPane;
    private MenuUtils menuUtils;
    protected int selectedListPosition;
    protected SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_main_list_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        menuUtils = new MenuUtils(getActivity());
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = getActivity().findViewById(R.id.details);
        isDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        adapter = new RecordingListAdapter(activity, new ArrayList<>());
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                Intent intent = new Intent(getActivity(), RecordingAddEditActivity.class);
                intent.putExtra("type", "recording");
                getActivity().startActivity(intent);
                return true;
            case R.id.menu_record_remove_all:
                CopyOnWriteArrayList<Recording> list = new CopyOnWriteArrayList<>(adapter.getAllItems());
                menuUtils.handleMenuRemoveAllRecordingsSelection(list);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_menu_recording_list, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!sharedPreferences.getBoolean("hideMenuDeleteAllRecordingsPref", false) && adapter.getCount() > 1) {
            menu.findItem(R.id.menu_record_remove_all).setVisible(true);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        showRecordingDetails(position);
    }

    protected void showRecordingDetails(int position) {
        selectedListPosition = position;
        Recording recording = adapter.getItem(position);
        if (recording == null) {
            return;
        }
        if (!isDualPane) {
            // Launch a new activity to display the program list of the selected channel.
            Intent intent = new Intent(getActivity(), RecordingDetailsActivity.class);
            intent.putExtra("dvrId", recording.id);
            intent.putExtra("type", "recording");
            activity.startActivity(intent);
        } else {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the program details fragment.
            getListView().setItemChecked(position, true);
            // Check what fragment is currently shown, replace if needed.
            RecordingDetailsFragment recordingDetailsFragment = (RecordingDetailsFragment) getFragmentManager().findFragmentById(R.id.details);
            if (recordingDetailsFragment == null || recordingDetailsFragment.getShownDvrId() != recording.id) {
                // Make new fragment to show this selection.
                recordingDetailsFragment = RecordingDetailsFragment.newInstance(recording.id);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.details, recordingDetailsFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        final Recording recording = adapter.getItem(position);
        if (getActivity() == null || recording == null) {
            return true;
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu_recordings, popupMenu.getMenu());

        if (recording.isCompleted()) {
            popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
            popupMenu.getMenu().findItem(R.id.menu_play).setVisible(true);
            popupMenu.getMenu().findItem(R.id.menu_download).setVisible(isUnlocked);

        } else if (recording.isScheduled() && !recording.isRecording()) {
            popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
            popupMenu.getMenu().findItem(R.id.menu_edit).setVisible(isUnlocked);

        } else if (recording.isRecording()) {
            popupMenu.getMenu().findItem(R.id.menu_record_stop).setVisible(true);
            popupMenu.getMenu().findItem(R.id.menu_play).setVisible(true);
            popupMenu.getMenu().findItem(R.id.menu_edit).setVisible(isUnlocked);

        } else if (recording.isFailed() || recording.isRemoved() || recording.isMissed() || recording.isAborted()) {
            popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchWebSelection(recording.title);
                    return true;
                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(recording.title);
                    return true;
                case R.id.menu_record_stop:
                    menuUtils.handleMenuStopRecordingSelection(recording.id, recording.title);
                    return true;
                case R.id.menu_record_remove:
                    if (recording.isScheduled()) {
                        menuUtils.handleMenuCancelRecordingSelection(recording.id, recording.title);
                    } else {
                        menuUtils.handleMenuRemoveRecordingSelection(recording.id, recording.title);
                    }
                    return true;
                case R.id.menu_play:
                    menuUtils.handleMenuPlaySelection(-1, recording.id);
                    return true;
                case R.id.menu_download:
                    menuUtils.handleMenuDownloadSelection(recording.id);
                    return true;
                case R.id.menu_edit:
                    Intent intent = new Intent(getActivity(), RecordingAddEditActivity.class);
                    intent.putExtra("dvrId", recording.id);
                    intent.putExtra("type", "recording");
                    getActivity().startActivity(intent);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
        return true;
    }

    protected void populateList() {
        // Show the newest recordings first
        adapter.sort(Constants.RECORDING_SORT_DESCENDING);
        adapter.notifyDataSetChanged();
        // Show the number of recordings
        String items = getResources().getQuantityString(R.plurals.recordings, adapter.getCount(), adapter.getCount());
        toolbarInterface.setSubtitle(items);
    }

    protected void handleAdapterChanges(String action, Recording recording) {
        switch (action) {
            case "dvrEntryAdd":
                adapter.add(recording);
                adapter.notifyDataSetChanged();
                break;
            case "dvrEntryDelete":
                // Get the position of the recording that is to be
                // deleted so the previous one can be selected
                if (--selectedListPosition < 0) {
                    selectedListPosition = 0;
                }
                adapter.remove(recording);
                adapter.notifyDataSetChanged();
                // Update the number of recordings
                String items = getResources().getQuantityString(R.plurals.recordings, adapter.getCount(), adapter.getCount());
                toolbarInterface.setSubtitle(items);
                // Select the previous recording to show its details
                if (isDualPane) {
                    showRecordingDetails(selectedListPosition);
                }
                break;
            case "dvrEntryUpdate":
                adapter.update(recording);
                adapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void onSearchRequested(String query) {
        // Start searching for recordings
        Intent searchIntent = new Intent(getActivity(), SearchActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra("type", "recordings");
        startActivity(searchIntent);
    }
}
