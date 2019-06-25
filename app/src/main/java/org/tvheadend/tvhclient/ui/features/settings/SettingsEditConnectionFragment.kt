package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import org.tvheadend.tvhclient.R

class SettingsEditConnectionFragment : SettingsConnectionBaseFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbarInterface.setTitle(getString(R.string.edit_connection))

        if (savedInstanceState == null) {
            settingsViewModel.loadConnectionById(arguments?.getInt("connection_id", -1) ?: -1)
        }
    }

    override fun save() {
        if (isConnectionInputValid(settingsViewModel.connection)) {
            settingsViewModel.updateConnection()
            activity?.finish()
        }
    }
}
