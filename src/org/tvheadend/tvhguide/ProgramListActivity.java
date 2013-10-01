/*
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
package org.tvheadend.tvhguide;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.tvheadend.tvhguide.adapter.ProgrammeListAdapter;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Program;
import org.tvheadend.tvhguide.model.Recording;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

/**
 *
 * @author john-tornblom
 */
public class ProgramListActivity extends Activity implements HTSListener {

    private ProgrammeListAdapter prAdapter;
    private List<Program> prList;
    private ListView prListView;
    private Channel channel;
    private boolean isLoading = false;
    private static int newProgramsLoadedCounter = 0;
    private static final int newProgramsToLoad = 10;
    // The currently selected program
    private Program program;
    
    @Override
    public void onCreate(Bundle icicle) {
        
        // Apply the specified theme
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        setTheme(theme ? R.style.CustomTheme_Light : R.style.CustomTheme);
        
        super.onCreate(icicle);
        setContentView(R.layout.channel_list);
        
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        channel = app.getChannel(getIntent().getLongExtra("channelId", 0));

        if (channel == null) {
            finish();
            return;
        }

        // Setup the action bar and show the title
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setTitle(channel.name);
        getActionBar().setIcon(new BitmapDrawable(getResources(), channel.iconBitmap));
        
        // Add a listener to check if the program list has been scrolled.
        // If the last list item is visible, load more data and show it.
        prListView = (ListView) findViewById(R.id.channel_list);
        prListView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if ((++firstVisibleItem + visibleItemCount) > totalItemCount) {
                    getActionBar().setSubtitle(R.string.inf_load);
                    loadMorePrograms();
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
        prAdapter = new ProgrammeListAdapter(this, prList);
        prListView.setAdapter(prAdapter);

        registerForContextMenu(prListView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addListener(this);
        
        // In case we return from the program details screen and the user added
        // a program to the schedule or has deleted one from it we need to
        // update the list to reflect these changes.
        prList.clear();
        prList.addAll(channel.epg);
        prAdapter.sort();
        prAdapter.notifyDataSetChanged();

        getActionBar().setSubtitle(prAdapter.getCount() + " " + getString(R.string.programs));
    }

    @Override
    protected void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.removeListener(this);
    }

    protected void showProgramDetails(int position) {
        Program p = prAdapter.getItem(position);
        Intent intent = new Intent(this, ProgramActivity.class);
        intent.putExtra("eventId", p.id);
        intent.putExtra("channelId", p.channel.id);
        startActivity(intent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_search:
            // Show the search text input in the action bar
            onSearchRequested();
            return true;

        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(this, program.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(this, program.title));
            return true;
            
        case R.id.menu_record_remove:
            Utils.removeProgram(this, program.recording.id);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(this, program.recording.id);
            return true;

        case R.id.menu_record:
            Utils.recordProgram(this, program.id, program.channel.id);
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.details_menu, menu);
        
        // Get the currently selected program from the list
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        program = prAdapter.getItem(info.position);
        
        // Set the title of the context menu and show or hide 
        // the menu items depending on the program state
        menu.setHeaderTitle(program.title);
        Utils.setProgramMenu(menu, program);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.program_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        case R.id.menu_search:
            // Show the search text input in the action bar
            onSearchRequested();
            return true;
        case R.id.menu_play:
            // Open a new activity to stream the current program to this device
            Intent intent = new Intent(ProgramListActivity.this, PlaybackActivity.class);
            intent.putExtra("channelId", channel.id);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSearchRequested() {
        Bundle bundle = new Bundle();
        bundle.putLong("channelId", channel.id);
        startSearch(null, false, bundle, false);
        return true;
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_ADD)) {
            
            // Increase the counter that will allow loading more programs.
            if (++newProgramsLoadedCounter >= newProgramsToLoad)
                isLoading  = false;
            
            // A new program has been added
            runOnUiThread(new Runnable() {
                public void run() {
                    Program p = (Program) obj;
                    if (channel != null && p.channel.id == channel.id) {
                        prAdapter.add(p);
                        prAdapter.notifyDataSetChanged();
                        prAdapter.sort();
                        getActionBar().setSubtitle(prAdapter.getCount() + " " + getString(R.string.programs));
                    }
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_DELETE)) {
            // An existing program has been deleted
            runOnUiThread(new Runnable() {
                public void run() {
                    Program p = (Program) obj;
                    prAdapter.remove(p);
                    prAdapter.notifyDataSetChanged();
                    getActionBar().setSubtitle(prAdapter.getCount() + " " + getString(R.string.programs));
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_UPDATE)) {
            // An existing program has been updated
            runOnUiThread(new Runnable() {
                public void run() {
                    Program p = (Program) obj;
                    prAdapter.updateView(prListView, p);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_UPDATE)) {
            // An existing recording has been updated
            runOnUiThread(new Runnable() {
                public void run() {
                    Recording rec = (Recording) obj;
                    for (Program p : prAdapter.getList()) {
                        if (rec == p.recording) {
                            prAdapter.updateView(prListView, p);
                            return;
                        }
                    }
                }
            });
        }
    }
	
    /**
     * 
     */
    protected void loadMorePrograms() {

        // Do not load more programs if we are already doing it. This avoids
        // calling the service for nothing and reduces the used bandwidth.
        if (isLoading)
            return;
        
        isLoading = true;
        
        Iterator<Program> it = channel.epg.iterator();
        Program p = null;
        long nextId = 0;

        while (it.hasNext()) {
            p = it.next();
            if (p.id != nextId && nextId != 0) {
                break;
            }
            nextId = p.nextId;
        }

        if (p == null) {
            return;
        }
        if (nextId == 0) {
            nextId = p.nextId;
        }
        if (nextId == 0) {
            nextId = p.id;
        }

        // Set the required information and start the service command.
        Intent intent = new Intent(this, HTSService.class);
        intent.setAction(HTSService.ACTION_GET_EVENTS);
        intent.putExtra("eventId", nextId);
        intent.putExtra("channelId", channel.id);
        intent.putExtra("count", newProgramsToLoad);
        startService(intent);
    }
}
