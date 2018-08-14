package org.tvheadend.tvhclient.features;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.SearchEvent;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.data.service.EpgSyncStatusCallback;
import org.tvheadend.tvhclient.data.service.EpgSyncStatusReceiver;
import org.tvheadend.tvhclient.data.service.EpgSyncTaskState;
import org.tvheadend.tvhclient.features.download.DownloadPermissionGrantedInterface;
import org.tvheadend.tvhclient.features.playback.CastSessionManagerListener;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.BaseActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkAvailabilityChangedInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.features.shared.receivers.SnackbarMessageReceiver;
import org.tvheadend.tvhclient.utils.MiscUtils;

import javax.inject.Inject;

import timber.log.Timber;

// TODO brown genre color is not shown in the genre color info dialog
// TODO when sorting channels by number, consider minor major channel numbers
// TODO casting needs rework
// TODO check for gmtoffset
// TODO move the conversion from ms to s or minutes from the intents into the entity getter and setter
// TODO add option in menu to show file missing recordings
// TODO rotating program list is then messed up
// TODO removing recording in program list it does not reset state
// TODO network connectivity change is messing up the menu items
// TODO when searching the selected item in the nav drawer is wrong
// TODO give up after x reconnect retries
// TODO reschedule work when not successful
// TODO epg genre colors
// TODO epg search in epg
// TODO navigation count increases when done then the number is correct
// TODO show recording state in details view

public class MainActivity extends BaseActivity implements SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, ToolbarInterface, EpgSyncStatusCallback {

    private MenuItem searchMenuItem;
    private SearchView searchView;
    private MenuItem mediaRouteMenuItem;
    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected AppRepository appRepository;
    protected boolean isUnlocked;
    protected boolean isDualPane;
    protected Toolbar toolbar;

    private IntroductoryOverlay introductoryOverlay;
    private CastSession castSession;
    private CastContext castContext;
    private CastStateListener castStateListener;
    private SessionManagerListener<CastSession> castSessionManagerListener;
    private EpgSyncStatusReceiver epgSyncStatusReceiver;
    private SnackbarMessageReceiver snackbarMessageReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        MiscUtils.setLanguage(this);

        MainApplication.getComponent().inject(this);

