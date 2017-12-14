package org.tvheadend.tvhclient.fragments.recordings;

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
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.adapter.RecordingListAdapter;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingListFragment extends ListFragment implements OnItemClickListener, AdapterView.OnItemLongClickListener {

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected RecordingListAdapter adapter;
    boolean isUnlocked;
    protected boolean isDualPane;
    private MenuUtils menuUtils;
    protected int selectedListPosition;
    protected SharedPreferences sharedPreferences;

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
        View detailsFrame = getActivity().findViewById(R.id.right_fragment);
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
        final Recording rec = adapter.getSelectedItem();
        switch (item.getItemId()) {
            case R.id.menu_play:
                menuUtils.handleMenuPlaySelection(-1, rec.id);
                return true;

            case R.id.menu_download:
                menuUtils.handleMenuDownloadSelection(rec.id);
                return true;

            case R.id.menu_add:
                DialogFragment addFragment = RecordingAddFragment.newInstance();
                addFragment.show(activity.getSupportFragmentManager(), "dialog");
                return true;

            case R.id.menu_edit:
                // Create the fragment and show it as a dialog.
                DialogFragment editFragment = RecordingAddFragment.newInstance();
                Bundle bundle = new Bundle();
                bundle.putInt("dvrId", adapter.getSelectedItem().id);
                editFragment.setArguments(bundle);
                editFragment.show(activity.getSupportFragmentManager(), "dialog");
                return true;

            case R.id.menu_record_remove:
                if (rec != null) {
                    if (rec.isRecording()) {
                        menuUtils.handleMenuStopRecordingSelection(rec.id, rec.title);
                    } else if (rec.isScheduled()) {
                        menuUtils.handleMenuCancelRecordingSelection(rec.id, rec.title);
                    } else {
                        menuUtils.handleMenuRemoveRecordingSelection(rec.id, rec.title);
                    }
                }
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
        inflater.inflate(R.menu.recording_menu, menu);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        showRecordingDetails(position);
    }

    protected void showRecordingDetails(int position) {
        selectedListPosition = position;
        Recording recording = adapter.getItem(position);

        if (!isDualPane) {
            // Launch a new activity to display the program list of the selected channel.
            /*Intent intent = new Intent(getActivity(), ProgramDetailsActivity.class);
            intent.putExtra("event_id", eventId);
            getActivity().startActivity(intent);*/
        } else {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the program details fragment.
            getListView().setItemChecked(position, true);

            // Check what fragment is currently shown, replace if needed.
            RecordingDetailsFragment recordingDetailsFragment = (RecordingDetailsFragment) getFragmentManager().findFragmentById(R.id.right_fragment);
            if (recordingDetailsFragment == null || recordingDetailsFragment.getShownDvrId() != recording.id) {
                // Make new fragment to show this selection.
                recordingDetailsFragment = RecordingDetailsFragment.newInstance(recording.id);

                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.right_fragment, recordingDetailsFragment);
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
        popupMenu.getMenuInflater().inflate(R.menu.recording_context_menu, popupMenu.getMenu());

        if (recording.isCompleted()) {
            popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
            popupMenu.getMenu().findItem(R.id.menu_play).setVisible(true);
            popupMenu.getMenu().findItem(R.id.menu_download).setVisible(isUnlocked);

        } else if (recording.isScheduled() && !recording.isRecording()) {
            popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
            popupMenu.getMenu().findItem(R.id.menu_edit).setVisible(isUnlocked);

        } else if (recording.isRecording()) {
            popupMenu.getMenu().findItem(R.id.menu_record_remove).setTitle(R.string.stop);
            popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
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
                case R.id.menu_record_remove:
                    if (recording.isRecording()) {
                        menuUtils.handleMenuStopRecordingSelection(recording.id, recording.title);
                    } else if (recording.isScheduled()) {
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
                    // Create the fragment to edit a recording but show it as a dialog.
                    DialogFragment df = RecordingAddFragment.newInstance();
                    Bundle bundle = new Bundle();
                    bundle.putInt("dvrType", recording.id);
                    df.setArguments(bundle);
                    df.show(activity.getSupportFragmentManager(), "dialog");
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
        toolbarInterface.setActionBarSubtitle(items);
    }

    protected void handleAdapterChanges(String action, Recording recording) {
        switch (action) {
            case "dvrEntryAdd":
                adapter.add(recording);
                adapter.notifyDataSetChanged();
                break;
            case "dvrEntryUpdate":
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
                break;
            case "dvrEntryDelete":
                adapter.remove(recording);
                adapter.notifyDataSetChanged();
                break;
        }
    }
}
