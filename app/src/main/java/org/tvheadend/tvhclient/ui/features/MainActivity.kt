package org.tvheadend.tvhclient.ui.features

import android.Manifest
import android.app.ActivityManager
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.provider.SearchRecentSuggestions
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.cast.MediaRouteButtonFactory
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.google.common.util.concurrent.ListenableFuture
import org.tvheadend.api.AuthenticationFailureReason
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionFailureReason
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.cast.CastPlaybackService
import org.tvheadend.tvhclient.cast.CastSessionState
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.service.SyncState
import org.tvheadend.tvhclient.service.SyncStateReceiver
import org.tvheadend.tvhclient.service.SyncStateResult
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.ui.common.NetworkStatusReceiver
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import org.tvheadend.tvhclient.ui.common.SuggestionProvider
import org.tvheadend.tvhclient.ui.common.WakeOnLanTask
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.HideNavigationDrawerInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.common.interfaces.SearchRequestInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ShowProgramListFragmentInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.ui.common.showConfirmationToReconnectToServer
import org.tvheadend.tvhclient.ui.common.showOrCancelNotificationDiskSpaceIsLow
import org.tvheadend.tvhclient.ui.common.showOrCancelNotificationProgramIsCurrentlyBeingRecorded
import org.tvheadend.tvhclient.ui.features.dvr.recordings.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.ui.features.information.ChangeLogFragment
import org.tvheadend.tvhclient.ui.features.information.PrivacyPolicyFragment
import org.tvheadend.tvhclient.ui.features.information.StartupPrivacyPolicyFragment
import org.tvheadend.tvhclient.ui.features.information.StatusViewModel
import org.tvheadend.tvhclient.ui.features.navigation.NavDestination
import org.tvheadend.tvhclient.ui.features.navigation.NavigationDrawer
import org.tvheadend.tvhclient.ui.features.navigation.NavigationViewModel
import org.tvheadend.tvhclient.ui.features.playback.external.ExpandedControlsActivity
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment
import org.tvheadend.tvhclient.ui.features.startup.StartupFragment
import org.tvheadend.tvhclient.util.extensions.applySystemBarInsets
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.util.extensions.setUpEdgeToEdge
import org.tvheadend.tvhclient.util.extensions.showSnackbarMessage
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
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
    private var castControllerFuture: ListenableFuture<MediaController>? = null
    private var castController: MediaController? = null
    private var castSessionStateListener: (() -> Unit)? = null

    private lateinit var navigationDrawer: NavigationDrawer
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var syncStateReceiver: SyncStateReceiver

    private var isDualPane: Boolean = false

    private lateinit var miniController: View

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))

        if (BuildConfig.DEBUG) {
            // penaltyLog only, deliberately. These detectors report from a GC callback rather
            // than at the offending call site, so a death penalty would kill the process at an
            // arbitrary moment that is hard to attribute to the leak. The logged report still
            // carries the allocation stack trace that points at the culprit.
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build())
        }
        super.onCreate(savedInstanceState)
        setUpEdgeToEdge()
        setContentView(R.layout.main_activity)

        findViewById<View>(R.id.app_bar).applySystemBarInsets(top = true)
        findViewById<View>(R.id.content_container).applySystemBarInsets(bottom = true, horizontal = true)

        onBackPressedDispatcher.addCallback(this) { handleBackPressed() }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        baseViewModel = ViewModelProvider(this)[BaseViewModel::class.java]
        navigationViewModel = ViewModelProvider(this)[NavigationViewModel::class.java]
        statusViewModel = ViewModelProvider(this)[StatusViewModel::class.java]

        // Allows billing to refresh purchases during onResume
        lifecycle.addObserver(baseViewModel.billingLifecycleObserver)

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
        miniController.setOnClickListener {
            startActivity(Intent(this, ExpandedControlsActivity::class.java))
        }
        findViewById<View>(R.id.cast_mini_controller_stop)?.setOnClickListener {
            // Stop remote playback and clear the queue so the notification and the bar go away.
            castController?.stop()
            castController?.clearMediaItems()
        }

        navigationDrawer = NavigationDrawer(this, toolbar, drawerLayout, navigationView, navigationViewModel, statusViewModel, isDualPane)

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

        baseViewModel.isUnlockedLiveData.observe(this) { isUnlocked ->
            Timber.d("Received live data, isUnlocked value changed to $isUnlocked")
            baseViewModel.isUnlocked = isUnlocked || BuildConfig.OVERRIDE_UNLOCKED
        }

        baseViewModel.startupCompleteLiveData.observe(this) { event ->
            val isComplete = event.getContentIfNotHandled() ?: false
            Timber.d("Received live data, startup complete value changed to $isComplete")
            if (isComplete) {
                startupIsCompleteObserveMainLiveData()
            }
        }

        // In case an orientation change occurred assume the startup is complete and start observing the
        // other required live data. Without that navigation and connectivity changes would not work anymore
        if (savedInstanceState != null) {
            Timber.d("Orientation change occurred")
            baseViewModel.setStartupComplete(true)
        }
    }

    private fun startupIsCompleteObserveMainLiveData() {
        Timber.d("Startup complete, observing other required live data")

        baseViewModel.networkStatusLiveData.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                Timber.d("Network status changed to $it")
                connectToServer(it)
            }
        }

        baseViewModel.connectionToServerAvailableLiveData.observe(this) { isAvailable ->
            Timber.d("Connection to server availability changed to $isAvailable")
            invalidateOptionsMenu()
            statusViewModel.stopDiskSpaceUpdateHandler()
            if (isAvailable) {
                statusViewModel.startDiskSpaceUpdateHandler()
            }
        }

        navigationViewModel.getNavigationMenuId().observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                Timber.d("Navigation menu id changed to $it")
                baseViewModel.clearSearchQuery()
                removeOnboardingFromBackStack()
                navigationDrawer.handleDrawerItemSelected(it)
            }
        }
        statusViewModel.showRunningRecordingCount.observe(this) { show ->
            showOrCancelNotificationProgramIsCurrentlyBeingRecorded(this, statusViewModel.runningRecordingCount, show)
        }
        statusViewModel.showLowStorageSpace.observe(this) { show ->
            showOrCancelNotificationDiskSpaceIsLow(this, statusViewModel.availableStorageSpace, show)
        }
        baseViewModel.snackbarMessageLiveData.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                this.showSnackbarMessage(it)
            }
        }
        baseViewModel.isUnlockedLiveData.observe(this) { unlocked ->
            Timber.d("Received live data, unlocked changed to $unlocked")
            invalidateOptionsMenu()
            updateMiniControllerVisibility(unlocked || BuildConfig.OVERRIDE_UNLOCKED)
        }

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
        registerCastSessionStateListener()
        connectCastController()
    }

    public override fun onResume() {
        super.onResume()
        // The mini controller can also be switched off in the settings, so re-evaluate it here and
        // not only when the unlocked state changes.
        updateMiniControllerVisibility(baseViewModel.isUnlocked)
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(syncStateReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver)
        unregisterReceiver(networkStatusReceiver)
        unregisterCastSessionStateListener()
        disconnectCastController()
    }

    /**
     * Connects a [MediaController] to [CastPlaybackService]. This starts the service (whose
     * CastPlayer owns the Cast session) and provides the controller that drives the mini controller.
     * Cast availability itself is published through [CastSessionState] by the service.
     */
    private fun connectCastController() {
        val sessionToken = SessionToken(this, ComponentName(this, CastPlaybackService::class.java))
        val future = MediaController.Builder(this, sessionToken).buildAsync()
        castControllerFuture = future
        future.addListener({
            if (isFinishing || isDestroyed) {
                MediaController.releaseFuture(future)
                return@addListener
            }
            try {
                val controller = future.get()
                controller.addListener(castPlayerListener)
                castController = controller
                updateMiniControllerVisibility(baseViewModel.isUnlocked)
            } catch (e: Exception) {
                Timber.e(e, "Could not connect to the cast playback service")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun disconnectCastController() {
        castController?.removeListener(castPlayerListener)
        castController = null
        castControllerFuture?.let { MediaController.releaseFuture(it) }
        castControllerFuture = null
    }

    /**
     * Keeps [CastSessionState] and the mini controller in sync with the controller's device type and
     * playlist. `onDeviceInfoChanged` replaces the deprecated CastPlayer session-availability API.
     */
    private val castPlayerListener = object : Player.Listener {
        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            CastSessionState.setCastAvailable(deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE)
        }

        override fun onEvents(player: Player, events: Player.Events) {
            updateMiniControllerVisibility(baseViewModel.isUnlocked)
        }
    }

    private fun registerCastSessionStateListener() {
        if (castSessionStateListener != null) return
        val listener: () -> Unit = {
            runOnUiThread {
                invalidateOptionsMenu()
                updateMiniControllerVisibility(baseViewModel.isUnlocked)
            }
        }
        castSessionStateListener = listener
        CastSessionState.addListener(listener)
    }

    private fun unregisterCastSessionStateListener() {
        castSessionStateListener?.let { CastSessionState.removeListener(it) }
        castSessionStateListener = null
    }

    /**
     * Shows the Cast mini controller only while a cast is active and has media, the feature is
     * unlocked and the user left the setting on. The unlocked state is passed in rather than read
     * from the view model, because the two `isUnlockedLiveData` observers must not depend on each
     * other's ordering.
     */
    private fun updateMiniControllerVisibility(isUnlocked: Boolean) {
        val hasMedia = (castController?.mediaItemCount ?: 0) > 0
        miniController.visibleOrGone(
            CastSessionState.isCastAvailable &&
                hasMedia &&
                isUnlocked &&
                sharedPreferences.getBoolean(
                    "casting_minicontroller_enabled",
                    resources.getBoolean(R.bool.pref_default_casting_minicontroller_enabled)
                )
        )
    }

    override fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun setSubtitle(subtitle: String) {
        supportActionBar?.subtitle = subtitle
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // WRITE_EXTERNAL_STORAGE is only requested on API 28 and below (it is capped at API 28 in the
        // manifest). From Android 10 the download goes straight to the public folder through scoped
        // storage, so there is no permission result to handle there.
        if (permissions.isEmpty() || permissions[0] != Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            return
        }
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
            MediaRouteButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
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

        when (navigationViewModel.currentDestination) {
            NavDestination.UNLOCKER,
            NavDestination.HELP -> {
                menu.findItem(R.id.media_route_menu_item)?.isVisible = false
                menu.findItem(R.id.menu_search).isVisible = false
                menu.findItem(R.id.menu_reconnect_to_server).isVisible = false
                menu.findItem(R.id.menu_privacy_policy).isVisible = false
                menu.findItem(R.id.menu_send_wake_on_lan_packet)?.isVisible = false
            }
            NavDestination.STATUS -> {
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
                onBackPressedDispatcher.onBackPressed()
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
        // Search-as-you-type was intentionally removed; the query is submitted explicitly and the
        // results are shown in the dedicated program list fragment.
        Timber.d("Search query changed to $newText")
        return true
    }

    override fun onSuggestionSelect(position: Int): Boolean {
        return false
    }

    override fun onSuggestionClick(position: Int): Boolean {
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

    /**
     * Handles a back press.
     *
     * Registered as an OnBackPressedCallback in onCreate so the press is intercepted through the
     * supported dispatcher API instead of the deprecated onBackPressed override, which is no
     * longer called from Android 16 (API 36) onwards.
     */
    private fun handleBackPressed() {

        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is BackPressedInterface) {
            fragment.onBackPressed()
            return
        }

        // Navigation history is always recorded, so Back pops one entry and only finishes at the
        // root destination (a single back stack entry once the onboarding screens have been removed).
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
        } else {
            clearSearchResultsOrPopBackStack()
        }
    }

    /**
     * Removes the onboarding screens (startup, privacy policy, changelog) from the back stack once
     * the first real destination is shown. They are kept on the back stack only so that
     * [StartupFragment] can be restored to finish its work; afterwards Back must not return to them.
     */
    private fun removeOnboardingFromBackStack() {
        val current = supportFragmentManager.findFragmentById(R.id.main)
        if (current is StartupFragment
                || current is StartupPrivacyPolicyFragment
                || current is ChangeLogFragment) {
            Timber.d("Removing onboarding fragment from the back stack")
            supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
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
                popBackStackOrFinish()
                navigationViewModel.setSelectedMenuItemId(navigationDrawer.getSelectedMenu())
            }
        } else {
            Timber.d("Removing current fragment")
            popBackStackOrFinish()
            navigationViewModel.setSelectedMenuItemId(navigationDrawer.getSelectedMenu())
        }
    }

    /**
     * Pops the current fragment off the back stack, or finishes the activity when there is nothing
     * left to pop. Replaces the former `super.onBackPressed()` call, which no longer works because
     * the deprecated onBackPressed override is not invoked from Android 16 (API 36) onwards.
     */
    private fun popBackStackOrFinish() {
        if (!supportFragmentManager.popBackStackImmediate()) {
            finish()
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
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f)
    }

    override fun enableDualScreenLayout() {
        Timber.d("Dual pane is active, showing details layout")
        val mainFrameLayout: FrameLayout = findViewById(R.id.main)
        val detailsFrameLayout: FrameLayout? = findViewById(R.id.details)
        detailsFrameLayout?.visible()
        mainFrameLayout.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.35f)
    }

    override fun forceSingleScreenLayout() {
        enableSingleScreenLayout()
    }
}
