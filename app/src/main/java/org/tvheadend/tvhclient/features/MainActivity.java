package org.tvheadend.tvhclient.features;

import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
import org.tvheadend.tvhclient.features.download.DownloadPermissionGrantedInterface;
import org.tvheadend.tvhclient.features.logging.AnswersWrapper;
import org.tvheadend.tvhclient.features.navigation.NavigationDrawer;
import org.tvheadend.tvhclient.features.navigation.NavigationDrawerCallback;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.BaseActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.features.shared.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.features.streaming.external.CastSessionManagerListener;
import org.tvheadend.tvhclient.utils.MiscUtils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import timber.log.Timber;

// TODO duplicate programs are sometimes received from the server with different ids and time
// TODO what happens when no connection to the server is active and the user presses an action in a notification?

public class MainActivity extends BaseActivity implements ToolbarInterface, WakeOnLanTaskCallback, NavigationDrawerCallback, SearchView.OnQueryTextListener, SearchView.OnSuggestionListener {

    private MenuItem searchMenuItem;
    private SearchView searchView;
    private MenuItem mediaRouteMenuItem;
    private IntroductoryOverlay introductoryOverlay;
    private CastSession castSession;
    private CastContext castContext;
    private CastStateListener castStateListener;
    private SessionManagerListener<CastSession> castSessionManagerListener;

    private NavigationDrawer navigationDrawer;
    private int selectedNavigationMenuId;
    private boolean isUnlocked;
    private boolean isDualPane;

    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected AppRepository appRepository;
    private boolean isSavedInstanceStateNull;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        MiscUtils.setLanguage(this);
        ButterKnife.bind(this);

        MainApplication.getComponent().inject(this);

        isUnlocked = MainApplication.getInstance().isUnlocked();
        isDualPane = findViewById(R.id.details) != null;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        navigationDrawer = new NavigationDrawer(this, savedInstanceState, toolbar, appRepository, this);
        navigationDrawer.createHeader();
        navigationDrawer.createMenu();

        // When the activity is created it got called by the main activity. Get the initial
        // navigation menu position and show the associated fragment with it. When the device
        // was rotated just restore the position from the saved instance.
        if (savedInstanceState == null) {
            isSavedInstanceStateNull = true;
            selectedNavigationMenuId = Integer.parseInt(sharedPreferences.getString("start_screen", "0"));
        } else {
            isSavedInstanceStateNull = false;
            selectedNavigationMenuId = savedInstanceState.getInt("navigationMenuId", NavigationDrawer.MENU_CHANNELS);
        }

