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
package org.tvheadend.tvhclient.ui.search;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.local.SuggestionProvider;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.ui.recordings.RecordingDetailsActivity;
import org.tvheadend.tvhclient.service.HTSService;
import org.tvheadend.tvhclient.service.HTSListener;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.ArrayList;
import java.util.List;

// TODO search for recordings broken/missing

public class SearchResultActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, HTSListener {

    private final static String TAG = SearchResultActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private SearchResultAdapter adapter;
    private Channel channel;
    //private Recording recording;
    private MenuItem searchMenuItem = null;
    private Runnable updateTask;
    private Runnable finishTask;
    private final Handler updateHandler = new Handler();
    private boolean handlerRunning = false;

    // Contains the search string from the search input field 
    private String query;

    private SearchView searchView;
    private TVHClientApplication app;
    private DataStorage dataStorage;
    private MenuUtils menuUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.x_list_layout);

        app = TVHClientApplication.getInstance();
        dataStorage = DataStorage.getInstance();

        // Setup the action bar and show the title
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        TextView actionBarTitle = toolbar.findViewById(R.id.actionbar_title);
        actionBarTitle.setText(getString(R.string.search));

        ListView listView = findViewById(R.id.item_list);
        registerForContextMenu(listView);

        menuUtils = new MenuUtils(this);

        // Show the details of the program when the user has selected one
        if (listView != null) {
            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Program program = adapter.getItem(position);

                    // Show the program details dialog
                    //if (model instanceof Program) {
                    //final Program program = (Program) model;
                    /*
                    Bundle args = new Bundle();
                    args.putLong("eventId", program.eventId);
                    Channel channel = dataStorage.getChannelFromArray(program.channelId);
                    args.putLong("channelId", channel.eventId);
                    args.putBoolean("dual_pane", false);
                    args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, true);
                    DialogFragment newFragment = ProgramDetailsFragment.newInstance(args);
                    newFragment.show(getSupportFragmentManager(), "dialog");
                    //}
*/
                    Intent intent = new Intent(SearchResultActivity.this, RecordingDetailsActivity.class);
                    intent.putExtra("eventId", program.eventId);
                    intent.putExtra("type", "program");
                    startActivity(intent);

                    // Show the recording details dialog
                    /*if (model instanceof Recording) {
                        final Recording recording = (Recording) model;
                        Bundle args = new Bundle();
                        args.putLong("dvrId", recording.id);
                        args.putBoolean("dual_pane", false);
                        args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, true);
                        DialogFragment newFragment = RecordingDetailsFragment.newInstance(args);
                        newFragment.show(getSupportFragmentManager(), "dialog");
                    }*/
                }
            });
        }

        // This is the list with the initial data from the program guide. It
        // will be passed to the search adapter. 
        List<Program> list = new ArrayList<>();

        Intent intent = getIntent();

        // Try to get the channel and recording id if given, to limit the search
        // a single channel or the completed recordings.
        Bundle bundle = intent.getBundleExtra(SearchManager.APP_DATA);
        if (bundle != null) {
            channel = dataStorage.getChannelFromArray(bundle.getInt("channelId"));
            //recording = dataStorage.getRecording(bundle.getLong("dvrId"));
        } else {
            channel = null;
            //recording = null;
        }

        // Depending on the search request, fill the adapter with the already
        // available data. Either add all completed recordings or add all
        // available programs from one or all channels.
        /*if (recording != null) {
            for (Recording rec : dataStorage.getRecordingsByType(Constants.RECORDING_TYPE_COMPLETED)) {
                if (rec != null && rec.title != null && rec.title.length() > 0) {
                    list.add(rec);
                }
            }
        } else {
            if (channel == null) {
                // Get all available programs from all channels.
                for (Channel ch : dataStorage.getChannels()) {
                    if (ch != null) {
                        CopyOnWriteArrayList<Program> epg = new CopyOnWriteArrayList<>(ch.epg);
                        for (Program p : epg) {
                            if (p != null && p.title != null && p.title.length() > 0) {
                                list.add(p);
                            }
                        }
                    }
                }
            } else {
                // Get all available programs from the given channel.
                if (channel.epg != null) {
                    CopyOnWriteArrayList<Program> epg = new CopyOnWriteArrayList<>(channel.epg);
                    for (Program p : epg) {
                        if (p != null && p.title != null && p.title.length() > 0) {
                            list.add(p);
                        }
                    }
                }
            }
        //}
*/
        // Create the adapter with the given initial program guide data
        adapter = new SearchResultAdapter(this, list);
        if (listView != null) {
            listView.setAdapter(adapter);
        }

        // Create the runnable that will filter the data from the adapter. It
        // also updates the action bar with the number of available program that
        // will be searched 
        updateTask = new Runnable() {
            public void run() {
                adapter.getFilter().filter(query);
                adapter.sort();
                /*if (recording == null) {
                    actionBar.setSubtitle(getResources().getQuantityString(
                            R.plurals.searching_programs, adapter.getFullCount(),
                            adapter.getFullCount()));
                } else {*/
                actionBar.setSubtitle(getResources().getQuantityString(
                        R.plurals.searching_recordings, adapter.getFullCount(),
                        adapter.getFullCount()));
                //}
                handlerRunning = false;
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
        //if (recording == null) {
        intent = new Intent(this, HTSService.class);
        intent.setAction("epgQuery");
        intent.putExtra("query", query);
        if (channel != null) {
            intent.putExtra("channelId", channel.channelId);
        }
        startService(intent);
        //}

        startDelayedAdapterUpdate();
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
        getMenuInflater().inflate(R.menu.options_menu_search_results, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(this);
        searchView.setOnSuggestionListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_genre_color_info:
                menuUtils.handleMenuGenreColorSelection();
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

        final Program program = adapter.getItem(info.position);
        if (program == null) {
            return super.onContextItemSelected(item);
        }

        Recording rec = dataStorage.getRecordingFromArray(program.dvrId);
        Channel channel = dataStorage.getChannelFromArray(program.channelId);

        switch (item.getItemId()) {
            case R.id.menu_search_epg:
                menuUtils.handleMenuSearchEpgSelection(program.title);
                return true;

            case R.id.menu_search_imdb:
                menuUtils.handleMenuSearchWebSelection(program.title);
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
            /*
            if (model instanceof Recording) {
                Recording rec = (Recording) model;
                menuUtils.handleMenuRemoveRecordingSelection(rec.id, rec.title);
            }
            */
                return true;

            case R.id.menu_record_once:
                menuUtils.handleMenuRecordSelection(program.eventId);
                return true;

            case R.id.menu_record_once_custom_profile:
                // TODO hide this menu if no profiles are available
                menuUtils.handleMenuCustomRecordSelection(program.eventId, channel.channelId);
                return true;

            case R.id.menu_record_series:
                menuUtils.handleMenuSeriesRecordSelection(program.title);
                return true;

            case R.id.menu_play:
                menuUtils.handleMenuPlaySelection(channel.channelId, -1);
                //menuUtils.handleMenuPlaySelection(-1, rec.id);
                return true;

            case R.id.menu_download:
                if (rec != null) {
                    menuUtils.handleMenuDownloadSelection(rec.id);
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
        final Program program = adapter.getItem(info.position);

        getMenuInflater().inflate(R.menu.popup_menu_channellist_program, menu);
        // Set the title of the context menu
        menu.setHeaderTitle(program.title);
        // Show or hide the menu items depending on the program state
        //Utils.setProgramMenu(app, menu, program);
/*
        } else if (model instanceof Recording) {
            getMenuInflater().inflate(R.menu.popup_menu_recordings, menu);
            Recording rec = (Recording) model;
            menu.setHeaderTitle(rec.title);
            // Hide all menu entries before activating certain ones
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }

            // Allow playing, removing and downloading a recording
            (menu.findItem(R.id.menu_record_remove)).setVisible(true);
            (menu.findItem(R.id.menu_play)).setVisible(true);
            if (app.isUnlocked()) {
                (menu.findItem(R.id.menu_download)).setVisible(true);
            }
        }
*/
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
            updateHandler.postDelayed(updateTask, updateTaskTime);

            // Show the final result when loading is done. This is called only
            // once when the startDelayedAdapterUpdate method is called for the
            // last time.
            updateHandler.removeCallbacks(finishTask);
            updateHandler.postDelayed(finishTask, finishTaskTime);
            handlerRunning = true;
        }
    }

    /**
     * Calls the two timers with the default timeout values.
     */
    private void startDelayedAdapterUpdate() {
        startAdapterUpdate(1000, 3000);
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(final String action, final Object obj) {
        switch (action) {
            case "eventAdd":
                runOnUiThread(new Runnable() {
                    public void run() {
                        Program m = (Program) obj;
                        adapter.remove(m);
                        adapter.add(m);
                        startDelayedAdapterUpdate();
                    }
                });
                break;
            case "eventDelete":
                runOnUiThread(new Runnable() {
                    public void run() {
                        Program m = (Program) obj;
                        adapter.remove(m);
                        startDelayedAdapterUpdate();
                    }
                });
                break;
            case "eventUpdate":
                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.update((Program) obj);
                        startDelayedAdapterUpdate();
                    }
                });
                break;
            case "dvrEntryDelete":
                runOnUiThread(new Runnable() {
                    public void run() {
                        Program m = (Program) obj;
                        adapter.remove(m);
                        startDelayedAdapterUpdate();
                    }
                });
                break;
            case "dvrEntryUpdate":
                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.update((Program) obj);
                        startDelayedAdapterUpdate();
                    }
                });
                break;
        }
    }

    @Override
    public boolean onQueryTextChange(String text) {
        if (text.length() >= 3 && app.isUnlocked()) {
            query = text;
            startDelayedAdapterUpdate();
        }
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onQueryTextSubmit(String text) {
        query = text;
        startDelayedAdapterUpdate();

        // Close the search view and show the action bar again
        searchMenuItem.collapseActionView();
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onSuggestionClick(int position) {
        // Get the text of the selected suggestion
        Cursor cursor = searchView.getSuggestionsAdapter().getCursor();
        query = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        startDelayedAdapterUpdate();

        // Close the search view and show the action bar again
        searchMenuItem.collapseActionView();
        return true;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        // Get the text of the selected suggestion
        Cursor cursor = searchView.getSuggestionsAdapter().getCursor();
        query = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        startDelayedAdapterUpdate();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Stop the service to stop loading any EPG data.
        stopService(new Intent(this, HTSService.class));
    }
}
