package org.tvheadend.tvhclient.ui.features.navigation

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
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

    init {
        MainApplication.getComponent().inject(this)
        connection = appRepository.connectionData.activeItem
        connections = appRepository.connectionData.getLiveDataItems()
        navigationMenuId.value = NavigationDrawer.MENU_CHANNELS
    }

    fun setSelectedNavigationMenuId(id: Int) {
        navigationMenuId.value = id
    }

    fun setNewActiveConnection(id: Int): Boolean {
        val connection = appRepository.connectionData.getItemById(id)
        return if (connection != null) {
            connection.isActive = true
            appRepository.connectionData.updateItem(connection)
            true
        } else {
            false
        }
    }
}