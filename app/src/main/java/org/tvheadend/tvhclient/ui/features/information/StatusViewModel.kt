package org.tvheadend.tvhclient.ui.features.information

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Input
import org.tvheadend.tvhclient.domain.entity.Subscription
import javax.inject.Inject

class StatusViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appRepository: AppRepository

    val subscriptions: LiveData<List<Subscription>>
    val inputs: LiveData<List<Input>>

    init {
        MainApplication.getComponent().inject(this)

        subscriptions = appRepository.subscriptionData.getLiveDataItems()
        inputs = appRepository.inputData.getLiveDataItems()
    }
}