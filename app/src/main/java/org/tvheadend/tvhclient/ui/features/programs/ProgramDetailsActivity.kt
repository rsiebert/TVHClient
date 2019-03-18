package org.tvheadend.tvhclient.ui.features.programs

import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.util.getThemeId

class ProgramDetailsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val fragment = ProgramDetailsFragment()
            fragment.arguments = intent.extras
            supportFragmentManager.beginTransaction().add(R.id.main, fragment).commit()
        }
    }
}
