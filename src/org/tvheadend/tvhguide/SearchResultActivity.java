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
package org.tvheadend.tvhguide;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.tvheadend.tvhguide.adapter.SearchResultAdapter;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Program;
import org.tvheadend.tvhguide.model.Recording;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class SearchResultActivity extends ActionBarActivity implements HTSListener {

    private ActionBar actionBar = null;
    private SearchResultAdapter srAdapter;
    private ListView searchListView;
    private Pattern pattern;
    private Channel channel;
    // The currently selected program
    private Program program;

    @Override
    public void onCreate(Bundle icicle) {

        // Apply the specified theme
        setTheme(Utils.getThemeId(this));

        super.onCreate(icicle);
        setContentView(R.layout.list_layout);
        
        // Setup the action bar and show the title
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("Searching");
        actionBar.setSubtitle(getIntent().getStringExtra(SearchManager.QUERY));

        searchListView = (ListView) findViewById(R.id.item_list);
        registerForContextMenu(searchListView);
        
        List<Program> srList = new ArrayList<Program>();
        srAdapter = new SearchResultAdapter(this, srList);
        srAdapter.sort();
        searchListView.setAdapter(srAdapter);

        searchListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showProgramDetails(position);
            }
        });
        
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (!Intent.ACTION_SEARCH.equals(intent.getAction()) || !intent.hasExtra(SearchManager.QUERY)) {
            return;
        }

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            channel = app.getChannel(appData.getLong("channelId"));
        } else {
            channel = null;
        }

        srAdapter.clear();

        String query = intent.getStringExtra(SearchManager.QUERY);
        pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        intent = new Intent(SearchResultActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_EPG_QUERY);
        intent.putExtra("query", query);
        if (channel != null) {
            intent.putExtra("channelId", channel.id);
        }

        startService(intent);

        if (channel == null) {
            for (Channel ch : app.getChannels()) {
                for (Program p : ch.epg) {
                    if (pattern.matcher(p.title).find()) {
                        srAdapter.add(p);
                    }
                }
            }
        } else {
            for (Program p : channel.epg) {
                if (pattern.matcher(p.title).find()) {
                    srAdapter.add(p);
                }
            }
        }

        actionBar.setTitle(android.R.string.search_go);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.removeListener(this);
    }

    protected void showProgramDetails(int position) {
        Program p = (Program) srAdapter.getItem(position);
        Intent intent = new Intent(this, ProgramDetailsActivity.class);
        intent.putExtra("eventId", p.id);
        intent.putExtra("channelId", p.channel.id);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
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
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
        getMenuInflater().inflate(R.menu.context_menu, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        program = srAdapter.getItem(info.position);

        // Set the title of the context menu and show or hide 
        // the menu items depending on the program state
        menu.setHeaderTitle(program.title);
        Utils.setProgramMenu(menu, program);
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_ADD)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Program p = (Program) obj;
                    if (pattern != null && pattern.matcher(p.title).find()) {
                        srAdapter.add(p);
                        srAdapter.notifyDataSetChanged();
                        srAdapter.sort();
                        
                        actionBar.setSubtitle(srAdapter.getCount() + " " + getString(R.string.results));
                    }
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_DELETE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Program p = (Program) obj;
                    srAdapter.remove(p);
                    srAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_UPDATE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    srAdapter.update((Program) obj);
                    srAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_UPDATE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Recording rec = (Recording) obj;
                    for (Program p : srAdapter.getList()) {
                        if (rec == p.recording) {
                            srAdapter.update((Program) obj);
                            srAdapter.notifyDataSetChanged();
                            return;
                        }
                    }
                }
            });
        }
    }
}
