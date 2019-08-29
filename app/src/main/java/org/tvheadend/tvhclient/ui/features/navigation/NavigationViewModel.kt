package org.tvheadend.tvhclient.ui.features.navigation

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.common.Event
import org.tvheadend.tvhclient.ui.features.navigation.NavigationDrawer.Companion.MENU_SETTINGS
import timber.log.Timber

class NavigationViewModel(application: Application) : BaseViewModel(application) {

    val connections = appRepository.connectionData.getLiveDataItems()
    private val navigationMenuId = MutableLiveData<Event<Int>>()
    var currentNavigationMenuId: Int

    init {
        Timber.d("Initializing")
        currentNavigationMenuId = Integer.parseInt(sharedPreferences.getString("start_screen", appContext.resources.getString(R.string.pref_default_start_screen))!!)
        navigationMenuId.value = Event(currentNavigationMenuId)
    }

    fun getNavigationMenuId(): LiveData<Event<Int>> = navigationMenuId

    fun setNavigationMenuId(id: Int) {
        Timber.d("Received new navigation id $id, previous navigation id is ${navigationMenuId.value}")
        if (currentNavigationMenuId != id || id == MENU_SETTINGS) {
            Timber.d("Setting navigation id to $id")
            currentNavigationMenuId = id
            navigationMenuId.value = Event(id)
        }
    }

    fun setSelectedConnectionAsActive(id: Int): Boolean {
        val newActiveConnection = appRepository.connectionData.getItemById(id)
        return if (newActiveConnection != null) {

            val currentlyActiveConnection = appRepository.connectionData.activeItem
            currentlyActiveConnection.isActive = false
            appRepository.connectionData.updateItem(currentlyActiveConnection)

            newActiveConnection.isActive = true
            newActiveConnection.isSyncRequired = true
            newActiveConnection.lastUpdate = 0
            appRepository.connectionData.updateItem(newActiveConnection)
            true
        } else {
            false
        }
    }

    fun setSelectedMenuItemId(id: Int) {
        Timber.d("Back button was pressed, setting current navigation id ${navigationMenuId.value} to id $id")
        currentNavigationMenuId = id
    }
}