        boolean showCastingMiniController = isUnlocked && sharedPreferences.getBoolean("casting_minicontroller_enabled", false);
        View miniController = findViewById(R.id.cast_mini_controller);
        miniController.setVisibility(showCastingMiniController ? View.VISIBLE : View.GONE);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
            navigationDrawer.handleSelection(fragment);
        });

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) {
            Timber.d("Google API is available");

            AnswersWrapper.getInstance().logCustom(new CustomEvent("Startup")
                    .putCustomAttribute("Google API", "Available"));

            try {
                castContext = CastContext.getSharedInstance(this);
            } catch (Exception e) {
                Timber.e("Could not get cast context", e);
            }

            castSessionManagerListener = new CastSessionManagerListener(this, castSession);
            castStateListener = newState -> {
                Timber.d("Cast state changed to " + newState);
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    showIntroductoryOverlay();
                }
            };
        } else {
            Timber.d("Google API is not available, casting will no be enabled");
            AnswersWrapper.getInstance().logCustom(new CustomEvent("Startup")
                    .putCustomAttribute("Google API", "Not available"));
        }

        // Update the drawer menu so that all available menu items are
        // shown in case the recording counts have changed or the user has
        // bought the unlocked version to enable all features
        navigationDrawer.showConnectionsInDrawerHeader();
        navigationDrawer.startObservingViewModels();
        handleDrawerItemSelected(selectedNavigationMenuId);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_options_menu, menu);

        mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        try {
            CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        } catch (Exception e) {
            Timber.e("Could not setup media route button", e);
        }

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

    private void showIntroductoryOverlay() {
        if (introductoryOverlay != null) {
            introductoryOverlay.remove();
        }
        if ((mediaRouteMenuItem != null) && mediaRouteMenuItem.isVisible()) {
            new Handler().post(() -> {
                introductoryOverlay = new IntroductoryOverlay.Builder(
                        MainActivity.this, mediaRouteMenuItem)
                        .setTitleText(getString(R.string.intro_overlay_text))
                        .setOverlayColor(R.color.primary)
                        .setSingleTime()
                        .setOnOverlayDismissedListener(() -> introductoryOverlay = null)
                        .build();
                introductoryOverlay.show();
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
            if (fragment instanceof DownloadPermissionGrantedInterface) {
                ((DownloadPermissionGrantedInterface) fragment).downloadRecording();
            }
        } else {
            Timber.d("Storage permission could not be granted");
        }
    }

    /**
     * Called when a menu item from the navigation drawer was selected. It loads
     * and shows the correct fragment or fragments depending on the selected
     * menu item.
     *
     * @param position Selected position within the menu array
     */
    private void handleDrawerItemSelected(int position) {
        Fragment fragment;
        boolean addFragmentToBackStack = true;

        // Get the already created fragment when the device orientation changes. In this
        // case the saved instance is not null. This avoids recreating fragments after
        // every orientation change which would reset any saved states in these fragments.
        if (isSavedInstanceStateNull || selectedNavigationMenuId != position) {
            Timber.d("Saved instance is null or selected id has changed, creating new fragment");
            fragment = navigationDrawer.getFragmentFromSelection(position);
        } else {
            Timber.d("Saved instance is not null, trying to existing fragment");
            fragment = getSupportFragmentManager().findFragmentById(R.id.main);
            addFragmentToBackStack = false;
        }

        if (fragment != null) {
            // Save the menu position so we know which one was selected
            selectedNavigationMenuId = position;

            // Remove the old details fragment if there is one so that it is not visible when
            // the new main fragment is loaded. It takes a while until the new details
            // fragment is visible. This prevents showing wrong data when switching screens.
            if (isDualPane) {
                Fragment detailsFragment = getSupportFragmentManager().findFragmentById(R.id.details);
                if (detailsFragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .remove(detailsFragment)
                            .commit();
                }
            }

            // Show the new fragment that represents the selected menu entry.
            fragment.setArguments(getIntent().getExtras());
            FragmentTransaction fm = getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main, fragment);
            // Only add the fragment to the back stack if a new one has been created.
            // Existing fragments that were already available due to an orientation
            // change shall not be added to the back stack. This prevents having to
            // press the back key as often as the device was rotated.
            if (addFragmentToBackStack) {
                fm.addToBackStack(null);
            }
            fm.commit();

        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        switch (selectedNavigationMenuId) {
            case NavigationDrawer.MENU_STATUS:
            case NavigationDrawer.MENU_UNLOCKER:
            case NavigationDrawer.MENU_HELP:
                // Do not show these menus in one of those fragments
                mediaRouteMenuItem.setVisible(false);
                menu.findItem(R.id.menu_search).setVisible(false);
                menu.findItem(R.id.menu_refresh).setVisible(false);
                break;
            default:
                mediaRouteMenuItem.setVisible(isUnlocked);
                break;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // add the values which need to be saved from the drawer and header to the bundle
        outState = navigationDrawer.saveInstanceState(outState);
        outState.putInt("navigationMenuId", selectedNavigationMenuId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void notify(String message) {
        if (getCurrentFocus() != null) {
            Snackbar.make(getCurrentFocus(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onNavigationMenuSelected(int id) {
        Timber.d("Newly selected menu id is " + id + ", current selection id is " + selectedNavigationMenuId);
        if (selectedNavigationMenuId != id) {
            handleDrawerItemSelected(id);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        AnswersWrapper.getInstance().logSearch(new SearchEvent().putQuery(query));

        searchMenuItem.collapseActionView();
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
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

    @Override
    public void onBackPressed() {
        Timber.d("Back pressed");
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment.isAdded() && fragment instanceof SearchRequestInterface) {
            Timber.d("Found fragment");
            if (!((SearchRequestInterface) fragment).onSearchResultsCleared()) {
                Timber.d("Search results were not cleared");
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }
}
