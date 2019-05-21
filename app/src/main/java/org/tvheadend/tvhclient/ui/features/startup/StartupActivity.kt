package org.tvheadend.tvhclient.ui.features.startup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.features.changelog.ChangeLogActivity
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

class StartupActivity : BaseActivity() {

    private var showStatusFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        // Get the toolbar so that the fragments can set the title
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        if (savedInstanceState == null) {
            // Show the full changelog if the changelog was never shown before (app version
            // name is empty) or if it was already shown and the version name is the same as
            // the one in the preferences. Otherwise show the changelog of the newest app version.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val versionName = sharedPreferences.getString("versionNameForChangelog", "")
            val showChangeLogRequired = BuildConfig.VERSION_NAME != versionName

            if (showChangeLogRequired) {
                Timber.d("Showing changelog, version name from prefs is $versionName, build version from gradle is ${BuildConfig.VERSION_NAME}")
                val intent = Intent(this, ChangeLogActivity::class.java)
                intent.putExtra("showFullChangelog", versionName.isNullOrEmpty())
                intent.putExtra("versionNameForChangelog", versionName)
                startActivityForResult(intent, 0)
            } else {
                showStatusFragment = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            showStatusFragment = true
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (showStatusFragment) {
            val fragment = StartupFragment()
            fragment.arguments = intent.extras
            supportFragmentManager.beginTransaction().replace(R.id.main, fragment).commit()
        }
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is BackPressedInterface) {
            fragment.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }
}
