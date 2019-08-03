package org.tvheadend.tvhclient.domain.repository.data_source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.Subscription

class SubscriptionData(private val db: AppRoomDatabase) : DataSourceInterface<Subscription> {

    var subscriptions: MutableList<Subscription> = ArrayList()

    override fun getLiveDataItemCount(): LiveData<Int> {
        val inputLiveDataCount = MutableLiveData<Int>()
        inputLiveDataCount.value = subscriptions.size
        return inputLiveDataCount
    }

    override fun getLiveDataItems(): LiveData<List<Subscription>> {
        val inputLiveData = MutableLiveData<List<Subscription>>()
        inputLiveData.value = getItems()
        return inputLiveData
    }

    override fun getLiveDataItemById(id: Any): LiveData<Subscription> {
        val inputLiveData = MutableLiveData<Subscription>()
        inputLiveData.value = getItemById(id)
        return inputLiveData
    }

    override fun getItems(): List<Subscription> {
        return subscriptions
    }

    override fun getItemById(id: Any): Subscription? {
        return subscriptions[id as Int]
    }

    override fun addItem(item: Subscription) {
        subscriptions.add(item)
    }

    override fun updateItem(item: Subscription) {
        subscriptions.add(item)
    }

    override fun removeItem(item: Subscription) {
        subscriptions.remove(item)
    }

    fun removeItems() {
        subscriptions.clear()
    }
}