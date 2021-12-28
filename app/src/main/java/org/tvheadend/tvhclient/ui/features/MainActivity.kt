package org.tvheadend.tvhclient.ui.features

import android.app.ActivityManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.os.*
import android.provider.SearchRecentSuggestions
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.framework.*
import org.tvheadend.api.AuthenticationFailureReason
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionFailureReason
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.service.SyncState
import org.tvheadend.tvhclient.service.SyncStateReceiver
import org.tvheadend.tvhclient.service.SyncStateResult
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.*
import org.tvheadend.tvhclient.ui.features.dvr.recordings.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.ui.features.information.ChangeLogFragment
import org.tvheadend.tvhclient.ui.features.information.PrivacyPolicyFragment
import org.tvheadend.tvhclient.ui.features.information.StartupPrivacyPolicyFragment
import org.tvheadend.tvhclient.ui.features.information.StatusViewModel
import org.tvheadend.tvhclient.ui.features.navigation.NavigationDrawer
import org.tvheadend.tvhclient.ui.features.navigation.NavigationViewModel
import org.tvheadend.tvhclient.ui.features.playback.external.CastSessionManagerListener
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment
import org.tvheadend.tvhclient.ui.features.startup.StartupFragment
import org.tvheadend.tvhclient.util.extensions.*
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

class MainActivity : AppCompatActivity(), ToolbarInterface, LayoutControlInterface, SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, SyncStateReceiver.Listener, View.OnFocusChangeListener {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var navigationViewModel: NavigationViewModel
    private lateinit var statusViewModel: StatusViewModel
    private lateinit var baseViewModel: BaseViewModel

    private lateinit var snackbarMessageReceiver: SnackbarMessageReceiver
    private lateinit var networkStatusReceiver: NetworkStatusReceiver
    private lateinit var toolbar: Toolbar

    private lateinit var syncProgress: ProgressBar

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

    private var isUnlocked: Boolean = false
    private var isDualPane: Boolean = false

    private lateinit var queryTextSubmitTask: Runnable
    private val delayedQueryTextSubmitHandler = Handler(Looper.getMainLooper())

    private lateinit var miniController: View

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        baseViewModel = ViewModelProvider(this)[BaseViewModel::class.java]
        navigationViewModel = ViewModelProvider(this)[NavigationViewModel::class.java]
        statusViewModel = ViewModelProvider(this)[StatusViewModel::class.java]

        snackbarMessageReceiver = SnackbarMessageReceiver(baseViewModel)
        networkStatusReceiver = NetworkStatusReceiver(baseViewModel)

        // Reset the search in case the main activity was called for the first
        // time or when we came back from another like the search activity
        if (savedInstanceState == null) {
            Timber.d("Saved instance is null")
            baseViewModel.clearSearchQuery()
            baseViewModel.removeFragmentWhenSearchIsDone = false

            Timber.d("Showing startup fragment")
            supportFragmentManager.beginTransaction()
                    .replace(R.id.main, StartupFragment())
                    .addToBackStack(null)
                    .commit()

            val showPrivacyPolicyRequired = sharedPreferences.getBoolean("showPrivacyPolicy", true)
            Timber.d("Privacy policy needs to be displayed $showPrivacyPolicyRequired")
            if (showPrivacyPolicyRequired) {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.main, StartupPrivacyPolicyFragment())
                        .addToBackStack(null)
                        .commit()
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }

            // Show the full changelog if the changelog was never shown before (app version
            // name is empty) or if it was already shown and the version name is the same as
            // the one in the preferences. Otherwise show the changelog of the newest app version.
            val versionName = sharedPreferences.getString("versionNameForChangelog", "") ?: ""
            val showChangeLogRequired = BuildConfig.VERSION_NAME != versionName
            Timber.d("Version name from prefs is $versionName, build version from gradle is ${BuildConfig.VERSION_NAME}")

