package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import android.view.View
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage

class SettingsEditConnectionFragment : SettingsConnectionBaseFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarInterface.setTitle(getString(R.string.edit_connection))

        if (savedInstanceState == null) {
            settingsViewModel.loadConnectionById(settingsViewModel.connectionIdToBeEdited)
            toolbarInterface.setSubtitle(settingsViewModel.connectionToEdit.name ?: "")
        }
    }

    override fun save() {
        when(val result = connectionValidator.isConnectionInputValid(settingsViewModel.connectionToEdit)) {
            is ValidationResult.Success -> {
                settingsViewModel.updateConnection()
                activity.let {
                    if (it is RemoveFragmentFromBackstackInterface) {
                        it.removeFragmentFromBackstack()
                    }
                }
            }
            is ValidationResult.Failed -> {
                context?.sendSnackbarMessage(getErrorDescription(result.reason))
            }
        }
    }
}
