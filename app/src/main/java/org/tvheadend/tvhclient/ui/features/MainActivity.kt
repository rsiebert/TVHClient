package org.tvheadend.tvhclient.ui.features

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.framework.*
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.data.service.SyncStateReceiver
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.features.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingDetailsFragment
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingDetailsFragment
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingDetailsFragment
import org.tvheadend.tvhclient.ui.features.epg.ProgramGuideFragment
import org.tvheadend.tvhclient.ui.features.information.StatusViewModel
import org.tvheadend.tvhclient.ui.features.navigation.NavigationDrawer
import org.tvheadend.tvhclient.ui.features.navigation.NavigationViewModel
import org.tvheadend.tvhclient.ui.features.notification.addDiskSpaceLowNotification
import org.tvheadend.tvhclient.ui.features.notification.addRunningRecordingNotification
import org.tvheadend.tvhclient.ui.features.playback.external.CastSessionManagerListener
import org.tvheadend.tvhclient.ui.features.programs.ProgramDetailsFragment
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

// TODO make the notification ids a constant in the not...utils file

class MainActivity : BaseActivity(), SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, SyncStateReceiver.Listener {

    private lateinit var navigationViewModel: NavigationViewModel
    private lateinit var statusViewModel: StatusViewModel

    private lateinit var syncProgress: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private var mediaRouteMenuItem: MenuItem? = null
    private var introductoryOverlay: IntroductoryOverlay? = null
    private var castSession: CastSession? = null
    private var castContext: CastContext? = null
    private var castStateListener: CastStateListener? = null
    private var castSessionManagerListener: SessionManagerListener<CastSession>? = null

    private lateinit var navigationDrawer: NavigationDrawer
    private lateinit var syncStateReceiver: SyncStateReceiver
    private lateinit var snackbarMessageReceiver: SnackbarMessageReceiver

    private var isUnlocked: Boolean = false
    private var isDualPane: Boolean = false

    private var searchQuery: String = ""
    private lateinit var queryTextSubmitTask: Runnable
    private val queryTextSubmitHandler = Handler()

    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        Timber.d("Initializing")
        MainApplication.component.inject(this)

        navigationViewModel = ViewModelProviders.of(this).get(NavigationViewModel::class.java)
        statusViewModel = ViewModelProviders.of(this).get(StatusViewModel::class.java)

