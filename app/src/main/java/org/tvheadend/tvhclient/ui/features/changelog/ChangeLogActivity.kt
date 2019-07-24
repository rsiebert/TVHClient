package org.tvheadend.tvhclient.ui.features.changelog


import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.features.settings.RemoveFragmentFromBackstackInterface

class ChangeLogActivity : BaseActivity(), RemoveFragmentFromBackstackInterface {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    override fun removeFragmentFromBackstack() {
        super.onBackPressed()
    }
}
