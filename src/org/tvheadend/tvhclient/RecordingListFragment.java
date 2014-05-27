/*
 *  Copyright (C) 2013 Robert Siebert
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.adapter.RecordingListAdapter;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

public class RecordingListFragment extends Fragment implements HTSListener {

    private final static String TAG = RecordingListFragment.class.getSimpleName();

    private Activity activity;
    private ActionBarInterface actionBarInterface;
    private OnRecordingListListener recordingListListener;
    private RecordingListAdapter adapter;
    private ListView listView;
    private List<Recording> recList;
    private int tabIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null) {
            return null;
        }
        View v = inflater.inflate(R.layout.list_layout, container, false);
        listView = (ListView) v.findViewById(R.id.item_list);

        // Get the passed argument so we know which recording type to display
        Bundle bundle = getArguments();
        if (bundle != null) {
            tabIndex = bundle.getInt("tabIndex", 0);
        }
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (activity instanceof OnRecordingListListener) {
            recordingListListener = (OnRecordingListListener) activity;
        }

        recList = new ArrayList<Recording>();
        adapter = new RecordingListAdapter(activity, recList);
        listView.setAdapter(adapter);

        registerForContextMenu(listView);
        // Set the listener to show the recording details activity when the user
        // has selected a recording
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Recording rec = (Recording) adapter.getItem(position);
                if (recordingListListener != null) {
                    recordingListListener.onRecordingSelected(position, rec);
                }
                adapter.setPosition(position);
                adapter.notifyDataSetChanged();
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        setLoading(app.isLoading());
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    @Override
    public void onDetach() {
        actionBarInterface = null;
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.recording_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_search:
            activity.onSearchRequested();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        activity.getMenuInflater().inflate(R.menu.program_context_menu, menu);
        
        // Get the currently selected program from the list
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Recording rec = adapter.getItem(info.position);
        
        // Set the title of the context menu and show or hide 
        // the menu items depending on the recording state
        menu.setHeaderTitle(rec.title);
        Utils.setRecordingMenu(menu, rec);
        Utils.setRecordingMenuIcons(activity, menu, rec);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
     
        // The context menu is triggered for all fragments in a fragment pager.
        // Do nothing if this fragment is not visible.
        if (!getUserVisibleHint()) {
            return super.onContextItemSelected(item);
        }
        // Get the currently selected program from the list
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Recording rec = adapter.getItem(info.position);
        
        switch (item.getItemId()) {
        case R.id.menu_play:
            Intent pi = new Intent(activity, PlaybackSelectionActivity.class);
            pi.putExtra("dvrId", rec.id);
            startActivity(pi);
            return true;

        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, rec.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, rec.title));
            return true;
            
        case R.id.menu_record_remove:
            Utils.removeProgram(activity, rec);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(activity, rec);
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Show that either no connection (and no data) is available, the data is
     * loaded or calls the method to display it.
     * 
     * @param loading
     */
    private void setLoading(boolean loading) {
        if (DatabaseHelper.getInstance() != null && 
                DatabaseHelper.getInstance().getSelectedConnection() == null) {
            // Clear any channels in the list and 
            // show that we have no connection
            adapter.clear();
            adapter.notifyDataSetChanged();
            if (actionBarInterface != null) {
                actionBarInterface.setActionBarSubtitle(getString(R.string.no_connections), TAG);
            }
        } else {
            if (loading) {
                adapter.clear();
                adapter.notifyDataSetChanged();
                if (actionBarInterface != null) {
                    actionBarInterface.setActionBarSubtitle(getString(R.string.loading), TAG);
                }
            } else {
                populateList();
            }
        }
    }

    /**
     * Fills the list with the available recordings.
     */
    private void populateList() {
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        recList.clear();
        
        // Show only the recordings that belong to the tab
        for (Recording rec : app.getRecordings()) {
            if (tabIndex == 0 &&
                    rec.error == null &&
                    rec.state.equals("completed")) {
                recList.add(rec);
            } else if (tabIndex == 1 &&
                    rec.error == null &&
                    (rec.state.equals("scheduled") || 
                     rec.state.equals("recording") ||
                     rec.state.equals("autorec"))) {
                recList.add(rec);
            } else if (tabIndex == 2 &&
                    (rec.error != null ||
                    (rec.state.equals("missed") || rec.state.equals("invalid")))) {
                recList.add(rec);
            }
        }
        adapter.sort();
        adapter.notifyDataSetChanged();
        
        // Inform the listeners that the channel list is populated.
        // They could then define the preselected list item.
        if (recordingListListener != null) {
            recordingListListener.onRecordingListPopulated();
        }

        ((RecordingListTabsActivity)activity).updateTitle(tabIndex, recList.size());
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHClientApplication.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    setLoading(loading);
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_DVR_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.add((Recording) obj);
                    adapter.notifyDataSetChanged();
                    adapter.sort();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_DVR_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.remove((Recording) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_DVR_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Recording) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    public int getRecordingCount() {
        if (recList != null) {
            return recList.size();
        }
        return 0;
    }

    /**
     * Sets the selected item in the list to the desired position. Any listener
     * is then informed that a new recording item has been selected.
     * 
     * @param position
     */
    public void setSelectedItem(int position) {
        if (listView.getCount() > position && adapter.getCount() > position) {
            adapter.setPosition(position);
            recordingListListener.onRecordingSelected(position, adapter.getItem(position));
        }
    }

    public interface OnRecordingListListener {
        public void onRecordingSelected(int position, Recording recording);
        public void onRecordingListPopulated();
    }
}
