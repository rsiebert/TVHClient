package org.tvheadend.tvhclient.ui.features.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
import javax.inject.Inject

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val allConnections: LiveData<List<Connection>>

    val activeConnectionId: Int
        get() {
            return appRepository.connectionData.activeItem.id
        }

    var connectionHasChanged: Boolean
        get() = sharedPreferences.getBoolean("connection_value_changed", false)
        set(change) = sharedPreferences.edit().putBoolean("connection_value_changed", change).apply()

    init {
        MainApplication.getComponent().inject(this)
        allConnections = appRepository.connectionData.getLiveDataItems() ?: MutableLiveData()
    }
}
