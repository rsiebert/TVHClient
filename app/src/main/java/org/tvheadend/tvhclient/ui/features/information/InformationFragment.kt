package org.tvheadend.tvhclient.ui.features.information

import android.os.Bundle
import org.tvheadend.tvhclient.R

class InformationFragment : WebViewFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        website = "information"

        toolbarInterface.setTitle(getString(R.string.pref_information))
        toolbarInterface.setSubtitle("")
    }
}