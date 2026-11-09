package org.tvheadend.tvhclient.ui.features.navigation

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.navigation.NavigationDrawer.Companion.MENU_SETTINGS
import org.tvheadend.tvhclient.util.livedata.Event
import kotlinx.coroutines.launch
import timber.log.Timber

class NavigationViewModel(application: Application) : BaseViewModel(application) {

    // The list of connections is loaded off the main thread and published as LiveData; readers
    // observe it instead of reading a blocking snapshot.
    val connections = MutableLiveData<List<Connection>>()
    val connectionLiveData = appRepository.connectionData.liveDataActiveItem
    private val navigationMenuId = MutableLiveData<Event<Int>>()
    var currentNavigationMenuId: Int
    private val defaultStartScreen = application.applicationContext.resources.getString(R.string.pref_default_start_screen)

    init {
        Timber.d("Initializing")
        currentNavigationMenuId = Integer.parseInt(sharedPreferences.getString("start_screen", defaultStartScreen)!!)
        navigationMenuId.value = Event(currentNavigationMenuId)
        viewModelScope.launch {
            connections.value = appRepository.connectionData.getItems()
        }
    }

    fun getNavigationMenuId(): LiveData<Event<Int>> = navigationMenuId

    fun setNavigationMenuId(id: Int) {
        Timber.d("Received new navigation id $id, previous navigation id is ${navigationMenuId.value?.peekContent()}")
        if (currentNavigationMenuId != id || id == MENU_SETTINGS) {
            Timber.d("Setting navigation id to $id")
            currentNavigationMenuId = id
            navigationMenuId.value = Event(id)
        }
    }

    suspend fun setSelectedConnectionAsActive(id: Int): Boolean {
        val currentlyActiveConnection = appRepository.connectionData.getActiveItem()
        val newActiveConnection = appRepository.connectionData.getItemById(id)

        Timber.d("Switching connection from ${currentlyActiveConnection.name} with id ${currentlyActiveConnection.id} to ${newActiveConnection?.name} with id ${newActiveConnection?.id}")

        if (newActiveConnection != null && newActiveConnection.id != currentlyActiveConnection.id) {
            appRepository.connectionData.switchActiveConnection(currentlyActiveConnection.id, newActiveConnection.id)
            Timber.d("Switched active connection from ${currentlyActiveConnection.name} to ${newActiveConnection.name} (db version is ${appRepository.connectionData.getActiveItem().name})")
            return true
        }
        return false
    }

    fun setSelectedMenuItemId(id: Int) {
        Timber.d("Back button was pressed, setting current navigation id ${navigationMenuId.value} to id $id")
        currentNavigationMenuId = id
    }
}