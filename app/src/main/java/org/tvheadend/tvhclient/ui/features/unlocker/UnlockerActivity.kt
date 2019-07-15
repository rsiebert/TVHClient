package org.tvheadend.tvhclient.ui.features.unlocker

import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity

class UnlockerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val fragment = UnlockerFragment()
            val bundle = Bundle()
            bundle.putString("website", "features")
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction().add(R.id.main, fragment).commit()
        }
    }
}
