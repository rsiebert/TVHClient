package org.tvheadend.tvhclient.ui.features.programs

import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity

class ProgramDetailsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val fragment = ProgramDetailsFragment()
            fragment.arguments = intent.extras
            supportFragmentManager.beginTransaction().add(R.id.main, fragment).commit()
        }
    }
}
