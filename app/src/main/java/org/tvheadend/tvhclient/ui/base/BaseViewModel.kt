package org.tvheadend.tvhclient.ui.base

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.di.component.DaggerViewModelInjector
import org.tvheadend.tvhclient.di.component.ViewModelInjector
import org.tvheadend.tvhclient.di.module.ContextModule
import org.tvheadend.tvhclient.di.module.RepositoryModule
import org.tvheadend.tvhclient.di.module.SharedPreferencesModule
import javax.inject.Inject

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val injector: ViewModelInjector = DaggerViewModelInjector
            .builder()
            .contextModule(ContextModule(application.applicationContext))
            .sharedPreferencesModule(SharedPreferencesModule)
            .repositoryModule(RepositoryModule)
            .build()

    init {
        inject()
    }

    private fun inject() {
        injector.inject(this)
    }
}