            if (showChangeLogRequired) {
                Timber.d("Showing changelog")
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportFragmentManager.beginTransaction()
                        .replace(R.id.main, ChangeLogFragment.newInstance(versionName, false))
                        .addToBackStack(null)
                        .commit()
            }
        }

        syncProgress = findViewById(R.id.sync_progress)
        syncStateReceiver = SyncStateReceiver(this)
        isDualPane = findViewById<View>(R.id.details) != null

        miniController = findViewById(R.id.cast_mini_controller)
        miniController.gone()

        navigationDrawer = NavigationDrawer(this, savedInstanceState, toolbar, navigationViewModel, statusViewModel, isDualPane)

        supportFragmentManager.addOnBackStackChangedListener {

            // Hide the navigation menu and show an arrow icon to allow going back for certain fragments
            // Otherwise show the navigation menu again and invalidate any menus and update the toolbar.
            val fragment = supportFragmentManager.findFragmentById(R.id.main)
            if (fragment is HideNavigationDrawerInterface) {
                navigationDrawer.enableDrawerIndicator(false)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            } else {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                navigationDrawer.enableDrawerIndicator(true)
                invalidateOptionsMenu()
            }

            navigationDrawer.setSelectedNavigationDrawerMenuFromFragmentType(supportFragmentManager.findFragmentById(R.id.main))
        }

        castContext = this.getCastContext()
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
            Timber.d("Delayed search timer elapsed, starting search")
        }

        baseViewModel.startupCompleteLiveData.observe(this,  { event ->
            val isComplete = event.getContentIfNotHandled() ?: false
            Timber.d("Received live data, startup complete value changed to $isComplete")
            if (isComplete) {
                startupIsCompleteObserveMainLiveData()
            }
        })

        // In case an orientation change occurred assume the startup is complete and start observing the
        // other required live data. Without that navigation and connectivity changes would not work anymore
        if (savedInstanceState != null) {
            Timber.d("Orientation change occurred")
            baseViewModel.setStartupComplete(true)
        }
    }

    private fun startupIsCompleteObserveMainLiveData() {
        Timber.d("Startup complete, observing other required live data")

        baseViewModel.networkStatusLiveData.observe(this,  { event ->
            event.getContentIfNotHandled()?.let {
                Timber.d("Network status changed to $it")
                connectToServer(it)
            }
        })

        baseViewModel.connectionToServerAvailableLiveData.observe(this,  { isAvailable ->
            Timber.d("Connection to server availability changed to $isAvailable")
            invalidateOptionsMenu()
            statusViewModel.stopDiskSpaceUpdateHandler()
            if (isAvailable) {
                statusViewModel.startDiskSpaceUpdateHandler()
            }
        })

        navigationViewModel.getNavigationMenuId().observe(this,  { event ->
            event.getContentIfNotHandled()?.let {
                Timber.d("Navigation menu id changed to $it")
                baseViewModel.clearSearchQuery()
                navigationDrawer.handleDrawerItemSelected(it)
            }
        })
        statusViewModel.showRunningRecordingCount.observe(this,  { show ->
            showOrCancelNotificationProgramIsCurrentlyBeingRecorded(this, statusViewModel.runningRecordingCount, show)
        })
        statusViewModel.showLowStorageSpace.observe(this,  { show ->
            showOrCancelNotificationDiskSpaceIsLow(this, statusViewModel.availableStorageSpace, show)
        })
        baseViewModel.snackbarMessageLiveData.observe(this,  { event ->
            event.getContentIfNotHandled()?.let {
                this.showSnackbarMessage(it)
            }
        })
        baseViewModel.isUnlockedLiveData.observe(this,  { unlocked ->
            Timber.d("Received live data, unlocked changed to $unlocked")
            invalidateOptionsMenu()
            miniController.visibleOrGone(isUnlocked && sharedPreferences.getBoolean("casting_minicontroller_enabled", resources.getBoolean(R.bool.pref_default_casting_minicontroller_enabled)))
        })

        Timber.d("Done initializing")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        var out = outState
        // add the values which need to be saved from the drawer and header to the bundle
        out = navigationDrawer.saveInstanceState(out)
        super.onSaveInstanceState(out)
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    public override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(syncStateReceiver, IntentFilter(SyncStateReceiver.ACTION))
        LocalBroadcastManager.getInstance(this).registerReceiver(snackbarMessageReceiver, IntentFilter(SnackbarMessageReceiver.SNACKBAR_ACTION))
        registerReceiver(networkStatusReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
    }

    public override fun onResume() {
        super.onResume()
        castSession = this.getCastSession()
    }

    public override fun onPause() {
        castContext?.let {
            try {
                it.removeCastStateListener(castStateListener!!)
                it.sessionManager.removeSessionManagerListener(castSessionManagerListener!!, CastSession::class.java)
            } catch (e: IllegalStateException) {
                Timber.e(e, "Could not remove cast state listener or get cast session manager")
            }
        }
        delayedQueryTextSubmitHandler.removeCallbacks(queryTextSubmitTask)
        super.onPause()
    }

    public override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(syncStateReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver)
        unregisterReceiver(networkStatusReceiver)
    }

    override fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun setSubtitle(subtitle: String) {
        supportActionBar?.subtitle = subtitle
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        castContext?.let {
            return it.onDispatchVolumeKeyEventBeforeJellyBean(event) || super.dispatchKeyEvent(event)
        } ?: run {
            return super.dispatchKeyEvent(event)
        }
    }

    private fun showIntroductoryOverlay() {
        introductoryOverlay?.remove()

        mediaRouteMenuItem?.let {
            if (it.isVisible) {
                Handler(Looper.getMainLooper()).post {
                    introductoryOverlay = IntroductoryOverlay.Builder(
                            this@MainActivity, it)
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
            it.setOnQueryTextFocusChangeListener(this)

            val fragment = supportFragmentManager.findFragmentById(R.id.main)
            if (fragment is SearchRequestInterface && fragment.isVisible) {
                it.queryHint = fragment.getQueryHint()
            }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        when (navigationViewModel.currentNavigationMenuId) {
            NavigationDrawer.MENU_UNLOCKER,
            NavigationDrawer.MENU_HELP -> {
                menu.findItem(R.id.media_route_menu_item)?.isVisible = false
                menu.findItem(R.id.menu_search).isVisible = false
                menu.findItem(R.id.menu_reconnect_to_server).isVisible = false
                menu.findItem(R.id.menu_privacy_policy).isVisible = false
                menu.findItem(R.id.menu_send_wake_on_lan_packet)?.isVisible = false
            }
            NavigationDrawer.MENU_STATUS -> {
                menu.findItem(R.id.media_route_menu_item)?.isVisible = false
                menu.findItem(R.id.menu_search).isVisible = false
                menu.findItem(R.id.menu_send_wake_on_lan_packet)?.isVisible = baseViewModel.isUnlocked && baseViewModel.connection.isWolEnabled
            }
            else -> {
                menu.findItem(R.id.media_route_menu_item)?.isVisible = baseViewModel.isUnlocked
                menu.findItem(R.id.menu_send_wake_on_lan_packet)?.isVisible = baseViewModel.isUnlocked && baseViewModel.connection.isWolEnabled
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_privacy_policy -> {
                Timber.d("Showing privacy policy fragment")
                val fragment: Fragment = PrivacyPolicyFragment()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.main, fragment)
                        .addToBackStack(null)
                        .commit()
                true
            }
            R.id.menu_reconnect_to_server -> showConfirmationToReconnectToServer(this, baseViewModel)
            R.id.menu_send_wake_on_lan_packet -> {
                WakeOnLanTask(lifecycleScope, this, baseViewModel.connection)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        Timber.d("Search string $query was entered")
        delayedQueryTextSubmitHandler.removeCallbacks(queryTextSubmitTask)
        searchMenuItem?.collapseActionView()

        // Save the entered query so it will be shown later in the suggestion drpo down list
        val suggestions = SearchRecentSuggestions(this, SuggestionProvider.AUTHORITY, SuggestionProvider.MODE)
        suggestions.saveRecentQuery(query, null)

        // In case the channels or epg is currently visible, show the program list fragment.
        // It observes the search query and will perform the search and show the results.
        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is ShowProgramListFragmentInterface) {
            Timber.d("Adding program list fragment where the search will be done")
            val newFragment: Fragment = ProgramListFragment.newInstance()
            supportFragmentManager.beginTransaction().replace(R.id.main, newFragment).let {
                it.addToBackStack(null)
                it.commit()
            }
            baseViewModel.removeFragmentWhenSearchIsDone = true
        }

        Timber.d("Submitting search query to the view model")
        baseViewModel.startSearchQuery(query)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        Timber.d("Search query changed to $newText")
        delayedQueryTextSubmitHandler.removeCallbacks(queryTextSubmitTask)

        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment !is ShowProgramListFragmentInterface) {
            if (newText.length >= 3) {
                Timber.d("Search query is ${newText.length} characters long, starting timer to start searching")
                delayedQueryTextSubmitHandler.postDelayed(queryTextSubmitTask, 2000)
            }
        }
        return true
    }

    override fun onSuggestionSelect(position: Int): Boolean {
        return false
    }

    override fun onSuggestionClick(position: Int): Boolean {
        delayedQueryTextSubmitHandler.removeCallbacks(queryTextSubmitTask)
        searchMenuItem?.collapseActionView()

        val cursor = searchView?.suggestionsAdapter?.getItem(position) as Cursor?
        cursor?.let {
            val index = cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
            val suggestion = cursor.getString(index)
            searchView?.setQuery(suggestion, true)
        }
        return true
    }

    override fun onSyncStateChanged(result: SyncStateResult) {
        when (result) {
            is SyncStateResult.Connecting -> {
                when (result.reason) {
                    is ConnectionStateResult.Idle -> {
                        Timber.d("Connection is idle")
                    }
                    is ConnectionStateResult.Closed -> {
                        Timber.d("Connection failed or closed")
                        sendSnackbarMessage(getString(R.string.connection_closed))
                        Timber.d("Setting connection to server not available")
                        baseViewModel.setConnectionToServerAvailable(false)
                    }
                    is ConnectionStateResult.Connecting -> {
                        Timber.d("Connecting")
                        sendSnackbarMessage(getString(R.string.connecting_to_server))
                    }
                    is ConnectionStateResult.Connected -> {
                        Timber.d("Connected")
                        sendSnackbarMessage(getString(R.string.connected_to_server))
                        baseViewModel.setConnectionToServerAvailable(true)
                    }
                    is ConnectionStateResult.Failed -> {
                        when (result.reason.reason) {
                            is ConnectionFailureReason.Interrupted -> sendSnackbarMessage(getString(R.string.failed_during_connection_attempt))
                            is ConnectionFailureReason.UnresolvedAddress -> sendSnackbarMessage(getString(R.string.failed_to_resolve_address))
                            is ConnectionFailureReason.ConnectingToServer -> sendSnackbarMessage(getString(R.string.failed_connecting_to_server))
                            is ConnectionFailureReason.SocketException -> sendSnackbarMessage(getString(R.string.failed_opening_socket))
                            is ConnectionFailureReason.Other -> sendSnackbarMessage(getString(R.string.connection_failed))
                        }
                        Timber.d("Setting connection to server not available")
                        baseViewModel.setConnectionToServerAvailable(false)
                    }
                }
            }
            is SyncStateResult.Authenticating -> {
                if (result.reason is AuthenticationStateResult.Failed) {
                    when (result.reason.reason) {
                        is AuthenticationFailureReason.BadCredentials -> sendSnackbarMessage(getString(R.string.bad_username_or_password))
                        is AuthenticationFailureReason.Other -> sendSnackbarMessage(getString(R.string.authentication_failed))
                    }
                }
            }
            is SyncStateResult.Syncing -> {
                when (result.state) {
                    is SyncState.Started -> {
                        Timber.d("Sync started, showing progress bar")
                        syncProgress.visible()
                        sendSnackbarMessage(getString(R.string.loading_data))
                    }
                    is SyncState.InProgress -> {
                        Timber.d("Sync in progress, updating progress bar")
                        syncProgress.visible()
                        //sendSnackbarMessage(getString(R.string.saving_data))
                    }
                    is SyncState.Done -> {
                        Timber.d("Sync done, hiding progress bar")
                        syncProgress.gone()
                        sendSnackbarMessage(getString(R.string.loading_data_done))
                    }
                }
            }
        }
    }

    private fun connectToServer(status: NetworkStatus) {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcessInfo = activityManager.runningAppProcesses?.get(0)
        val intent = Intent(this, ConnectionService::class.java)

        if (runningAppProcessInfo != null
                && runningAppProcessInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {

            when (status) {
                NetworkStatus.NETWORK_IS_UP -> {
                    Timber.d("Connecting to server because network is up again")
                    intent.action = "connect"
                    startService(intent)
                }
                NetworkStatus.NETWORK_IS_STILL_UP -> {
                    Timber.d("Reconnecting to server because network is still up")
                    intent.action = "reconnect"
                    startService(intent)
                }
                NetworkStatus.NETWORK_IS_DOWN -> {
                    Timber.d("Disconnecting from server because network is down")
                    stopService(intent)
                    Timber.d("Setting connection to server not available")
                    baseViewModel.setConnectionToServerAvailable(false)
                }
                else -> {
                    Timber.d("Network status is $status, doing nothing")
                }
            }
        }
    }

    override fun onBackPressed() {

        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is BackPressedInterface) {
            fragment.onBackPressed()
            return
        }

        val navigationHistoryEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("navigation_history_enabled", resources.getBoolean(R.bool.pref_default_navigation_history_enabled))
        if (!navigationHistoryEnabled) {
            // The following fragments can be called from the channel list fragment.
            // So do not finish the activity in case any of these fragments are visible
            // but pop the back stack so that the channel list is shown again.
            when (supportFragmentManager.findFragmentById(R.id.main)) {
                is ClearSearchResultsOrPopBackStackInterface -> clearSearchResultsOrPopBackStack()
                else -> finish()
            }
        } else {
            // Only finish the activity when the status fragment and
            // another fragment are the last two fragments on the back stack.
            if (supportFragmentManager.backStackEntryCount <= 2) {
                finish()
            } else {
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
        if (baseViewModel.isSearchActive) {
            Timber.d("Clearing search result")
            baseViewModel.clearSearchQuery()

            if (baseViewModel.removeFragmentWhenSearchIsDone) {
                Timber.d("Removing current fragment because flag was set")
                baseViewModel.removeFragmentWhenSearchIsDone = false
                super.onBackPressed()
                navigationViewModel.setSelectedMenuItemId(navigationDrawer.getSelectedMenu())
            }
        } else {
            Timber.d("Removing current fragment")
            super.onBackPressed()
            navigationViewModel.setSelectedMenuItemId(navigationDrawer.getSelectedMenu())
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (Build.VERSION.SDK_INT in 21..25) {
            return
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        baseViewModel.searchViewHasFocus = hasFocus
    }

    override fun enableSingleScreenLayout() {
        Timber.d("Dual pane is not active, hiding details layout")
        val mainFrameLayout: FrameLayout = findViewById(R.id.main)
        val detailsFrameLayout: FrameLayout? = findViewById(R.id.details)
        detailsFrameLayout?.gone()
        mainFrameLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f)
    }

    override fun enableDualScreenLayout() {
        Timber.d("Dual pane is active, showing details layout")
        val mainFrameLayout: FrameLayout = findViewById(R.id.main)
        val detailsFrameLayout: FrameLayout? = findViewById(R.id.details)
        detailsFrameLayout?.visible()
        mainFrameLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.65f)
    }

    override fun forceSingleScreenLayout() {
        enableSingleScreenLayout()
    }
}
