package org.tvheadend.tvhclient.ui.features.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.MainViewModel
import javax.inject.Inject

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var appRepository: AppRepository

    lateinit var toolbarInterface: ToolbarInterface
    lateinit var sharedPreferences: SharedPreferences
    protected lateinit var mainViewModel: MainViewModel

    var isUnlocked: Boolean = false
    lateinit var serverStatus: ServerStatus

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        MainApplication.getComponent().inject(this)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        mainViewModel = ViewModelProviders.of(activity as BaseActivity).get(MainViewModel::class.java)

        serverStatus = appRepository.serverStatusData.activeItem
        isUnlocked = MainApplication.getInstance().isUnlocked
    }
}
