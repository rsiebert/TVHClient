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
import java.util.regex.Pattern;

import org.tvheadend.tvhclient.adapter.SearchResultAdapter;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.R;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
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
    private SearchResultAdapter adapter;
    private ListView listView;
    private Pattern pattern;
    private Channel channel;
    // The currently selected program
    private Program program;

    @Override
    public void onCreate(Bundle icicle) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(icicle);
        setContentView(R.layout.list_layout);
        
        // Setup the action bar and show the title
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.menu_search);
        actionBar.setSubtitle(getIntent().getStringExtra(SearchManager.QUERY));

        listView = (ListView) findViewById(R.id.item_list);
        registerForContextMenu(listView);
        
        List<Program> srList = new ArrayList<Program>();
        adapter = new SearchResultAdapter(this, srList);
        adapter.sort();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new OnItemClickListener() {
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

        TVHClientApplication app = (TVHClientApplication) getApplication();
        Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            channel = app.getChannel(appData.getLong("channelId"));
        } else {
            channel = null;
        }

        adapter.clear();

        // Create the intent with the search options 
        String query = intent.getStringExtra(SearchManager.QUERY);
        pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        intent = new Intent(SearchResultActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_EPG_QUERY);
        intent.putExtra("query", query);
        if (channel != null) {
            intent.putExtra("channelId", channel.id);
        }

        // Save the query so it can be shown again
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
                this, SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
        suggestions.saveRecentQuery(query, null);

        // Now call the service with the query to get results
        startService(intent);

        if (channel == null) {
            // Go through the list of programs in all channels and search if the
            // desired program exists
            for (Channel ch : app.getChannels()) {
                if (ch != null) {
                    for (Program p : ch.epg) {
                        if (p != null && p.title != null && p.title.length() > 0) {
                            // Check if the program name matches the search pattern
                            if (pattern.matcher(p.title).find()) {
                                adapter.add(p);
                            }
                        }
                    }
                }
            }
        } else {
            // Go through the list of programs in the given channel and search
            // if the desired program exists
            if (channel.epg != null) {
                for (Program p : channel.epg) {
                    if (p != null && p.title != null && p.title.length() > 0) {
                        // Check if the program name matches the search pattern
                        if (pattern.matcher(p.title).find()) {
                            adapter.add(p);
                        }
                    }
                }
            }
        }

        actionBar.setTitle(android.R.string.search_go);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeListener(this);
    }

    protected void showProgramDetails(int position) {
        Program p = (Program) adapter.getItem(position);
        if (p != null) {
            Intent intent = new Intent(this, ProgramDetailsActivity.class);
            intent.putExtra("eventId", p.id);
            intent.putExtra("channelId", p.channel.id);
            startActivity(intent);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu item of not required
        MenuItem genreItem = menu.findItem(R.id.menu_genre_color_info);
        if (genreItem != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            genreItem.setVisible(prefs.getBoolean("showGenreColorsSearchPref", false));
        }
        return true;
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

        case R.id.menu_genre_color_info:
            Utils.showGenreColorDialog(this);
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
            Utils.removeProgram(this, program.recording);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(this, program.recording);
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
        getMenuInflater().inflate(R.menu.program_context_menu, menu);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        program = adapter.getItem(info.position);

        // Set the title of the context menu
        if (program != null) {
            menu.setHeaderTitle(program.title);
        }
        //  Show or hide the menu items depending on the program state
        Utils.setProgramMenu(menu, program);
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHClientApplication.ACTION_PROGRAMME_ADD)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Program p = (Program) obj;
                    if (p != null && p.title != null && p.title.length() > 0) {
                        if (pattern != null && pattern.matcher(p.title).find()) {
                            adapter.add(p);
                            adapter.notifyDataSetChanged();
                            adapter.sort();
                            actionBar.setSubtitle(adapter.getCount() + " " + getString(R.string.results));
                        }
                    }
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_PROGRAMME_DELETE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Program p = (Program) obj;
                    adapter.remove(p);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_PROGRAMME_UPDATE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Program) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_DVR_UPDATE)) {
            runOnUiThread(new Runnable() {
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
}
