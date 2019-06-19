package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import org.tvheadend.tvhclient.R

class SettingsEditConnectionFragment : SettingsConnectionBaseFragment() {

    private var connectionId: Int = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbarInterface.setTitle(getString(R.string.edit_connection))

        if (savedInstanceState == null) {
            connectionId = arguments?.getInt("connection_id", -1) ?: -1
        } else {
            connectionId = savedInstanceState.getInt("connection_id")
        }

        connection = settingsViewModel.getConnectionById(connectionId)
    }

    override fun save() {
        settingsViewModel.updateConnection(connection)
        activity?.finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("connection_id", connectionId)
        super.onSaveInstanceState(outState)
    }
}
