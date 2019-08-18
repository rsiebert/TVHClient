package org.tvheadend.tvhclient.ui.features.settings


import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.showSnackbarMessage
import org.tvheadend.tvhclient.ui.features.changelog.ChangeLogFragment
import org.tvheadend.tvhclient.ui.features.information.InformationFragment
import org.tvheadend.tvhclient.ui.features.information.PrivacyPolicyFragment
import org.tvheadend.tvhclient.ui.features.unlocker.UnlockerFragment
import timber.log.Timber

class SettingsActivity : BaseActivity(), RemoveFragmentFromBackstackInterface {

    lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsViewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)

        // If the user wants to go directly to a sub setting screen like the connections
        // and not the main settings screen the setting type can be passed here
        if (savedInstanceState == null && intent.hasExtra("setting_type")) {
            val id = intent.getStringExtra("setting_type")
            settingsViewModel.setNavigationMenuId(id)
        }

        settingsViewModel.getNavigationMenuId().observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let {
                Timber.d("New preference selected with id $it, replacing settings fragment")
                val fragment: Fragment = getSettingsFragment(it)
                supportFragmentManager.beginTransaction()
                        .replace(R.id.main, fragment)
                        .addToBackStack(null)
                        .commit()
            }
        })

        baseViewModel.showSnackbar.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let {
                showSnackbarMessage(this, it)
            }
        })
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
            "unlocker" -> UnlockerFragment()
            "information" -> InformationFragment()
            "privacy_policy" -> PrivacyPolicyFragment()
            "changelog" -> {
                val bundle = Bundle()
                bundle.putBoolean("showFullChangelog", true)
                ChangeLogFragment().also {
                    it.arguments = bundle
                }
            }
            else -> {
                SettingsFragment().also { it.arguments = intent.extras }
            }
        }
    }

    override fun onBackPressed() {
        // If a settings fragment is currently visible, let the fragment
        // handle the back press, otherwise the setting activity.
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
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
