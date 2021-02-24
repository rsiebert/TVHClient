package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat

import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface

class SettingsPlaybackFragment : PreferenceFragmentCompat() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
