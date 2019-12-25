package org.tvheadend.tvhclient.ui.features.startup

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.information.ChangeLogFragment
import org.tvheadend.tvhclient.ui.features.information.StartupPrivacyPolicyFragment
import org.tvheadend.tvhclient.ui.features.settings.RemoveFragmentFromBackstackInterface
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber
import javax.inject.Inject

class StartupActivity : AppCompatActivity(), ToolbarInterface, RemoveFragmentFromBackstackInterface {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        MainApplication.component.inject(this)

        if (savedInstanceState == null) {
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
            // The changelog fragment was removed and the startup fragment
            // is only fragment in the back stack, disable the home button
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun setSubtitle(subtitle: String) {
        supportActionBar?.subtitle = subtitle
    }
}
