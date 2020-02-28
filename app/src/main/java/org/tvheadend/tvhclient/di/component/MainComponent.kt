package org.tvheadend.tvhclient.di.component

import dagger.Component
import org.tvheadend.data.di.FeatureScope
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.di.module.ContextModule
import org.tvheadend.tvhclient.di.module.SharedPreferencesModule
import org.tvheadend.tvhclient.service.HtspIntentService
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import org.tvheadend.tvhclient.ui.features.settings.SettingsViewModel
import org.tvheadend.tvhclient.ui.features.startup.StartupActivity
import org.tvheadend.tvhclient.ui.features.startup.StartupViewModel
import org.tvheadend.tvhclient.util.billing.BillingManager


@Component(
        modules = [ContextModule::class, SharedPreferencesModule::class],
        dependencies = [org.tvheadend.data.di.RepositoryComponent::class])
@FeatureScope
interface MainComponent {

    fun inject(mainApplication: MainApplication)
    fun inject(htspService: HtspService)
    fun inject(htspIntentService: HtspIntentService)
    fun inject(baseActivity: BaseActivity)
    fun inject(settingsActivity: SettingsActivity)
    fun inject(baseFragment: BaseFragment)
    fun inject(baseViewModel: BaseViewModel)
    fun inject(settingsViewModel: SettingsViewModel)
    fun inject(startupActivity: StartupActivity)
    fun inject(startupViewModel: StartupViewModel)
    fun inject(billingManager: BillingManager)

/*
    @Component.Builder
    interface Builder {

        fun build(): MainApplicationComponent

        fun sharedPreferencesModule(sharedPreferencesModule: SharedPreferencesModule): Builder

        fun repositoryModule(repositoryModule: RepositoryModule): Builder

        fun contextModule(contextModule: ContextModule): Builder
    }

 */
}