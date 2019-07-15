package org.tvheadend.tvhclient.ui.features.information

import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity

class WebViewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val fragment = WebViewFragment()
            fragment.arguments = intent.extras
            supportFragmentManager.beginTransaction().replace(R.id.main, fragment).commit()
        }
    }
}
