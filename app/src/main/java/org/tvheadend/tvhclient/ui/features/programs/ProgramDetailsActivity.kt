package org.tvheadend.tvhclient.ui.features.programs

import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity

class ProgramDetailsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val fragment = ProgramDetailsFragment.newInstance(
                    intent.getIntExtra("eventId", 0),
                    intent.getIntExtra("channelId", 0))
            supportFragmentManager.beginTransaction().add(R.id.main, fragment).commit()
        }
    }
}
