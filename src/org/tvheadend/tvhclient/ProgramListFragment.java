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

import org.tvheadend.tvhclient.adapter.ProgramListAdapter;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.ProgramLoadingInterface;
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

public class ProgramListFragment extends Fragment implements HTSListener {

    private final static String TAG = ProgramListFragment.class.getSimpleName();

    private Activity activity;
    private ActionBarInterface actionBarInterface;
    private ProgramLoadingInterface loadMoreProgramsInterface;

    private ProgramListAdapter prAdapter;
    private List<Program> prList;
    private ListView prListView;
    private Channel channel;
    private boolean isLoading = false;

    private static int newProgramsLoadedCounter = 0;
    private static final int newProgramsToLoad = 10;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null) {
            return null;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            channel = app.getChannel(bundle.getLong("channelId", 0));
        }

        // Add a listener to check if the program list has been scrolled.
        // If the last list item is visible, load more data and show it.
        View v = inflater.inflate(R.layout.list_layout, container, false);
        prListView = (ListView) v.findViewById(R.id.item_list);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (activity instanceof ProgramLoadingInterface) {
            loadMoreProgramsInterface = (ProgramLoadingInterface) activity;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        prListView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if ((++firstVisibleItem + visibleItemCount) > totalItemCount) {
                    if (actionBarInterface != null) {
                        actionBarInterface.setActionBarSubtitle(getString(R.string.loading), TAG);
                    }
                    
                    // Do not load more programs if we are already doing it. This avoids
                    // calling the service for nothing and reduces the used bandwidth.
                    if (isLoading || channel == null) {
                        return;
                    }
                    isLoading = true;
                    if (loadMoreProgramsInterface != null) {
                        loadMoreProgramsInterface.loadMorePrograms(channel);
                    }
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
            }
        });

        prListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showProgramDetails(position);
            }
        });
        
        prList = new ArrayList<Program>();
        prAdapter = new ProgramListAdapter(activity, prList);
        prListView.setAdapter(prAdapter);
        registerForContextMenu(prListView);
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        
        // In case we return from the program details screen and the user added
        // a program to the schedule or has deleted one from it we need to
        // update the list to reflect these changes.
        prList.clear();
        if (channel != null) {
            prList.addAll(channel.epg);
        }
        prAdapter.sort();
        prAdapter.notifyDataSetChanged();
        
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarSubtitle(prAdapter.getCount() + " " + getString(R.string.programs), TAG);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    protected void showProgramDetails(int position) {
        Program p = prAdapter.getItem(position);
        Intent intent = new Intent(activity, ProgramDetailsActivity.class);
        intent.putExtra("eventId", p.id);
        intent.putExtra("channelId", p.channel.id);
        startActivity(intent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        
        // Get the currently selected program from the list
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Program program = prAdapter.getItem(info.position);
        
        switch (item.getItemId()) {
        case R.id.menu_search:
            // Show the search text input in the action bar
            onSearchRequested();
            return true;

        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, program.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, program.title));
            return true;
            
        case R.id.menu_record_remove:
            Utils.removeProgram(activity, program.recording);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(activity, program.recording);
            return true;

        case R.id.menu_record:
            Utils.recordProgram(activity, program.id, program.channel.id);
            return true;

        case R.id.menu_play:
            // Open a new activity to stream the current program to this device
            Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
            intent.putExtra("channelId", program.channel.id);
            startActivity(intent);
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        activity.getMenuInflater().inflate(R.menu.program_context_menu, menu);
        
        // Get the currently selected program from the list
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Program program = prAdapter.getItem(info.position);
        
        // Set the title of the context menu and show or hide 
        // the menu items depending on the program state
        menu.setHeaderTitle(program.title);
        Utils.setProgramMenu(menu, program);
        
        // Allow playing the first item, its currently being shown
        if (info.position == 0) {
            MenuItem playMenuItem = menu.findItem(R.id.menu_play);
            playMenuItem.setVisible(true);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu item of not required
        MenuItem genreItem = menu.findItem(R.id.menu_genre_color_info);
        if (genreItem != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            genreItem.setVisible(prefs.getBoolean("showGenreColorsProgramsPref", false));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.program_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_search:
            // Show the search text input in the action bar
            onSearchRequested();
            return true;
        case R.id.menu_settings:
            // Now start the settings activity 
            Intent i = new Intent(activity, SettingsActivity.class);
            startActivityForResult(i, Utils.getResultCode(R.id.menu_settings));
            return true;
        case R.id.menu_play:
            // Open a new activity to stream the current program to this device
            Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
            if (channel != null) {
                intent.putExtra("channelId", channel.id);
            }
            startActivity(intent);
            return true;
        case R.id.menu_genre_color_info:
            Utils.showGenreColorDialog(activity);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public boolean onSearchRequested() {
        Bundle bundle = new Bundle();
        if (channel != null) {
            bundle.putLong("channelId", channel.id);
        }
        activity.startSearch(null, false, bundle, false);
        return true;
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHClientApplication.ACTION_PROGRAMME_ADD)) {
            
            // Increase the counter that will allow loading more programs.
            if (++newProgramsLoadedCounter >= newProgramsToLoad) {
                isLoading  = false;
            }
            // A new program has been added
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Program p = (Program) obj;
                    if (channel != null && p.channel.id == channel.id) {
                        prAdapter.add(p);
                        prAdapter.notifyDataSetChanged();
                        prAdapter.sort();
                        if (actionBarInterface != null) {
                            actionBarInterface.setActionBarSubtitle(prAdapter.getCount() + " " + getString(R.string.programs), TAG);
                        }
                    }
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_PROGRAMME_DELETE)) {
            // An existing program has been deleted
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    prAdapter.remove((Program) obj);
                    prAdapter.notifyDataSetChanged();
                    if (actionBarInterface != null) {
                        actionBarInterface.setActionBarSubtitle(prAdapter.getCount() + " " + getString(R.string.programs), TAG);
                    }
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_PROGRAMME_UPDATE)) {
            // An existing program has been updated
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    prAdapter.update((Program) obj);
                    prAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_DVR_UPDATE)) {
            // An existing recording has been updated
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Recording rec = (Recording) obj;
                    for (Program p : prAdapter.getList()) {
                        if (rec == p.recording) {
                            prAdapter.update(p);
                            prAdapter.notifyDataSetChanged();
                            return;
                        }
                    }
                }
            });
        }
    }
}
