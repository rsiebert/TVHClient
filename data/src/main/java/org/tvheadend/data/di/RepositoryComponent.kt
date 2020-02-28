package org.tvheadend.data.di

import dagger.Component
import org.tvheadend.data.AppRepository
import javax.inject.Singleton

@Component(modules = [RepositoryModule::class])
@Singleton
interface RepositoryComponent {
    fun getAppRepository(): AppRepository
}