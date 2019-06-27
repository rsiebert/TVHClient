package org.tvheadend.tvhclient.ui.features.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import timber.log.Timber

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    lateinit var toolbarInterface: ToolbarInterface
    lateinit var sharedPreferences: SharedPreferences
    lateinit var settingsViewModel: SettingsViewModel

    protected var isUnlocked: Boolean = false
    protected var htspVersion: Int = 13

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        settingsViewModel = ViewModelProviders.of(activity as BaseActivity).get(SettingsViewModel::class.java)

        settingsViewModel.serverStatusLiveData.observe(viewLifecycleOwner, Observer { serverStatus ->
            Timber.d("Received live data, htsp server version is ${serverStatus.htspVersion}")
            htspVersion = serverStatus.htspVersion
        })

        settingsViewModel.isUnlocked.observe(viewLifecycleOwner, Observer { unlocked ->
            Timber.d("Received live data, unlocked changed to $unlocked")
            isUnlocked = unlocked
        })
    }
}
