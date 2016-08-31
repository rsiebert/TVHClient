package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
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
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.RecordingListAdapter;
import org.tvheadend.tvhclient.intent.DownloadIntent;
import org.tvheadend.tvhclient.intent.PlayIntent;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Recording;

import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class RecordingListFragment extends Fragment implements HTSListener {

    protected static String TAG = RecordingListFragment.class.getSimpleName();

    protected ActionBarActivity activity;
    protected ActionBarInterface actionBarInterface;
    protected FragmentStatusInterface fragmentStatusInterface;
    protected RecordingListAdapter adapter;
    private ListView listView;

    protected boolean isDualPane;

    protected TVHClientApplication app;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // If the view group does not exist, the fragment would not be shown. So
        // we can return anyway.
        if (container == null) {
            return null;
        }

        // Get the passed argument so we know which recording type to display
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

        // This is the default view for the channel list adapter. Other views can be
        // passed to the adapter to show less information. This is used in the
        // program guide where only the channel icon is relevant.
        int adapterLayout = R.layout.recording_list_widget;

        adapter = new RecordingListAdapter(activity, new ArrayList<Recording>(), adapterLayout);
        listView.setAdapter(adapter);

        // Set the listener to show the recording details activity when the user
        // has selected a recording
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Recording rec = adapter.getItem(position);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onListItemSelected(position, rec, TAG);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        final Recording rec = adapter.getSelectedItem();
        switch (item.getItemId()) {
        case R.id.menu_play:
            startActivity(new PlayIntent(activity, rec));
            return true;

        case R.id.menu_download:
            startActivity(new DownloadIntent(activity, rec));
            return true;

        case R.id.menu_add:
            // Create the fragment and show it as a dialog.
            DialogFragment addFragment = RecordingAddFragment.newInstance(null);
            addFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_edit:
            // Create the fragment and show it as a dialog.
            DialogFragment editFragment = RecordingAddFragment.newInstance(null);
            Bundle bundle = new Bundle();
            bundle.putLong(Constants.BUNDLE_RECORDING_ID, adapter.getSelectedItem().id);
            editFragment.setArguments(bundle);
            editFragment.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        case R.id.menu_record_remove:
            if (rec != null && rec.isRecording()) {
                Utils.confirmStopRecording(activity, rec);
            } else if (rec != null && rec.isScheduled()) {
                Utils.confirmCancelRecording(activity, rec);
            } else {
                Utils.confirmRemoveRecording(activity, rec);
            }
            return true;

        case R.id.menu_record_remove_all:
            // Show a confirmation dialog before deleting all recordings
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.record_remove_all)
                    .setMessage(R.string.confirm_remove_all)
                    .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (rec != null && (rec.isRecording() || rec.isScheduled())) {
                                cancelAllRecordings();
                            } else {
                                removeAllRecordings();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // NOP
                        }
                    })
                    .show();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Calls the service to cancel the scheduled recordings. The service is
     * called in a certain interval to prevent too many calls to the interface.
     */
    private void cancelAllRecordings() {

        // Create a copy of all recordings from the adapter. This ensures that
        // we still have the same id's at the correct index in the array when the
        // adapter contents were refreshed.
        final ArrayList<Recording> idList = new ArrayList<>();
        for (int i = 0; i < adapter.getCount(); ++i) {
            idList.add(adapter.getItem(i));
        }

        new Thread() {
            public void run() {
                for (int i = 0; i < idList.size(); ++i) {
                    app.log(TAG, "Cancelling recording id " + idList.get(i).title);
                    Utils.cancelRecording(activity, idList.get(i));
                    try {
                        sleep(Constants.THREAD_SLEEPING_TIME);
                    } catch (InterruptedException e) {
                        app.log(TAG, "Error cancelling all recordings, " + e.getLocalizedMessage());
                    }
                }
            }
        }.start();
    }

    /**
     * Calls the service to remove the scheduled recordings. The service is
     * called in a certain interval to prevent too many calls to the interface.
     */
    private void removeAllRecordings() {

        // Create a copy of all recordings id's from the adapter. This ensures that
        // we still have the same id's at the correct index in the array when the
        // adapter contents were refreshed.
        final ArrayList<String> idList = new ArrayList<>();
        for (int i = 0; i < adapter.getCount(); ++i) {
            idList.add(String.valueOf(adapter.getItem(i).id));
        }

        new Thread() {
            public void run() {
                for (int i = 0; i < idList.size(); ++i) {
                    app.log(TAG, "Removing recording id " + idList.get(i));
                    Utils.removeRecording(activity, idList.get(i), Constants.ACTION_DELETE_DVR_ENTRY, false);
                    try {
                        sleep(Constants.THREAD_SLEEPING_TIME);
                    } catch (InterruptedException e) {
                        app.log(TAG, "Error removing all recordings, " + e.getLocalizedMessage());
                    }
                }
            }
        }.start();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.recording_menu, menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        activity.getMenuInflater().inflate(R.menu.recording_context_menu, menu);

        // Get the selected program from the list where the context menu was opened
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Recording rec = adapter.getItem(info.position);

        // Hide these menus as a default, the required ones will be made visible
        // in the derived classes
        (menu.findItem(R.id.menu_record_remove)).setVisible(false);
        (menu.findItem(R.id.menu_play)).setVisible(false);
        (menu.findItem(R.id.menu_edit)).setVisible(false);
        (menu.findItem(R.id.menu_download)).setVisible(false);

        // These are always visible if the recording exists
        (menu.findItem(R.id.menu_search_epg)).setVisible(rec != null);
        (menu.findItem(R.id.menu_search_imdb)).setVisible(rec != null);

        if (rec != null) {
            if (rec.isRecording()) {
                (menu.findItem(R.id.menu_record_remove)).setTitle(R.string.stop);
            }
            menu.setHeaderTitle(rec.title);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The context menu is triggered for all fragments that are in a
        // fragment pager. Do nothing for invisible fragments.
        if (!getUserVisibleHint()) {
            return super.onContextItemSelected(item);
        }
        // Get the selected program from the list where the context menu was opened
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // Check for a valid adapter size and objects
        if (info == null || adapter == null || adapter.getCount() <= info.position) {
            return super.onContextItemSelected(item);
        }

        final Recording rec = adapter.getItem(info.position);

        switch (item.getItemId()) {
        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, rec.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, rec.title));
            return true;

        case R.id.menu_record_remove:
            if (rec != null && rec.isRecording()) {
                Utils.confirmStopRecording(activity, rec);
            } else if (rec != null && rec.isScheduled()) {
                Utils.confirmCancelRecording(activity, rec);
            } else {
                Utils.confirmRemoveRecording(activity, rec);
            }
            return true;

        case R.id.menu_play:
            startActivity(new PlayIntent(activity, rec));
            return true;

        case R.id.menu_download:
            startActivity(new DownloadIntent(activity, rec));
            return true;

        case R.id.menu_edit:
            // Create the fragment to edit a recording but show it as a dialog.
            DialogFragment df = RecordingAddFragment.newInstance(null);
            Bundle bundle = new Bundle();
            bundle.putLong(Constants.BUNDLE_RECORDING_ID, rec.id);
            df.setArguments(bundle);
            df.show(activity.getSupportFragmentManager(), "dialog");
            return true;

        default:
            return super.onContextItemSelected(item);
        }
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
            case Constants.ACTION_DVR_ADD:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.add((Recording) obj);
                    }
                });
                break;
            case Constants.ACTION_DVR_DELETE:
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
            case Constants.ACTION_DVR_UPDATE:
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
     * @param offset Offset in pixels from the top
     */
    protected void setSelection(int position, int offset) {
        if (listView != null && listView.getCount() > position && position >= 0) {
            listView.setSelectionFromTop(position, offset);
        }
    }

    /**
     * Selects the given position in the adapter so that the item in the list
     * gets selected. When the dual pane mode is active inform the activity that
     * the list item was selected. The activity will then show the details
     * fragment to the selected item in the right side.
     * 
     * @param position Position in the list
     */
    protected void setInitialSelection(int position) {
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
}
