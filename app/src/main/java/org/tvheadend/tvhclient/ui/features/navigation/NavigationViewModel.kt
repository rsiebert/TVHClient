package org.tvheadend.tvhclient.ui.features.navigation

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.livedata.Event
import kotlinx.coroutines.launch
import timber.log.Timber

class NavigationViewModel(application: Application) : BaseViewModel(application) {

    // The list of connections is loaded off the main thread and published as LiveData; readers
    // observe it instead of reading a blocking snapshot.
    val connections = MutableLiveData<List<Connection>>()
    val connectionLiveData = appRepository.connectionData.liveDataActiveItem
    private val navigationMenuId = MutableLiveData<Event<NavDestination>>()
    var currentDestination: NavDestination

    init {
        Timber.d("Initializing")
        val startScreen = sharedPreferences.getString("start_screen", application.applicationContext.resources.getString(R.string.pref_default_start_screen))
        currentDestination = NavDestination.fromId(startScreen)
        navigationMenuId.value = Event(currentDestination)
        viewModelScope.launch {
            connections.value = appRepository.connectionData.getItems()
        }
    }

    fun getNavigationMenuId(): LiveData<Event<NavDestination>> = navigationMenuId

    fun setNavigationMenuId(destination: NavDestination) {
        Timber.d("Received new navigation destination $destination, previous destination is ${navigationMenuId.value?.peekContent()}")
        // Settings is always re-emitted so that selecting it again re-opens the settings screen,
        // even while it is already the current destination.
        if (currentDestination != destination || destination == NavDestination.SETTINGS) {
            Timber.d("Setting navigation destination to $destination")
            currentDestination = destination
            navigationMenuId.value = Event(destination)
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

    fun setSelectedMenuItemId(destination: NavDestination) {
        Timber.d("Back button was pressed, setting current navigation destination ${navigationMenuId.value} to $destination")
        currentDestination = destination
    }
}
