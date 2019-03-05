package org.tvheadend.tvhclient.ui.features.startup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.features.changelog.ChangeLogActivity
import org.tvheadend.tvhclient.util.LocaleUtils
import org.tvheadend.tvhclient.util.MiscUtils
import timber.log.Timber

class StartupActivity : AppCompatActivity() {
    private var showStatusFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(MiscUtils.getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        if (savedInstanceState == null) {
            // Show the full changelog if the changelog was never shown before (app version
            // name is empty) or if it was already shown and the version name is the same as
            // the one in the preferences. Otherwise show the changelog of the newest app version.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val versionName = sharedPreferences.getString("versionNameForChangelog", "")
            val showChangeLogRequired = BuildConfig.VERSION_NAME != versionName

            if (showChangeLogRequired) {
                Timber.d("Showing changelog, version name from prefs: " + versionName + ", build version from gradle: " + BuildConfig.VERSION_NAME)
                val intent = Intent(this, ChangeLogActivity::class.java)
                intent.putExtra("showFullChangelog", TextUtils.isEmpty(versionName))
                intent.putExtra("versionNameForChangelog", versionName)
                startActivityForResult(intent, 0)
            } else {
                showStatusFragment = true
            }
        }
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(LocaleUtils.onAttach(context))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
