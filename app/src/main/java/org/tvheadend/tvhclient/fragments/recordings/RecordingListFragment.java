package org.tvheadend.tvhclient.fragments.recordings;

import android.os.Bundle;
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

import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.adapter.RecordingListAdapter;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Recording2;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    protected static String TAG = RecordingListFragment.class.getSimpleName();

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected FragmentStatusInterface fragmentStatusInterface;
    protected RecordingListAdapter adapter;
    private ListView listView;

    protected boolean isDualPane;

    protected TVHClientApplication app;
    private Logger logger;
    protected DataStorage dataStorage;
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
        logger = Logger.getInstance();
        dataStorage = DataStorage.getInstance();
        menuUtils = new MenuUtils(getActivity());

        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }

        // Get the passed argument so we know which recording type to display
        Bundle bundle = getArguments();
        if (bundle != null) {
            isDualPane  = bundle.getBoolean("dual_pane", false);
        }

        // This is the default view for the channel list adapter. Other views can be
        // passed to the adapter to show less information. This is used in the
        // program guide where only the channel icon is relevant.
        int adapterLayout = R.layout.recording_list_widget;

        adapter = new RecordingListAdapter(activity, new ArrayList<Recording2>(), adapterLayout);
        listView.setAdapter(adapter);

        // Set the listener to show the recording details activity when the user
        // has selected a recording
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Recording2 rec = adapter.getItem(position);
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
    public void onDestroy() {
        fragmentStatusInterface = null;
        toolbarInterface = null;
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Recording2 rec = adapter.getSelectedItem();
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
            CopyOnWriteArrayList<Recording2> list = new CopyOnWriteArrayList<>(adapter.getAllItems());
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        activity.getMenuInflater().inflate(R.menu.recording_context_menu, menu);

        // Get the selected program from the list where the context menu was opened
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Recording2 rec = adapter.getItem(info.position);

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

        final Recording2 rec = adapter.getItem(info.position);
        if (rec == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.menu_search_imdb:
            menuUtils.handleMenuSearchWebSelection(rec.title);
            return true;

        case R.id.menu_search_epg:
            menuUtils.handleMenuSearchEpgSelection(rec.title);
            return true;

        case R.id.menu_record_remove:
            if (rec.isRecording()) {
                menuUtils.handleMenuStopRecordingSelection(rec.id, rec.title);
            } else if (rec.isScheduled()) {
                menuUtils.handleMenuCancelRecordingSelection(rec.id, rec.title);
            } else {
                menuUtils.handleMenuRemoveRecordingSelection(rec.id, rec.title);
            }
            return true;

        case R.id.menu_play:
            menuUtils.handleMenuPlaySelection(-1, rec.id);
            return true;

        case R.id.menu_download:
            menuUtils.handleMenuDownloadSelection(rec.id);
            return true;

        case R.id.menu_edit:
            // Create the fragment to edit a recording but show it as a dialog.
            DialogFragment df = RecordingAddFragment.newInstance();
            Bundle bundle = new Bundle();
            bundle.putInt("dvrId", rec.id);
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
            case "dvrEntryAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.add((Recording2) obj);
                    }
                });
                break;
            case "dvrEntryDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        // Get the position of the recording that is shown before
                        // the one that has been deleted. This recording will then
                        // be selected when the list has been updated.
                        int previousPosition = adapter.getPosition((Recording2) obj);
                        if (--previousPosition < 0) {
                            previousPosition = 0;
                        }
                        // Remove the recording from the adapter and set the
                        // recording below the deleted one as the newly selected one
                        adapter.remove((Recording2) obj);
                        setInitialSelection(previousPosition);
                    }
                });
                break;
            case "dvrEntryUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.update((Recording2) obj);
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
    public void setSelection(int position, int offset) {
        if (listView != null && listView.getCount() > position && position >= 0) {
            listView.setSelectionFromTop(position, offset);
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
                final Recording2 recording = adapter.getItem(position);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onListItemSelected(position, recording, TAG);
                }
            }
        }
    }
}
