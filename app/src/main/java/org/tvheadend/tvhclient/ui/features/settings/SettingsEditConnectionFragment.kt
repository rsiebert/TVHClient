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
            toolbarInterface.setSubtitle(settingsViewModel.connectionToEdit.name ?: "")
        }
    }

    override fun save() {
        when (val state = connectionValidator.isConnectionInputValid(settingsViewModel.connectionToEdit)) {
            is ConnectionValidator.ValidationState.Success -> {
                settingsViewModel.updateConnection()
                activity.let {
                    if (it is RemoveFragmentFromBackstackInterface) {
                        it.removeFragmentFromBackstack()
                    }
                }
            }
            is ConnectionValidator.ValidationState.Error -> {
                context?.sendSnackbarMessage(getErrorDescription(state.reason))
            }
        }
    }
}
