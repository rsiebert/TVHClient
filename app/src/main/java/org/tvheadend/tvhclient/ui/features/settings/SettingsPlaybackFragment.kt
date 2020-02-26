package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface

class SettingsPlaybackFragment : PreferenceFragmentCompat() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            val toolbarInterface = activity as ToolbarInterface
            toolbarInterface.setTitle(getString(R.string.playback))
            toolbarInterface.setSubtitle("")
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_playback, rootKey)
    }
}
