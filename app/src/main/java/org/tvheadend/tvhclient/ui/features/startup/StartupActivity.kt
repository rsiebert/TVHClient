package org.tvheadend.tvhclient.ui.features.startup

import android.os.Bundle
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.features.changelog.ChangeLogFragment
import org.tvheadend.tvhclient.ui.features.settings.RemoveFragmentFromBackstackInterface
import org.tvheadend.tvhclient.util.MigrateUtils
import timber.log.Timber

class StartupActivity : BaseActivity(), RemoveFragmentFromBackstackInterface {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Migrates existing connections from the old database to the new room database.
        // Migrates existing preferences or remove old ones before starting the actual application
        MigrateUtils(appContext, appRepository, sharedPreferences).doMigrate()

        if (savedInstanceState == null) {

            supportActionBar?.setDisplayHomeAsUpEnabled(false)

            supportFragmentManager.beginTransaction()
                    .replace(R.id.main, StartupFragment())
                    .addToBackStack(null)
                    .commit()

            // Show the full changelog if the changelog was never shown before (app version
            // name is empty) or if it was already shown and the version name is the same as
            // the one in the preferences. Otherwise show the changelog of the newest app version.
            val versionName = sharedPreferences.getString("versionNameForChangelog", "")
            val showChangeLogRequired = BuildConfig.VERSION_NAME != versionName

            if (showChangeLogRequired) {
                Timber.d("Showing changelog, version name from prefs is $versionName, build version from gradle is ${BuildConfig.VERSION_NAME}")

                val bundle = Bundle()
                bundle.putBoolean("showFullChangelog", true)
                bundle.putString("versionNameForChangelog", versionName)

                val fragment = ChangeLogFragment()
                fragment.arguments = bundle

                supportFragmentManager.beginTransaction()
                        .replace(R.id.main, fragment)
                        .addToBackStack(null)
                        .commit()
            }
        }
    }

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
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
