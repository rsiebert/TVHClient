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
package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.PlaybackSelectionActivity;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.RecordingListAdapter;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class RecordingListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    public static String TAG = RecordingListFragment.class.getSimpleName();

    protected Activity activity;
    protected ActionBarInterface actionBarInterface;
    protected FragmentStatusInterface fragmentStatusInterface;
    protected RecordingListAdapter adapter;
    private ListView listView;

    // This is the default view for the channel list adapter. Other views can be
    // passed to the adapter to show less information. This is used in the
    // program guide where only the channel icon is relevant.
    private int adapterLayout = R.layout.recording_list_widget;

    private boolean isDualPane;

    // Time to wait for the thread before the next service call is made when
    // either all recorded or scheduled programs are being removed. 
    private static final int THREAD_SLEEPING_TIME = 2000;

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
            isDualPane  = bundle.getBoolean(Constants.BUNDLE_DUAL_PANE, false);
        }
        
        if (isDualPane) {
            adapterLayout = R.layout.recording_list_widget_dual_pane;
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
        case R.id.menu_record_remove_all:
            // Show a confirmation dialog before deleting all recordings
            new AlertDialog.Builder(activity)
            .setTitle(R.string.menu_record_remove_all)
            .setMessage(getString(R.string.delete_all_recordings))
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    new Thread() {
                        public void run() {
                            for (int i = 0; i < adapter.getCount(); ++i) {
                                Log.i(TAG, "Removing recording " + adapter.getItem(i).title);
                                Utils.removeProgram(activity, adapter.getItem(i));
                                try {
                                    sleep(THREAD_SLEEPING_TIME);
                                } catch (InterruptedException e) {
                                    Log.d(TAG, "Exception while removing all recordings. " + e.getLocalizedMessage());
                                }
                            }
                        }
                    }.start();
                }
            }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // NOP
                }
            }).show();
            return true;

        case R.id.menu_record_cancel_all:
            // Show a confirmation dialog before canceling all recordings
            new AlertDialog.Builder(activity)
            .setTitle(R.string.menu_record_cancel_all)
            .setMessage(getString(R.string.cancel_all_recordings))
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    new Thread() {
                        public void run() {
                            for (int i = 0; i < adapter.getCount(); ++i) {
                                Log.i(TAG, "Canceling program " + adapter.getItem(i).title);
                                Utils.removeProgram(activity, adapter.getItem(i));
                                try {
                                    sleep(THREAD_SLEEPING_TIME);
                                } catch (InterruptedException e) {
                                    Log.d(TAG, "Exception while canceling all scheduled programs. " + e.getLocalizedMessage());
                                }
                            }
                        }
                    }.start();
                }
            }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // NOP
                }
            }).show();
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
        Utils.setRecordingMenuIcons(activity, menu);
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
            pi.putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
            startActivity(pi);
            return true;

        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, rec.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, rec.title));
            return true;

        case R.id.menu_record_remove:
            Utils.confirmRemoveProgram(activity, rec);
            return true;

        case R.id.menu_record_cancel:
            Utils.confirmCancelProgram(activity, rec);
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        Log.d(TAG, "onMessage, action " + action);
        if (action.equals(TVHClientApplication.ACTION_DVR_ADD)) {
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
                    // Get the position of the recording that has been deleted
                    int previousPosition = adapter.getPosition((Recording) obj);
                    adapter.remove((Recording) obj);
                    adapter.notifyDataSetChanged();
                    // Set the recording below the deleted one as selected
                    setSelectedItem(previousPosition);
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
        if (adapter != null) {
            return adapter.getCount();
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
        if (listView.getCount() > position && adapter.getCount() > position && position >= 0) {
            adapter.setPosition(position);
            fragmentStatusInterface.onListItemSelected(position, adapter.getItem(position), TAG);
        }
    }

    @Override
    public void reloadData() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSelection(int position) {
        if (listView != null && listView.getCount() > position && position >= 0) {
            listView.setSelection(position);
        }
    }

    @Override
    public void setSelectionFromTop(int position, int index) {
        if (listView != null && listView.getCount() > position && position >= 0) {
            listView.setSelectionFromTop(position, index);
        }
    }
    
    @Override
    public void setInitialSelection(int position) {
        setSelection(position);

        // Set the position in the adapter so that we can show the selected
        // recording in the theme with the arrow.
        if (adapter != null && adapter.getCount() > position) {
            adapter.setPosition(position);
            
            // Simulate a click in the list item to inform the activity
            Recording recording = (Recording) adapter.getItem(position);
            if (fragmentStatusInterface != null) {
                fragmentStatusInterface.onListItemSelected(position, recording, TAG);
            }
        }
    }
}
