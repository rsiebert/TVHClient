package org.tvheadend.tvhclient.ui.features.information

import android.os.Bundle
import org.tvheadend.tvhclient.R

class PrivacyPolicyFragment : WebViewFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        website = "privacy_policy"

        toolbarInterface.setTitle(getString(R.string.pref_privacy_policy))
        toolbarInterface.setSubtitle("")
    }
}