        epgSyncStatusReceiver = new EpgSyncStatusReceiver(this);
        snackbarMessageReceiver = new SnackbarMessageReceiver(this);

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) {
            Timber.d("Google API is available");
            Answers.getInstance().logCustom(new CustomEvent("Startup")
                    .putCustomAttribute("Google API", "Available"));

            castContext = CastContext.getSharedInstance(this);
            castSessionManagerListener = new CastSessionManagerListener(this, castSession);
            castStateListener = new CastStateListener() {
                @Override
                public void onCastStateChanged(int newState) {
                    if (newState != CastState.NO_DEVICES_AVAILABLE) {
                        showIntroductoryOverlay();
                    }
                }
            };
        } else {
            Timber.d("Google API is not available, casting will no be enabled");
            Answers.getInstance().logCustom(new CustomEvent("Startup")
                    .putCustomAttribute("Google API", "Not available"));
        }

        isDualPane = findViewById(R.id.details) != null;

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        isUnlocked = MainApplication.getInstance().isUnlocked();
        boolean showCastingMiniController = isUnlocked && sharedPreferences.getBoolean("casting_minicontroller_enabled", false);
        View miniController = findViewById(R.id.cast_mini_controller);
        miniController.setVisibility(showCastingMiniController ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (castContext != null) {
            return castContext.onDispatchVolumeKeyEventBeforeJellyBean(event) || super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        if (castContext != null) {
            castContext.addCastStateListener(castStateListener);
            castContext.getSessionManager().addSessionManagerListener(castSessionManagerListener, CastSession.class);
            if (castSession == null) {
                castSession = CastContext.getSharedInstance(this).getSessionManager().getCurrentCastSession();
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (castContext != null) {
            castContext.removeCastStateListener(castStateListener);
            castContext.getSessionManager().removeSessionManagerListener(castSessionManagerListener, CastSession.class);
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(snackbarMessageReceiver, new IntentFilter(SnackbarMessageReceiver.ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(epgSyncStatusReceiver, new IntentFilter(EpgSyncStatusReceiver.ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(epgSyncStatusReceiver);
    }

    @Override
    public void setTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void setSubtitle(String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mediaRouteMenuItem.setVisible(isUnlocked);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_options_menu, menu);

        mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
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
    public boolean onQueryTextSubmit(String query) {
        Answers.getInstance().logSearch(new SearchEvent().putQuery(query));

        searchMenuItem.collapseActionView();
        android.support.v4.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment.isAdded() && fragment instanceof SearchRequestInterface) {
            ((SearchRequestInterface) fragment).onSearchRequested(query);
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        searchMenuItem.collapseActionView();
        // Set the search query and return true so that the onQueryTextSubmit
        // is called. This is required to pass additional data to the search activity
        Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
        String suggestion = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        searchView.setQuery(suggestion, true);
        return true;
    }

    private void showIntroductoryOverlay() {
        if (introductoryOverlay != null) {
            introductoryOverlay.remove();
        }
        if ((mediaRouteMenuItem != null) && mediaRouteMenuItem.isVisible()) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    introductoryOverlay = new IntroductoryOverlay.Builder(
                            MainActivity.this, mediaRouteMenuItem)
                            .setTitleText(getString(R.string.intro_overlay_text))
                            .setOverlayColor(R.color.primary)
                            .setSingleTime()
                            .setOnOverlayDismissedListener(
                                    new IntroductoryOverlay.OnOverlayDismissedListener() {
                                        @Override
                                        public void onOverlayDismissed() {
                                            introductoryOverlay = null;
                                        }
                                    })
                            .build();
                    introductoryOverlay.show();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                && permissions[0].equals("android.permission.WRITE_EXTERNAL_STORAGE")) {

            Timber.d("Storage permission granted");
            Fragment fragment = getSupportFragmentManager().findFragmentById(isDualPane ? R.id.details : R.id.main);
            if (fragment != null && fragment instanceof DownloadPermissionGrantedInterface) {
                ((DownloadPermissionGrantedInterface) fragment).downloadRecording();
            }
        }
    }

    @Override
    public void onEpgTaskStateChanged(EpgSyncTaskState state) {
        Timber.d("Epg task state changed, message is " + state.getMessage());
        switch (state.getState()) {
            case FAILED:
                if (getCurrentFocus() != null) {
                    Snackbar.make(getCurrentFocus(), state.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                isNetworkAvailable = false;
                onNetworkAvailabilityChanged(false);
                break;
            // Show a message that the sync is in progress or
            // the connection to the server has not been fully
            // established or that the loading is done.
            case START:
            case LOADING:
            case DONE:
                if (getCurrentFocus() != null) {
                    Snackbar.make(getCurrentFocus(), state.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                break;
        }
    }

    protected void onNetworkAvailabilityChanged(boolean isNetworkAvailable) {
        if (isNetworkAvailable) {
            Timber.d("Network is available, starting worker to periodically ping server");
            startService(new Intent(this, EpgSyncService.class).setAction("getStatus"));
        } else {
            Timber.d("Network is not available anymore");
            if (getCurrentFocus() != null) {
                Snackbar.make(getCurrentFocus(), "No connection to server.", Snackbar.LENGTH_SHORT).show();
            }
            stopService(new Intent(this, EpgSyncService.class));
        }

        // The fragment on the main frame layout is already informed from the base activity
        if (isDualPane) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.details);
            if (fragment != null && fragment instanceof NetworkAvailabilityChangedInterface) {
                ((NetworkAvailabilityChangedInterface) fragment).onNetworkAvailabilityChanged(isNetworkAvailable);
            }
        }
    }
}
