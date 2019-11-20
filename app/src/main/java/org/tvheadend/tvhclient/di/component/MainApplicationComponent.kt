package org.tvheadend.tvhclient.di.component

import dagger.Component
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.service.HtspIntentService
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.di.module.ContextModule
import org.tvheadend.tvhclient.di.module.RepositoryModule
import org.tvheadend.tvhclient.di.module.SharedPreferencesModule
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.startup.StartupActivity
import org.tvheadend.tvhclient.ui.features.startup.StartupViewModel
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ContextModule::class,
    SharedPreferencesModule::class,
    RepositoryModule::class])
interface MainApplicationComponent {

    fun inject(mainApplication: MainApplication)
    fun inject(htspService: HtspService)
    fun inject(htspIntentService: HtspIntentService)
    fun inject(baseActivity: BaseActivity)
    fun inject(baseFragment: BaseFragment)
    fun inject(baseViewModel: BaseViewModel)
    fun inject(startupActivity: StartupActivity)
    fun inject(startupViewModel: StartupViewModel)

    @Component.Builder
    interface Builder {

        fun build(): MainApplicationComponent

        fun sharedPreferencesModule(sharedPreferencesModule: SharedPreferencesModule): Builder

        fun repositoryModule(repositoryModule: RepositoryModule): Builder

        fun contextModule(contextModule: ContextModule): Builder
    }
}