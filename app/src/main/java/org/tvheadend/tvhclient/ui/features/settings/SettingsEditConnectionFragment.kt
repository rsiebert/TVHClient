package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle

import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection

class SettingsEditConnectionFragment : SettingsConnectionBaseFragment() {

    private var connectionId: Int = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbarInterface.setTitle(getString(R.string.edit_connection))

        if (savedInstanceState == null) {
            connectionValuesChanged = false
            connectionId = arguments?.getInt("connection_id", -1) ?: -1
        } else {
            connectionId = savedInstanceState.getInt("connection_id")
            connectionValuesChanged = savedInstanceState.getBoolean("connection_values_changed")
        }

        connection = appRepository.connectionData.getItemById(connectionId) ?: Connection()
    }

    override fun save() {
        appRepository.connectionData.updateItem(connection)
        viewModel.connectionHasChanged = connectionValuesChanged
        activity?.finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("connection_id", connectionId)
        outState.putBoolean("connection_values_changed", connectionValuesChanged)
        super.onSaveInstanceState(outState)
    }
}
