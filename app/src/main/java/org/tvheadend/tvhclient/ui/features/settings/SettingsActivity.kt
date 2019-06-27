package org.tvheadend.tvhclient.ui.features.settings


import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.showSnackbarMessage
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var settingType = "default"
        if (intent.hasExtra("setting_type")) {
            settingType = intent.getStringExtra("setting_type")
        }

        val fragment: Fragment?
        if (savedInstanceState == null) {
            Timber.d("Saved instance is null, creating settings fragment")
            fragment = getSettingsFragment(settingType).also { it.arguments = intent.extras }
        } else {
            Timber.d("Saved instance is not null, trying to find settings fragment")
            fragment = supportFragmentManager.findFragmentById(R.id.main)
        }

        mainViewModel.showSnackbar.observe(this, Observer { intent ->
            showSnackbarMessage(this, intent)
        })

        Timber.d("Replacing fragment")
        if (fragment != null) {
            supportFragmentManager.beginTransaction().replace(R.id.main, fragment).commit()
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
            super.onBackPressed()
        }
    }
}
