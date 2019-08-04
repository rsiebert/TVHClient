package org.tvheadend.tvhclient.ui.features.startup

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.changelog.ChangeLogFragment
import org.tvheadend.tvhclient.ui.features.settings.RemoveFragmentFromBackstackInterface
import timber.log.Timber
import javax.inject.Inject

class StartupActivity : AppCompatActivity(), ToolbarInterface, RemoveFragmentFromBackstackInterface {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.CustomTheme_Light)
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

            // Show the full changelog if the changelog was never shown before (app version
            // name is empty) or if it was already shown and the version name is the same as
            // the one in the preferences. Otherwise show the changelog of the newest app version.
            val versionName = sharedPreferences.getString("versionNameForChangelog", "")
            val showChangeLogRequired = BuildConfig.VERSION_NAME != versionName
            Timber.d("Version name from prefs is $versionName, build version from gradle is ${BuildConfig.VERSION_NAME}")

            if (showChangeLogRequired) {
                Timber.d("Showing changelog")
                supportActionBar?.setDisplayHomeAsUpEnabled(true)

                val bundle = Bundle()
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
