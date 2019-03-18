package org.tvheadend.tvhclient.ui.features.unlocker

import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.util.getThemeId

class UnlockerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val fragment = UnlockerFragment()
            val bundle = Bundle()
            bundle.putString("website", "features")
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction().add(R.id.main, fragment).commit()
        }
    }
}
