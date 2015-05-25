package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.ExternalPlaybackActivity;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.RecordingListAdapter;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
public class RecordingListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    public static String TAG = RecordingListFragment.class.getSimpleName();

    protected ActionBarActivity activity;
    protected ActionBarInterface actionBarInterface;
    protected FragmentStatusInterface fragmentStatusInterface;
    protected RecordingListAdapter adapter;
    private ListView listView;

    // This is the default view for the channel list adapter. Other views can be
    // passed to the adapter to show less information. This is used in the
    // program guide where only the channel icon is relevant.
    private int adapterLayout = R.layout.recording_list_widget;

    protected boolean isDualPane;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't exist because the fragment
        // will not be shown.
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

        adapter = new RecordingListAdapter(activity, new ArrayList<Recording>(), adapterLayout);
        listView.setAdapter(adapter);

        // Set the listener to show the recording details activity when the user
        // has selected a recording
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Recording rec = (Recording) adapter.getItem(position);
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
        switch (item.getItemId()) {
        case R.id.menu_play:
            // Open a new activity that starts playing the selected recording
            Recording rec = adapter.getSelectedItem();
            if (rec != null) {
                Intent intent = new Intent(activity, ExternalPlaybackActivity.class);
                intent.putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
                startActivity(intent);
            }
            return true;

        case R.id.menu_edit:
            // Create the fragment and show it as a dialog.
            DialogFragment editFragment = RecordingEditFragment.newInstance(null);
            Bundle bundle = new Bundle();
            bundle.putLong(Constants.BUNDLE_RECORDING_ID, adapter.getSelectedItem().id);
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
                    .positiveText(getString(android.R.string.yes))
                    .negativeText(getString(android.R.string.no))
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

        case R.id.menu_record_cancel:
            Utils.confirmCancelRecording(activity, adapter.getSelectedItem());
            return true;

        case R.id.menu_record_cancel_all:
            // Show a confirmation dialog before canceling all recordings
            new MaterialDialog.Builder(activity)
                    .title(R.string.record_cancel_all)
                    .content(R.string.cancel_all_recordings)
                    .positiveText(getString(android.R.string.yes))
                    .negativeText(getString(android.R.string.no))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            cancelAllRecordings();
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
     * Calls the service to cancel the scheduled recordings. The service is
     * called in a certain interval to prevent too many calls to the interface.
     */
    private void cancelAllRecordings() {
        new Thread() {
            public void run() {
                for (int i = 0; i < adapter.getCount(); ++i) {
                    Utils.cancelRecording(activity, adapter.getItem(i));
                    try {
                        sleep(Constants.THREAD_SLEEPING_TIME);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Error cancelling all recordings, " + e.getLocalizedMessage());
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
        new Thread() {
            public void run() {
                for (int i = 0; i < adapter.getCount(); ++i) {
                    Utils.removeRecording(activity, String.valueOf(adapter.getItem(i).id), Constants.ACTION_DELETE_DVR_ENTRY);
                    try {
                        sleep(Constants.THREAD_SLEEPING_TIME);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Error removing all recordings, " + e.getLocalizedMessage());
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

        // Get the currently selected program from the list where the context
        // menu has been triggered
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Recording rec = adapter.getItem(info.position);

        // Set the title of the context menu and show or hide 
        // the menu items depending on the recording state
        menu.setHeaderTitle(rec.title);

        // Get the menu items so they can be shown 
        // or hidden depending on the recording state
        MenuItem recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel);
        MenuItem recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove);
        MenuItem playMenuItem = menu.findItem(R.id.menu_play);
        MenuItem editMenuItem = menu.findItem(R.id.menu_edit);
        MenuItem searchMenuItemEpg = menu.findItem(R.id.menu_search_epg);
        MenuItem searchMenuItemImdb = menu.findItem(R.id.menu_search_imdb);

        // Disable these menus as a default
        if (recordCancelMenuItem != null) {
            recordCancelMenuItem.setVisible(false);
        }
        if (recordRemoveMenuItem != null) {
            recordRemoveMenuItem.setVisible(false);
        }
        if (playMenuItem != null) {
            playMenuItem.setVisible(false);
        }
        if (editMenuItem != null) {
            editMenuItem.setVisible(false);
        }
        if (searchMenuItemEpg != null) {
            searchMenuItemEpg.setVisible(false);
        }
        if (searchMenuItemImdb != null) {
            searchMenuItemImdb.setVisible(false);
        }

        // Exit if the recording is not valid
        if (rec != null) {
            // Allow searching the recordings
            if (searchMenuItemEpg != null) {
                searchMenuItemEpg.setVisible(true);
            }
            if (searchMenuItemImdb != null) {
                searchMenuItemImdb.setVisible(true);
            }

            if (rec.error == null && rec.state.equals("completed")) {
                // The recording is available, it can be played and removed
                if (recordRemoveMenuItem != null) {
                    recordRemoveMenuItem.setVisible(true);
                }
                if (playMenuItem != null) {
                    playMenuItem.setVisible(true);
                }

            } else if (rec.isRecording()) {
                // The recording is recording it can be played or cancelled
                if (recordCancelMenuItem != null) {
                    recordCancelMenuItem.setVisible(true);
                }
                if (playMenuItem != null) {
                    playMenuItem.setVisible(true);
                }
                TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                if (editMenuItem != null) {
                    editMenuItem.setVisible(app.isUnlocked());
                }

            } else if (rec.isScheduled()) {
                // The recording is scheduled, it can only be cancelled
                if (recordCancelMenuItem != null) {
                    recordCancelMenuItem.setVisible(true);
                }
                TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                if (editMenuItem != null) {
                    editMenuItem.setVisible(app.isUnlocked());
                }

            } else if (rec.error != null || rec.state.equals("missed")) {
                // The recording has failed or has been missed, allow removal
                if (recordRemoveMenuItem != null) {
                    recordRemoveMenuItem.setVisible(true);
                }
            }
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

        final Recording rec = adapter.getItem(info.position);

        switch (item.getItemId()) {
        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, rec.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, rec.title));
            return true;

        case R.id.menu_record_remove:
            Utils.confirmRemoveRecording(activity, rec);
            return true;

        case R.id.menu_record_cancel:
            Utils.confirmCancelRecording(activity, rec);
            return true;

        case R.id.menu_play:
            Intent intent = new Intent(activity, ExternalPlaybackActivity.class);
            intent.putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
            startActivity(intent);
            return true;

        case R.id.menu_edit:
            // Create the fragment and show it as a dialog.
            DialogFragment editFragment = RecordingEditFragment.newInstance(null);
            Bundle bundle = new Bundle();
            bundle.putLong(Constants.BUNDLE_RECORDING_ID, rec.id);
            editFragment.setArguments(bundle);
            editFragment.show(activity.getSupportFragmentManager(), "dialog");
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
        Log.i(TAG, "onMessage " + action);
        if (action.equals(Constants.ACTION_DVR_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.add((Recording) obj);
                }
            });
        } else if (action.equals(Constants.ACTION_DVR_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    // Get the position of the recording that is shown before
                    // the one that has been deleted. This recording will then
                    // be selected when the list has been updated.
                    int previousPosition = adapter.getPosition((Recording) obj);
                    if (--previousPosition < 0) {
                        previousPosition = 0;
                    }
                    adapter.remove((Recording) obj);
                    // Set the recording below the deleted one as selected
                    setInitialSelection(previousPosition);
                }
            });
        } else if (action.equals(Constants.ACTION_DVR_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Recording) obj);
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
                Recording recording = (Recording) adapter.getItem(position);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onListItemSelected(position, recording, TAG);
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
