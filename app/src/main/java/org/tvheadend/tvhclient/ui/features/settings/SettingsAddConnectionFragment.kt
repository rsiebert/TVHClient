package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import org.tvheadend.tvhclient.R

class SettingsAddConnectionFragment : SettingsConnectionBaseFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbarInterface.setTitle(getString(R.string.add_connection))

        if (savedInstanceState == null) {
            settingsViewModel.createNewConnection()
        }
    }

    override fun save() {
        if (isConnectionInputValid(settingsViewModel.connection)) {
            settingsViewModel.addConnection()
            activity.let {
                if (it is RemoveFragmentFromBackstackInterface) {
                    it.removeFragmentFromBackstack()
                }
            }
        }
    }
}
