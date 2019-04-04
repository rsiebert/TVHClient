package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.ui.common.sendSnackbarMessage
import timber.log.Timber

class SettingsAddConnectionFragment : SettingsConnectionBaseFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbarInterface.setTitle(getString(R.string.add_connection))
        connection = Connection()
    }

    override fun save() {
        Timber.d("Validating input before saving")
        if (!connection.isNameValid(connection.name)) {
            context?.sendSnackbarMessage(R.string.pref_name_error_invalid)
        } else if (!connection.isIpAddressValid(connection.hostname)) {
            context?.sendSnackbarMessage(R.string.pref_host_error_invalid)
        } else if (!connection.isPortValid(connection.port)) {
            context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
        } else if (!connection.isPortValid(connection.streamingPort)) {
            context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
        } else {
            appRepository.connectionData.addItem(connection)
            // Save the information in the view model that a new connection is active.
            // This will then trigger a reconnect when the user leaves the connection list screen
            if (connection.isActive) {
                viewModel.connectionHasChanged = true
            }
            activity?.finish()
        }
    }
}
