package org.tvheadend.tvhclient.ui.features.settings


import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.ui.features.information.ChangeLogFragment
import org.tvheadend.tvhclient.ui.features.information.InformationFragment
import org.tvheadend.tvhclient.ui.features.information.PrivacyPolicyFragment
import org.tvheadend.tvhclient.ui.features.unlocker.UnlockerFragment
import org.tvheadend.tvhclient.util.extensions.showSnackbarMessage
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

class SettingsActivity : AppCompatActivity(), RemoveFragmentFromBackstackInterface, ToolbarInterface {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var snackbarMessageReceiver: SnackbarMessageReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        MainApplication.component.inject(this)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        snackbarMessageReceiver = SnackbarMessageReceiver(settingsViewModel)

        // If the user wants to go directly to a sub setting screen like the connections
        // and not the main settings screen the setting type can be passed here
        if (savedInstanceState == null && intent.hasExtra("setting_type")) {
            val id = intent.getStringExtra("setting_type") ?: "default"
            settingsViewModel.setNavigationMenuId(id)
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
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getSettingsFragment(type: String): Fragment {
        Timber.d("Getting settings fragment for type '$type'")
        return when (type) {
            "list_connections" -> SettingsListConnectionsFragment()
            "add_connection" -> SettingsAddConnectionFragment()
            "edit_connection" -> SettingsEditConnectionFragment()
            "user_interface" -> SettingsUserInterfaceFragment()
            "profiles" -> SettingsProfilesFragment()
            "playback" -> SettingsPlaybackFragment()
            "advanced" -> SettingsAdvancedFragment()
            "unlocker" -> UnlockerFragment.newInstance(settingsViewModel.isUnlocked)
            "information" -> InformationFragment()
            "privacy_policy" -> PrivacyPolicyFragment()
            "changelog" -> ChangeLogFragment.newInstance(showFullChangelog = true)
            else -> SettingsFragment()
        }
    }

    /*
     * Forwards the back press command to the fragment that implements the to handle the back press.
     * This is usually required when a confirmation dialog shall be shown before exiting the fragment.
     * Otherwise remove the last fragment from the back stack or close the activity in case only one fragment remains.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
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
            super.onBackPressed()
        }
    }
}
