/*
 *  Copyright (C) 2013 Robert Siebert
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHClient.
 *
 * TVHClient is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHClient is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHClient.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.adapter.SearchResultAdapter;
import org.tvheadend.tvhclient.fragments.ProgramDetailsFragment;
import org.tvheadend.tvhclient.fragments.RecordingDetailsFragment;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Model;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

@SuppressWarnings("deprecation")
public class SearchResultActivity extends ActionBarActivity implements SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, HTSListener {

    private final static String TAG = SearchResultActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private SearchResultAdapter adapter;
    private ListView listView;
    private Channel channel;
    private Recording recording;
    private MenuItem searchMenuItem = null;
    private Runnable updateTask;
    private Runnable finishTask;
    private Handler updateHandler = new Handler();
    private boolean handlerRunning = false;

    // Contains the search string from the search input field 
    private String query;

    private SearchView searchView;
    private TVHClientApplication app = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_layout);

        app = (TVHClientApplication) getApplication();

        // Setup the action bar and show the title
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.search);

        listView = (ListView) findViewById(R.id.item_list);
        registerForContextMenu(listView);

        // Show the details of the program when the user has selected one
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Model model = adapter.getItem(position);

                // Show the program details dialog
                if (model instanceof Program) {
                    final Program program = (Program) model;
                    if (program != null) {
                        Bundle args = new Bundle();
                        args.putLong(Constants.BUNDLE_PROGRAM_ID, program.id);
                        args.putLong(Constants.BUNDLE_CHANNEL_ID, program.channel.id);
                        args.putBoolean(Constants.BUNDLE_DUAL_PANE, false);
                        args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, true);
                        DialogFragment newFragment = ProgramDetailsFragment.newInstance(args);
                        newFragment.show(getSupportFragmentManager(), "dialog");
                    }
                }

                // Show the recording details dialog
                if (model instanceof Recording) {
                    final Recording recording = (Recording) model;
                    if (recording != null) {
                        Bundle args = new Bundle();
                        args.putLong(Constants.BUNDLE_RECORDING_ID, recording.id);
                        args.putBoolean(Constants.BUNDLE_DUAL_PANE, false);
                        args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, true);
                        DialogFragment newFragment = RecordingDetailsFragment.newInstance(args);
                        newFragment.show(getSupportFragmentManager(), "dialog");
                    }
                }
            }
        });

        // This is the list with the initial data from the program guide. It
        // will be passed to the search adapter. 
        List<Model> list = new ArrayList<Model>();

        Intent intent = getIntent();

        // Try to get the channel and recording id if given, to limit the search
        // a single channel or the completed recordings.
        Bundle bundle = intent.getBundleExtra(SearchManager.APP_DATA);
        if (bundle != null) {
            channel = app.getChannel(bundle.getLong(Constants.BUNDLE_CHANNEL_ID));
            recording = app.getRecording(bundle.getLong(Constants.BUNDLE_RECORDING_ID));
        } else {
            channel = null;
            recording = null;
        }

        // Depending on the search request, fill the adapter with the already
        // available data. Either add all completed recordings or add all
        // available programs from one or all channels.
        if (recording != null) {
            app.log(TAG, "onCreate recording");
            for (Recording rec : app.getRecordingsByType(Constants.RECORDING_TYPE_COMPLETED)) {
                if (rec != null && rec.title != null && rec.title.length() > 0) {
                    list.add(rec);
                }
            }
        } else {
            if (channel == null) {
                // Add all available programs from all channels.
                for (Channel ch : app.getChannels()) {
                    if (ch != null) {
                        synchronized(ch.epg) {
                            for (Program p : ch.epg) {
                                if (p != null && p.title != null && p.title.length() > 0) {
                                    list.add(p);
                                }
                            }
                        }
                    }
                }
            } else {
                // Add all available programs from the given channel.
                if (channel.epg != null) {
                    synchronized(channel.epg) {
                        for (Program p : channel.epg) {
                            if (p != null && p.title != null && p.title.length() > 0) {
                                list.add(p);
                            }
                        }
                    }
                }
            }
        }

        // Create the adapter with the given initial program guide data
        adapter = new SearchResultAdapter(this, list);
        listView.setAdapter(adapter);

        // Create the runnable that will filter the data from the adapter. It
        // also updates the action bar with the number of available program that
        // will be searched 
        updateTask = new Runnable() {
            public void run() {
                adapter.getFilter().filter(query.toString());
                if (recording == null) {
                    actionBar.setSubtitle(getString(R.string.searching_programs, adapter.getFullCount()));
                } else {
                    actionBar.setSubtitle(getString(R.string.searching_recordings, adapter.getFullCount()));
                }
                handlerRunning  = false;
            }
        };

        // Create the runnable that will show the final search results
        finishTask = new Runnable() {
            public void run() {
                actionBar.setSubtitle(getResources().getQuantityString(
                        R.plurals.results, adapter.getCount(),
                        adapter.getCount()));
            }
        };

        // If the screen has been rotated then overwrite the original query in
        // the intent with the one that has been saved before the rotation. 
        if (savedInstanceState != null) {
            query = savedInstanceState.getString(SearchManager.QUERY);
            intent.removeExtra(SearchManager.QUERY);
            intent.putExtra(SearchManager.QUERY, query);
        }

        onNewIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(SearchManager.QUERY, query);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Quit if the search mode is not active
        if (!Intent.ACTION_SEARCH.equals(intent.getAction())
                || !intent.hasExtra(SearchManager.QUERY)) {
            return;
        }

        // Try to get the channel and recording id if given, to limit the search
        // a single channel or the completed recordings.
        // TODO is this required if its already in the onCreate method?
        Bundle bundle = intent.getBundleExtra(SearchManager.APP_DATA);
        if (bundle != null) {
            channel = app.getChannel(bundle.getLong(Constants.BUNDLE_CHANNEL_ID));
            recording = app.getRecording(bundle.getLong(Constants.BUNDLE_RECORDING_ID));
        } else {
            channel = null;
            recording = null;
        }

        // Get the given search query
        query = intent.getStringExtra(SearchManager.QUERY);

        // Save the query so it can be shown again.
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
        suggestions.saveRecentQuery(query, null);

        // If the search shall be performed on one or all channels, fetch and
        // save all programs from the server and filter them later in the
        // adapter. If a user starts a different search later all programs are
        // available already and not only the ones that would be been loaded if
        // the query would have been passed to the server.
        if (recording == null) {
            intent = new Intent(this, HTSService.class);
            intent.setAction(Constants.ACTION_EPG_QUERY);
            intent.putExtra("query", "");
            if (channel != null) {
                intent.putExtra(Constants.BUNDLE_CHANNEL_ID, channel.id);
            }
            startService(intent);
        }

        startQuickAdapterUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu if no genre colors shall be shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsSearchPref", false);
        (menu.findItem(R.id.menu_genre_color_info)).setVisible(showGenreColors);
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_menu, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchMenuItem = menu.findItem(R.id.menu_search); 
            searchView = (SearchView) searchMenuItem.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(true);
            searchView.setOnQueryTextListener(this);
            searchView.setOnSuggestionListener(this);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;

        case R.id.menu_search:
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                onSearchRequested();
            }
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

        // Get the currently selected program from the list where the context
        // menu has been triggered
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // Check for a valid adapter size and objects
        if (info == null || adapter == null || adapter.getCount() <= info.position) {
            return super.onContextItemSelected(item);
        }

        final Model model = adapter.getItem(info.position);
        if (model == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.menu_search_epg:
            if (model instanceof Program) {
                startActivity(new SearchEPGIntent(this, ((Program) model).title));
            }
            return true;

        case R.id.menu_search_imdb:
            if (model instanceof Program) {
                startActivity(new SearchIMDbIntent(this, ((Program) model).title));
            }
            return true;

        case R.id.menu_record_remove:
            if (model instanceof Program) {
                Utils.confirmRemoveRecording(this, ((Program) model).recording);
            }
            if (model instanceof Recording) {
                Utils.confirmRemoveRecording(this, (Recording) model);
            }
            return true;

        case R.id.menu_record_cancel:
            if (model instanceof Program) {
                Utils.confirmCancelRecording(this, ((Program) model).recording);
            }
            return true;

        case R.id.menu_record_once:
            if (model instanceof Program) {
                Utils.recordProgram(this, (Program) model, false);
            }
            return true;

        case R.id.menu_record_series:
            if (model instanceof Program) {
                Utils.recordProgram(this, (Program) model, true);
            }
            return true;

        case R.id.menu_play:
            if (model instanceof Program) {
                Program program = (Program) model;
                // Open a new activity that starts playing the program
                if (program != null && program.channel != null) {
                    // TODO create play intent
                    Intent intent = new Intent(this, ExternalActionActivity.class);
                    intent.putExtra(Constants.BUNDLE_CHANNEL_ID, program.channel.id);
                    startActivity(intent);
                }
            }
            if (model instanceof Recording) {
                Recording recording = (Recording) model;
                // Open a new activity that starts playing the program
                if (recording != null) {
                    // TODO create play intent
                    Intent intent = new Intent(this, ExternalActionActivity.class);
                    intent.putExtra(Constants.BUNDLE_RECORDING_ID, recording.id);
                    startActivity(intent);
                }
            }
            return true;

        case R.id.menu_download:
            if (model instanceof Recording) {
                // TODO create download intent
                Recording rec = (Recording) model;
                Intent dlIntent = new Intent(this, ExternalActionActivity.class);
                dlIntent.putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
                dlIntent.putExtra(Constants.BUNDLE_EXTERNAL_ACTION, Constants.EXTERNAL_ACTION_DOWNLOAD);
                startActivity(dlIntent);
            }
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Get the currently selected program from the list where the context
        // menu has been triggered
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        final Model model = adapter.getItem(info.position);

        if (model instanceof Program) {
            getMenuInflater().inflate(R.menu.program_context_menu, menu);
            Program program = (Program) model;
            // Set the title of the context menu
            if (program != null) {
                menu.setHeaderTitle(program.title);
            }
            // Show or hide the menu items depending on the program state
            Utils.setProgramMenu(app, menu, program);

        } else if (model instanceof Recording) {
            getMenuInflater().inflate(R.menu.recording_context_menu, menu);
            Recording rec = (Recording) model;
            if (rec != null) {
                menu.setHeaderTitle(rec.title);
            }

            // Hide all menu entries before activating certain ones
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }
            
            // Allow playing and downloading a recording
            (menu.findItem(R.id.menu_record_remove)).setVisible(true);
            (menu.findItem(R.id.menu_play)).setVisible(true);
            if (app.isUnlocked()) {
                (menu.findItem(R.id.menu_download)).setVisible(true);
            }
        }
    }

    /**
     * Starts two timers that will update the search list view when expired. The
     * timer is only started when it is not running. The second timer shows the
     * final search results. If this timer is called while it is running, it
     * will be restarted. This ensures that this timer will be called only once
     * at the end.
     */
    private void startAdapterUpdate(int updateTaskTime, int finishTaskTime) {
        if (!handlerRunning) {
            // Show that the loading is still active
            updateHandler.postDelayed(updateTask, 500);

            // Show the final result when loading is done. This is called only
            // once when the startDelayedAdapterUpdate method is called for the
            // last time.
            updateHandler.removeCallbacks(finishTask);
            updateHandler.postDelayed(finishTask, 1500);
            handlerRunning = true;
        }
    }

    /**
     * Calls the two timers with the default timeout values.
     */
    private void startDelayedAdapterUpdate() {
        startAdapterUpdate(500, 2000);
    }

    /**
     * Calls the two timers with shorter timeout values.
     */
    private void startQuickAdapterUpdate() {
        startAdapterUpdate(10, 50);
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(final String action, final Object obj) {
        if (action.equals(Constants.ACTION_PROGRAM_ADD)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Model m = (Model) obj;
                    adapter.remove(m);
                    adapter.add(m);
                    startDelayedAdapterUpdate();
                }
            });
        } else if (action.equals(Constants.ACTION_PROGRAM_DELETE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Model m = (Model) obj;
                    adapter.remove(m);
                    startDelayedAdapterUpdate();
                }
            });
        } else if (action.equals(Constants.ACTION_PROGRAM_UPDATE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Model) obj);
                    startDelayedAdapterUpdate();
                }
            });
        } else if (action.equals(Constants.ACTION_DVR_DELETE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Model m = (Model) obj;
                    adapter.remove(m);
                    startDelayedAdapterUpdate();
                }
            });
        } else if (action.equals(Constants.ACTION_DVR_UPDATE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Model) obj);
                    startDelayedAdapterUpdate();
                }
            });
        }
    }

    @Override
    public boolean onQueryTextChange(String text) {
        if (text.length() >= 3 && app.isUnlocked()) {
            query = text;
            startQuickAdapterUpdate();
        }
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onQueryTextSubmit(String text) {
        query = text;
        startQuickAdapterUpdate();

        // Close the search view and show the action bar again
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            searchMenuItem.collapseActionView();
        }
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onSuggestionClick(int position) {
        // Get the text of the selected suggestion
        Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getCursor();
        query = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        startQuickAdapterUpdate();

        // Close the search view and show the action bar again
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            searchMenuItem.collapseActionView();
        }
        return true;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        // Get the text of the selected suggestion
        Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getCursor();
        query = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        startQuickAdapterUpdate();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Stop the service to stop loading any EPG data.
        stopService(new Intent(this, HTSService.class));
    }
}
