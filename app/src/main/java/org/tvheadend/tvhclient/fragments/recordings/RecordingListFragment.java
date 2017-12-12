package org.tvheadend.tvhclient.fragments.recordings;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.adapter.RecordingListAdapter;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingListFragment extends ListFragment implements HTSListener, FragmentControlInterface, OnItemClickListener, AdapterView.OnItemLongClickListener {

    protected static String TAG = RecordingListFragment.class.getSimpleName();

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected FragmentStatusInterface fragmentStatusInterface;
    protected RecordingListAdapter adapter;
    boolean isUnlocked;
    protected boolean isDualPane;
    private MenuUtils menuUtils;

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

        adapter = new RecordingListAdapter(activity, new ArrayList<>());
        setListAdapter(adapter);
        getListView().setFastScrollEnabled(true);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
    }

    @Override
    public void onDestroy() {
        fragmentStatusInterface = null;
        toolbarInterface = null;
        super.onDestroy();
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
                // Create the fragment and show it as a dialog.
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

    /**
     * This method is part of the HTSListener interface. Whenever a recording
     * was added, updated or removed the adapter gets updated. The updating of
     * the view itself with the contents is done in the child class, because
     * this parent class has no access to the methods of the child class.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        switch (action) {
            case "dvrEntryAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.add((Recording) obj);
                    }
                });
                break;
            case "dvrEntryDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        // Get the position of the recording that is shown before
                        // the one that has been deleted. This recording will then
                        // be selected when the list has been updated.
                        int previousPosition = adapter.getPosition((Recording) obj);
                        if (--previousPosition < 0) {
                            previousPosition = 0;
                        }
                        // Remove the recording from the adapter and set the
                        // recording below the deleted one as the newly selected one
                        adapter.remove((Recording) obj);
                        setInitialSelection(previousPosition);
                    }
                });
                break;
            case "dvrEntryUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.update((Recording) obj);
                    }
                });
                break;
        }
    }

    /**
     * Sets the selected item and positions the selection y pixels from the top
     * edge of the ListView.
     *
     * @param position Position in the list
     * @param offset   Offset in pixels from the top
     */
    public void setSelection(int position, int offset) {
        if (getListView().getCount() > position && position >= 0) {
            getListView().setSelectionFromTop(position, offset);
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
    public void reloadData() {
        // NOP
    }

    /**
     * Selects the given position in the adapter so that the item in the list
     * gets selected. When the dual pane mode is active inform the activity that
     * the list item was selected. The activity will then show the details
     * fragment to the selected item in the right side.
     *
     * @param position Position in the list
     */
    public void setInitialSelection(int position) {
        setSelection(position, 0);

        if (adapter != null && adapter.getCount() > position) {
            adapter.setPosition(position);

            if (isDualPane) {
                final Recording recording = adapter.getItem(position);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onListItemSelected(position, recording, TAG);
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Recording rec = adapter.getItem(position);
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListItemSelected(position, rec, TAG);
        }
        adapter.setPosition(position);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        final Recording recording = adapter.getItem(position);
        if (getActivity() == null || recording == null) {
            return true;
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.recording_context_menu, popupMenu.getMenu());

        // Hide menus as a default, the required ones will
        // be made visible depending on the recording status
        (popupMenu.getMenu().findItem(R.id.menu_record_remove)).setVisible(false);
        (popupMenu.getMenu().findItem(R.id.menu_play)).setVisible(false);
        (popupMenu.getMenu().findItem(R.id.menu_edit)).setVisible(false);
        (popupMenu.getMenu().findItem(R.id.menu_download)).setVisible(false);

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

        } else if (recording.isFailed() || recording.isRemoved()) {
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
                    bundle.putInt("dvrId", recording.id);
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
}
