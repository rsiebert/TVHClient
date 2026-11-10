package org.tvheadend.tvhclient.ui.features.settings


import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.util.extensions.applySystemBarInsets
import org.tvheadend.tvhclient.util.extensions.setUpEdgeToEdge
import org.tvheadend.tvhclient.util.extensions.showSnackbarMessage
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

class SettingsActivity : AppCompatActivity(), RemoveFragmentFromBackstackInterface, ToolbarInterface {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var snackbarMessageReceiver: SnackbarMessageReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setUpEdgeToEdge()
        setContentView(R.layout.misc_content_activity)

        findViewById<View>(R.id.app_bar).applySystemBarInsets(top = true)
        findViewById<View>(R.id.content_container).applySystemBarInsets(bottom = true, horizontal = true)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this) { handleBackPressed() }

        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        snackbarMessageReceiver = SnackbarMessageReceiver(settingsViewModel)

        // If the user wants to go directly to a sub setting screen like the connections
        // and not the main settings screen the setting type can be passed here
        if (savedInstanceState == null && intent.hasExtra("setting_type")) {
            settingsViewModel.setNavigationMenuId(SettingsDestination.fromId(intent.getStringExtra("setting_type")))
        }

        settingsViewModel.getNavigationMenuId().observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                Timber.d("New preference selected with id $it, replacing settings fragment")
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main, getSettingsFragment(it))
                    .addToBackStack(null)
                    .commit()
            }
        }

        settingsViewModel.currentServerStatusLiveData.observe(this) { serverStatus ->
            Timber.d("Received live data, server status has changed and is ${if (serverStatus != null) "" else "not "}available")
            if (serverStatus != null) {
                settingsViewModel.currentServerStatus = serverStatus
            }
        }

        settingsViewModel.snackbarMessageLiveData.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                this.showSnackbarMessage(it)
            }
        }

        settingsViewModel.isUnlockedLiveData.observe(this) { isUnlocked ->
            Timber.d("Received live data, isUnlocked value changed to $isUnlocked")
            settingsViewModel.isUnlocked = isUnlocked || BuildConfig.OVERRIDE_UNLOCKED
        }
    }

    public override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(snackbarMessageReceiver, IntentFilter(SnackbarMessageReceiver.SNACKBAR_ACTION))
    }

    public override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver)
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    override fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun setSubtitle(subtitle: String) {
        supportActionBar?.subtitle = subtitle
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getSettingsFragment(destination: SettingsDestination): Fragment {
        Timber.d("Getting settings fragment for destination $destination")
        return destination.fragmentFactory()
    }

    /*
     * Forwards the back press command to the fragment that implements the to handle the back press.
     * This is usually required when a confirmation dialog shall be shown before exiting the fragment.
     * Otherwise remove the last fragment from the back stack or close the activity in case only one fragment remains.
     *
     * Registered as an OnBackPressedCallback in onCreate; replaces the deprecated onBackPressed
     * override, which is no longer called from Android 16 (API 36) onwards.
     */
    private fun handleBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is BackPressedInterface && fragment.isVisible) {
            Timber.d("Calling back press in the fragment")
            fragment.onBackPressed()
        } else {
            Timber.d("Calling back press of super")
            removeFragmentFromBackstack()
        }
    }

    override fun removeFragmentFromBackstack() {
        Timber.d("Back stack count is ${supportFragmentManager.backStackEntryCount}")
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }
}
