package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle

import org.tvheadend.tvhclient.R

class SettingsPlaybackFragment : BasePreferenceFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(getString(R.string.playback))
        toolbarInterface.setSubtitle("")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_playback, rootKey)
    }
}
