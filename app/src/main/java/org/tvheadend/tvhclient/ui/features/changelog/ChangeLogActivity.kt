package org.tvheadend.tvhclient.ui.features.changelog


import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.base.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.util.getThemeId

class ChangeLogActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        // Get the toolbar so that the fragments can set the title
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val fragment = ChangeLogFragment()
            fragment.arguments = intent.extras
            supportFragmentManager.beginTransaction().add(R.id.main, fragment).commit()
        }
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is BackPressedInterface) {
            fragment.onBackPressed()
        }
    }
}
