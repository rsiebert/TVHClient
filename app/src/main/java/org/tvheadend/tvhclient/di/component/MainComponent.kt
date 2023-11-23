package org.tvheadend.tvhclient.di.component

import dagger.Component
import org.tvheadend.data.di.FeatureScope
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.di.module.ContextModule
import org.tvheadend.tvhclient.di.module.SharedPreferencesModule
import org.tvheadend.tvhclient.service.ConnectionIntentService
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import org.tvheadend.tvhclient.ui.features.settings.SettingsViewModel
import org.tvheadend.tvhclient.ui.features.startup.StartupViewModel


@Component(
        modules = [ContextModule::class, SharedPreferencesModule::class],
        dependencies = [org.tvheadend.data.di.RepositoryComponent::class])
@FeatureScope
interface MainComponent {

    fun inject(mainApplication: MainApplication)
    fun inject(connectionService: ConnectionService)
    fun inject(connectionIntentService: ConnectionIntentService)
    fun inject(settingsActivity: SettingsActivity)
    fun inject(baseFragment: BaseFragment)
    fun inject(baseViewModel: BaseViewModel)
    fun inject(settingsViewModel: SettingsViewModel)
    fun inject(startupViewModel: StartupViewModel)
}