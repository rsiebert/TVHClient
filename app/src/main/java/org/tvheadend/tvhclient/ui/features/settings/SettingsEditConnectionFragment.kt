package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage

class SettingsEditConnectionFragment : SettingsConnectionBaseFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbarInterface.setTitle(getString(R.string.edit_connection))

        if (savedInstanceState == null) {
            settingsViewModel.loadConnectionById(settingsViewModel.connectionIdToBeEdited)
            toolbarInterface.setSubtitle(settingsViewModel.connection.name ?: "")
        }
    }

    override fun save() {
        val status = connectionValidator.isConnectionInputValid(settingsViewModel.connection)
        if (status == ConnectionValidator.ValidationStatus.SUCCESS) {
            settingsViewModel.updateConnection()
            activity.let {
                if (it is RemoveFragmentFromBackstackInterface) {
                    it.removeFragmentFromBackstack()
                }
            }
        } else {
            context?.sendSnackbarMessage(getErrorDescription(status))
        }
    }
}
