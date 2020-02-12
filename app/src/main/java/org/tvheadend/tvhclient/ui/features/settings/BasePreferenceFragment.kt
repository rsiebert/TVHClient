package org.tvheadend.tvhclient.ui.features.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import timber.log.Timber

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    lateinit var toolbarInterface: ToolbarInterface
    lateinit var sharedPreferences: SharedPreferences
    lateinit var settingsViewModel: SettingsViewModel

    var isUnlocked: Boolean = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        settingsViewModel = ViewModelProviders.of(activity as SettingsActivity).get(SettingsViewModel::class.java)

        settingsViewModel.isUnlocked.observe(viewLifecycleOwner, Observer { unlocked ->
            Timber.d("Received live data, unlocked changed to $unlocked")
            isUnlocked = unlocked
        })

        // Observe and update the server status. This is required to get the update server status
        // in case the database has been cleared. The view model is not destroyed so it would
        // contain an old version with invalid profile ids.
        settingsViewModel.currentServerStatusLiveData.observe(viewLifecycleOwner, Observer { serverStatus ->
            Timber.d("Received live data, server status has changed")
            if (serverStatus != null) {
                Timber.d("Received server status")
                settingsViewModel.currentServerStatus = serverStatus
            }
        })
    }
}
