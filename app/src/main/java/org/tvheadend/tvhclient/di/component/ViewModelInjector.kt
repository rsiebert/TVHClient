package org.tvheadend.tvhclient.di.component

import dagger.Component
import org.tvheadend.tvhclient.di.module.ContextModule
import org.tvheadend.tvhclient.di.module.RepositoryModule
import org.tvheadend.tvhclient.di.module.SharedPreferencesModule
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ContextModule::class,
    SharedPreferencesModule::class,
    RepositoryModule::class])
interface ViewModelInjector {

    fun inject(baseViewModel: BaseViewModel)

    @Component.Builder
    interface Builder {

        fun build(): ViewModelInjector

        fun sharedPreferencesModule(sharedPreferencesModule: SharedPreferencesModule): Builder

        fun repositoryModule(repositoryModule: RepositoryModule): Builder

        fun contextModule(contextModule: ContextModule): Builder
    }
}