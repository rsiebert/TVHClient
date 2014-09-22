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
import java.util.Iterator;
import java.util.List;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.PlaybackSelectionActivity;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.ProgramListAdapter;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ProgramListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    private final static String TAG = ProgramListFragment.class.getSimpleName();

    private Activity activity;
    private ActionBarInterface actionBarInterface;
    private FragmentStatusInterface fragmentStatusInterface;

    private ProgramListAdapter adapter;
    private List<Program> list;
    private ListView listView;
    private Channel channel;
    private boolean isDualPane = false;
    private boolean allowLoading = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't exist because the fragment
        // will not be shown.
        if (container == null) {
            return null;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            channel = app.getChannel(bundle.getLong(Constants.BUNDLE_CHANNEL_ID, 0));
            isDualPane = bundle.getBoolean(Constants.BUNDLE_DUAL_PANE, false);
        }

        View v = inflater.inflate(R.layout.list_layout, container, false);
        listView = (ListView) v.findViewById(R.id.item_list);
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

        // If the channel is null exit
        if (channel == null) {
            activity.finish();
            return;
        }

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onListItemSelected(position, adapter.getItem(position), TAG);
                }
            }
        });
        
        list = new ArrayList<Program>();
        adapter = new ProgramListAdapter(activity, list);
        listView.setAdapter(adapter);

        setHasOptionsMenu(true);
        registerForContextMenu(listView);
    }

    /**
     * Activated the scroll listener to more programs can be loaded when the end
     * of the program list has been reached.
     */
    private void enableScrollListener() {
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // Enable loading when the user has scrolled pretty much to the end of the list
                if ((++firstVisibleItem + visibleItemCount) > totalItemCount) {
                    allowLoading = true;
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If loading is allowed and the scrolling has stopped, load more data 
                if (scrollState == SCROLL_STATE_IDLE && allowLoading) {
                    if (fragmentStatusInterface != null) {
                        allowLoading = false;
                        if (fragmentStatusInterface != null) {
                            fragmentStatusInterface.moreDataRequired(channel, TAG);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        if (!app.isLoading()) {
            populateList();
        }
    }
    
    /**
     * Fills the adapter with all program that are part of the given channel
     */
    public void populateList() {
        // This is required because addAll is only available in API 11 and higher
        for (Iterator<Program> iterator = channel.epg.iterator(); iterator.hasNext();) {
            Program p = iterator.next();
            adapter.add(p);
        }
        adapter.notifyDataSetChanged();

        // Inform the activity to show the currently visible number of the
        // programs and that the program list has been filled with data.
        if (actionBarInterface != null && channel != null) {
            actionBarInterface.setActionBarTitle(channel.name, TAG);
            actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.programs), TAG);
            if (!isDualPane) {
                if (Utils.showChannelIcons(activity)) {
                    actionBarInterface.setActionBarIcon(channel.iconBitmap, TAG);
                } else {
                    actionBarInterface.setActionBarIcon(R.drawable.ic_launcher, TAG);
                }
            }
        }
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
        enableScrollListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
        listView.setOnScrollListener(null);
    }

    @Override
    public void onDetach() {
        fragmentStatusInterface = null;
        actionBarInterface = null;
        super.onDetach();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (getUserVisibleHint() == false) {
            return false;
        }
        // Get the currently selected program from the list where the context
        // menu has been triggered
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Program program = adapter.getItem(info.position);

        // Check if the context menu call came from the list in this fragment
        // (needed for support for multiple fragments in one screen)
        if (info.targetView.getParent() != getView().findViewById(R.id.item_list)) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, program.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, program.title));
            return true;

        case R.id.menu_record_remove:
            Utils.confirmRemoveRecording(activity, program.recording);
            return true;

        case R.id.menu_record_cancel:
            Utils.confirmCancelRecording(activity, program.recording);
            return true;

        case R.id.menu_record:
            Utils.recordProgram(activity, program);
            return true;

        case R.id.menu_play:
            // Open a new activity that starts playing the program
            if (program != null && program.channel != null) {
                Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
                intent.putExtra(Constants.BUNDLE_CHANNEL_ID, program.channel.id);
                startActivity(intent);
            }
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        activity.getMenuInflater().inflate(R.menu.program_context_menu, menu);
        
        // Get the currently selected program from the list where the context
        // menu has been triggered
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Program program = adapter.getItem(info.position);
        
        // Set the title of the context menu and show or hide 
        // the menu items depending on the program state
        if (program != null) {
            menu.setHeaderTitle(program.title);
            Utils.setProgramMenu(menu, program);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu in dual pane mode or if no genre colors shall be shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsProgramsPref", false);
        (menu.findItem(R.id.menu_genre_color_info_programs)).setVisible(!isDualPane && showGenreColors);
        (menu.findItem(R.id.menu_play)).setVisible(!isDualPane);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.program_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_play:
            // Open a new activity that starts playing the first program that is
            // currently transmitted over this channel 
            if (channel != null) {
                Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
                intent.putExtra(Constants.BUNDLE_CHANNEL_ID, channel.id);
                startActivity(intent);
            }
            return true;

        case R.id.menu_genre_color_info_programs:
            Utils.showGenreColorDialog(activity);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the specified action will be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_LOADING)) {
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
        } else if (action.equals(Constants.ACTION_PROGRAM_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Program p = (Program) obj;
                    if (channel != null && p.channel.id == channel.id) {
                        adapter.add(p);
                        adapter.notifyDataSetChanged();
                        adapter.sort();
                        if (actionBarInterface != null) {
                            actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.programs), TAG);
                        }
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_PROGRAM_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.remove((Program) obj);
                    adapter.notifyDataSetChanged();
                    if (actionBarInterface != null) {
                        actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.programs), TAG);
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_PROGRAM_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Program) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(Constants.ACTION_DVR_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Recording rec = (Recording) obj;
                    for (Program p : adapter.getList()) {
                        if (rec == p.recording) {
                            adapter.update(p);
                            adapter.notifyDataSetChanged();
                            return;
                        }
                    }
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
        // Simulate a click in the list item to inform the activity
        if (adapter != null && adapter.getCount() > position) {
            Program p = (Program) adapter.getItem(position);
            if (fragmentStatusInterface != null) {
                fragmentStatusInterface.onListItemSelected(position, p, TAG);
            }
        }
    }

    @Override
    public Object getSelectedItem() {
        return channel;
    }

    @Override
    public int getItemCount() {
        return adapter.getCount();
    }
}
