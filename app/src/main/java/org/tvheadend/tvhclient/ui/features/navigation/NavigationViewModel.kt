package org.tvheadend.tvhclient.ui.features.navigation

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
import timber.log.Timber
import javax.inject.Inject

class NavigationViewModel : ViewModel() {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val connection: Connection
    val connections: LiveData<List<Connection>>
    val navigationMenuId: MutableLiveData<Int> = MutableLiveData()
    var previousNavigationMenuId: Int = -1

    init {
        Timber.d("Initializing")
        MainApplication.component.inject(this)
        connection = appRepository.connectionData.activeItem
        connections = appRepository.connectionData.getLiveDataItems()
        navigationMenuId.value = Integer.parseInt(sharedPreferences.getString("start_screen", appContext.resources.getString(R.string.pref_default_start_screen))!!)
    }

    fun setNavigationMenuId(id: Int) {
        Timber.d("Received new navigation id $id")
        if (previousNavigationMenuId != id) {
            Timber.d("Setting navigation id to $id")
            navigationMenuId.value = id
        }
    }

    fun setNewActiveConnection(id: Int): Boolean {
        val connection = appRepository.connectionData.getItemById(id)
        return if (connection != null) {
            connection.isActive = true
            connection.isSyncRequired = true
            connection.lastUpdate = 0
            appRepository.connectionData.updateItem(connection)
            true
        } else {
            false
        }
    }
}