        syncProgress = findViewById(R.id.sync_progress)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        snackbarMessageReceiver = SnackbarMessageReceiver(this)
        syncStateReceiver = SyncStateReceiver(this)
        isUnlocked = MainApplication.instance.isUnlocked
        isDualPane = findViewById<View>(R.id.details) != null

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }

        navigationDrawer = NavigationDrawer(this, savedInstanceState, toolbar, navigationViewModel, statusViewModel, isUnlocked)

        // When the activity is created it got called by the main activity. Get the initial
        // navigation menu position and show the associated fragment with it. When the device
        // was rotated just restore the position from the saved instance.
        searchQuery = savedInstanceState?.getString(SearchManager.QUERY) ?: ""

        val showCastingMiniController = isUnlocked && sharedPreferences.getBoolean("casting_minicontroller_enabled",
                resources.getBoolean(R.bool.pref_default_casting_minicontroller_enabled))
        val miniController = findViewById<View>(R.id.cast_mini_controller)
        miniController.visibleOrGone(showCastingMiniController)

        supportFragmentManager.addOnBackStackChangedListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.main)
            navigationDrawer.handleMenuSelection(fragment)
        }

        castContext = getCastContext(this)
        if (castContext != null) {
            Timber.d("Casting is available")
            castSessionManagerListener = CastSessionManagerListener(this, castSession)
            castStateListener = CastStateListener { newState ->
                Timber.d("Cast state changed to $newState")
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    showIntroductoryOverlay()
                }
            }
        } else {
            Timber.d("Casting is not available, casting will no be enabled")
        }

        // Calls the method in the fragments that will initiate the actual search
        // Disable search as you type in the epg because the results
        // will be shown in a separate fragment program list.
        queryTextSubmitTask = Runnable {
            val fragment = supportFragmentManager.findFragmentById(R.id.main)
            if (fragment is SearchRequestInterface
                    && fragment.isVisible
                    && fragment !is ProgramGuideFragment) {
                fragment.onSearchRequested(searchQuery)
            }
        }

        mainViewModel.isNetworkAvailableLiveData.observe(this, Observer { isAvailable ->
            Timber.d("Network availability changed to $isAvailable")
            connectOrReconnectToServer(isAvailable)

            if (mainViewModel.isNetworkAvailable && !isAvailable) sendSnackbarMessage(R.string.network_not_available)
            mainViewModel.isNetworkAvailable = isAvailable
            invalidateOptionsMenu()
        })

        navigationViewModel.navigationMenuId.observe(this, Observer { id ->
            handleDrawerItemSelected(id)
        })

        statusViewModel.showRunningRecordingCount.observe(this, Observer { show ->
            Timber.d("Notification of running recording count of ${statusViewModel.runningRecordingCount} shall be shown $show")
            if (show) {
                addRunningRecordingNotification(this, statusViewModel.runningRecordingCount)
            } else {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
            }
        })

        statusViewModel.showLowStorageSpace.observe(this, Observer { show ->
            Timber.d("Currently free disk space changed to ${statusViewModel.availableStorageSpace} gigabytes")
            if (show) {
                addDiskSpaceLowNotification(this, statusViewModel.availableStorageSpace)
            } else {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(2)
            }
        })

        Timber.d("Done initializing")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        var out = outState
        // add the values which need to be saved from the drawer and header to the bundle
        out = navigationDrawer.saveInstanceState(out)
        out.putString(SearchManager.QUERY, searchQuery)
        super.onSaveInstanceState(out)
    }

    public override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(syncStateReceiver, IntentFilter(SyncStateReceiver.ACTION))
        LocalBroadcastManager.getInstance(this).registerReceiver(snackbarMessageReceiver, IntentFilter(SnackbarMessageReceiver.ACTION))
    }

    public override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(syncStateReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        castContext?.let {
            return it.onDispatchVolumeKeyEventBeforeJellyBean(event) || super.dispatchKeyEvent(event)
        } ?: run {
            return super.dispatchKeyEvent(event)
        }
    }

    public override fun onResume() {
        super.onResume()
        castSession = getCastSession(this)
    }

    public override fun onPause() {
        castContext?.let {
            try {
                it.removeCastStateListener(castStateListener)
                it.sessionManager.removeSessionManagerListener(castSessionManagerListener, CastSession::class.java)
            } catch (e: IllegalStateException) {
                Timber.e(e, "Could not remove cast state listener or get cast session manager")
            }
        }
        queryTextSubmitHandler.removeCallbacks(queryTextSubmitTask)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_options_menu, menu)

        mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item)
        try {
            CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
        } catch (e: Exception) {
            Timber.e(e, "Could not setup media route button")
        }

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView

        searchView?.let {
            it.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            it.setIconifiedByDefault(true)
            it.setOnQueryTextListener(this)
            it.setOnSuggestionListener(this)

            val fragment = supportFragmentManager.findFragmentById(R.id.main)
            if (fragment is SearchRequestInterface && fragment.isVisible) {
                it.queryHint = fragment.getQueryHint()
            }
        }
        return true
    }

    private fun showIntroductoryOverlay() {
        introductoryOverlay?.remove()

        mediaRouteMenuItem?.let {
            if (it.isVisible) {
                Handler().post {
                    introductoryOverlay = IntroductoryOverlay.Builder(
                            this@MainActivity, mediaRouteMenuItem)
                            .setTitleText(getString(R.string.intro_overlay_text))
                            .setOverlayColor(R.color.primary)
                            .setSingleTime()
                            .setOnOverlayDismissedListener { introductoryOverlay = null }
                            .build()
                    introductoryOverlay?.show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && permissions.isNotEmpty()
                && permissions[0] == "android.permission.WRITE_EXTERNAL_STORAGE") {
            Timber.d("Storage permission granted")
            val fragment = supportFragmentManager.findFragmentById(if (isDualPane) R.id.details else R.id.main)
            if (fragment is DownloadPermissionGrantedInterface) {
                fragment.downloadRecording()
            }
        } else {
            Timber.d("Storage permission could not be granted")
        }
    }

    /**
     * Called when a menu item from the navigation drawer was selected. It loads
     * and shows the correct fragment or fragments depending on the selected
     * menu item.
     *
     * @param position Selected position within the menu array
     */
    private fun handleDrawerItemSelected(position: Int) {
        Timber.d("Navigation menu item selected at $position, previous position was ${navigationViewModel.previousNavigationMenuId}")
        val fragment: Fragment?
        var addFragmentToBackStack = sharedPreferences.getBoolean("navigation_history_enabled",
                resources.getBoolean(R.bool.pref_default_navigation_history_enabled))

        // Get the already created fragment when the device orientation changes. In this
        // case the saved instance is not null. This avoids recreating fragments after
        // every orientation change which would reset any saved states in these fragments.
        if (position != navigationViewModel.previousNavigationMenuId) {
            Timber.d("New navigation menu $position was selected, getting new fragment")
            fragment = navigationDrawer.getFragmentFromSelection(position)
        } else {
            Timber.d("Same navigation menu was selected, trying to get existing fragment")
            fragment = supportFragmentManager.findFragmentById(R.id.main)
            addFragmentToBackStack = false
        }

        // A new or existing main fragment shall be shown. So save the menu position so we
        // know which one was selected. Additionally remove any old details fragment in case
        // dual pane mode is active to prevent showing wrong details data.
        // Finally show the new main fragment and add it to the back stack
        // only if it is a new fragment and not an existing one.
        if (fragment != null) {
            navigationViewModel.previousNavigationMenuId = position
            if (isDualPane) {
                val detailsFragment = supportFragmentManager.findFragmentById(R.id.details)
                if (detailsFragment != null) {
                    supportFragmentManager.beginTransaction().remove(detailsFragment).commit()
                }
            }
            supportFragmentManager.beginTransaction().replace(R.id.main, fragment).let {
                if (addFragmentToBackStack) it.addToBackStack(null)
                it.commit()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        // Show certain menus not on all screens
        when (navigationViewModel.navigationMenuId.value) {
            NavigationDrawer.MENU_STATUS, NavigationDrawer.MENU_UNLOCKER, NavigationDrawer.MENU_HELP -> {
                mediaRouteMenuItem?.isVisible = false
                menu.findItem(R.id.menu_search).isVisible = false
                menu.findItem(R.id.menu_refresh).isVisible = false
                menu.findItem(R.id.menu_wol)?.isVisible = false
            }
            else -> {
                mediaRouteMenuItem?.isVisible = isUnlocked
                menu.findItem(R.id.menu_wol)?.isVisible = isUnlocked && statusViewModel.connection.isWolEnabled
            }
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        queryTextSubmitHandler.removeCallbacks(queryTextSubmitTask)
        searchMenuItem?.collapseActionView()
        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is SearchRequestInterface && fragment.isVisible) {
            fragment.onSearchRequested(query)
        }
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        searchQuery = newText
        if (newText.length >= 3) {
            Timber.d("Search query is ${newText.length} characters long, starting timer to start searching")
            queryTextSubmitHandler.removeCallbacks(queryTextSubmitTask)
            queryTextSubmitHandler.postDelayed(queryTextSubmitTask, 2000)
        }
        return true
    }

    override fun onSuggestionSelect(position: Int): Boolean {
        return false
    }

    override fun onSuggestionClick(position: Int): Boolean {
        searchMenuItem?.collapseActionView()
        // Set the search query and return true so that the onQueryTextSubmit
        // is called. This is required to pass additional data to the search activity
        val cursor = searchView?.suggestionsAdapter?.getItem(position) as Cursor?
        cursor?.let {
            val suggestion = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))
            searchView?.setQuery(suggestion, true)
        }
        return true
    }

    override fun onSyncStateChanged(state: SyncStateReceiver.State, message: String, details: String) {
        when (state) {
            SyncStateReceiver.State.CLOSED, SyncStateReceiver.State.FAILED -> {
                Timber.d("Connection failed or closed")
                sendSnackbarMessage(message)
                mainViewModel.isNetworkAvailableLiveData.postValue(false)
            }

            SyncStateReceiver.State.CONNECTING -> {
                Timber.d("Connecting")
                sendSnackbarMessage(message)
            }

            SyncStateReceiver.State.CONNECTED -> {
                Timber.d("Connected")
                sendSnackbarMessage(message)
            }

            SyncStateReceiver.State.SYNC_STARTED -> {
                Timber.d("Sync started, showing progress bar")
                syncProgress.visible()
                sendSnackbarMessage(message)
            }

            SyncStateReceiver.State.SYNC_IN_PROGRESS -> {
                Timber.d("Sync in progress, updating progress bar")
                syncProgress.visible()
            }

            SyncStateReceiver.State.SYNC_DONE -> {
                Timber.d("Sync done, hiding progress bar")
                syncProgress.gone()
                sendSnackbarMessage(message)
            }
            else -> {
            }
        }
    }

    private fun connectOrReconnectToServer(isAvailable: Boolean) {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcessInfo = activityManager.runningAppProcesses?.get(0)

        if (isAvailable
                && runningAppProcessInfo != null
                && runningAppProcessInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {

            val intent = Intent(this, HtspService::class.java)
            if (!mainViewModel.isNetworkAvailable) {
                Timber.d("Starting server because network is available again")
                intent.action = "connect"
                startService(intent)
            } else {
                Timber.d("Reconnecting to server because network is still available")
                intent.action = "reconnect"
                startService(intent)
            }
        }
    }

    override fun onBackPressed() {
        val navigationHistoryEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("navigation_history_enabled", resources.getBoolean(R.bool.pref_default_navigation_history_enabled))
        if (!navigationHistoryEnabled) {
            val fragment = supportFragmentManager.findFragmentById(R.id.main)
            if (fragment is ProgramListFragment
                    || fragment is ProgramDetailsFragment
                    || fragment is RecordingDetailsFragment
                    || fragment is SeriesRecordingDetailsFragment
                    || fragment is TimerRecordingDetailsFragment) {
                // Do not finish the activity in case any of these fragments
                // are visible which were called from the channel list fragment.
                clearSearchResultsOrPopBackStack()
            } else {
                finish()
            }
        } else {
            if (supportFragmentManager.backStackEntryCount <= 1) {
                finish()
            } else {
                // The last fragment on the stack is visible
                clearSearchResultsOrPopBackStack()
            }
        }
    }

    /**
     * Pops the back stack to go back to the previous fragment or
     * in case a search was active, clears the search results.
     * After that a new back press can finish the activity.
     */
    private fun clearSearchResultsOrPopBackStack() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is SearchRequestInterface && fragment.isVisible) {
            if (!fragment.onSearchResultsCleared()) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}
