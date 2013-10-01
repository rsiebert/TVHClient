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
import java.util.List;
import java.util.regex.Pattern;

import org.tvheadend.tvhguide.adapter.SearchResultAdapter;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Program;
import org.tvheadend.tvhguide.model.Recording;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 *
 * @author john-tornblom
 */
public class SearchResultActivity extends ListActivity implements HTSListener {

    private SearchResultAdapter srAdapter;
    private Pattern pattern;
    private Channel channel;

    @Override
    public void onCreate(Bundle icicle) {

        // Apply the specified theme
        setTheme(Utils.getThemeId(this));

        super.onCreate(icicle);

        // Setup the action bar and show the title
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setTitle("Searching");
        
        registerForContextMenu(getListView());
        
        List<Program> srList = new ArrayList<Program>();
        srAdapter = new SearchResultAdapter(this, srList);
        srAdapter.sort();
        setListAdapter(srAdapter);

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

        getActionBar().setTitle(android.R.string.search_go);
        getActionBar().setSubtitle(query);
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

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
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
        
            case R.string.menu_record:
            case R.string.menu_record_cancel:
            case R.string.menu_record_remove: {
                startService(item.getIntent());
                return true;
            }
            default: {
                return super.onContextItemSelected(item);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Program p = srAdapter.getItem(info.position);

        menu.setHeaderTitle(p.title);

        Intent intent = new Intent(this, HTSService.class);

        MenuItem item = null;

        if (p.recording == null) {
            intent.setAction(HTSService.ACTION_DVR_ADD);
            intent.putExtra("eventId", p.id);
            intent.putExtra("channelId", p.channel.id);
            item = menu.add(ContextMenu.NONE, R.string.menu_record, ContextMenu.NONE, R.string.menu_record);
        } else if ("recording".equals(p.recording.state) || "scheduled".equals(p.recording.state)) {
            intent.setAction(HTSService.ACTION_DVR_CANCEL);
            intent.putExtra("id", p.recording.id);
            item = menu.add(ContextMenu.NONE, R.string.menu_record_cancel, ContextMenu.NONE, R.string.menu_record_cancel);
        } else {
            intent.setAction(HTSService.ACTION_DVR_DELETE);
            intent.putExtra("id", p.recording.id);
            item = menu.add(ContextMenu.NONE, R.string.menu_record_remove, ContextMenu.NONE, R.string.menu_record_remove);
        }

        item.setIntent(intent);
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
                    Program p = (Program) obj;
                    srAdapter.updateView(getListView(), p);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Recording rec = (Recording) obj;
                    for (Program p : srAdapter.getList()) {
                        if (rec == p.recording) {
                            srAdapter.updateView(getListView(), p);
                            return;
                        }
                    }
                }
            });
        }
    }
}
