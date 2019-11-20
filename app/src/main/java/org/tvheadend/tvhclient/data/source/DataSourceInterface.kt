package org.tvheadend.tvhclient.data.source

import androidx.lifecycle.LiveData

interface DataSourceInterface<T> {

    fun getLiveDataItemCount(): LiveData<Int>

    fun getLiveDataItems(): LiveData<List<T>>

    fun getLiveDataItemById(id: Any): LiveData<T>

    fun getItems(): List<T>

    fun getItemById(id: Any): T?

    fun addItem(item: T)

    fun updateItem(item: T)

    fun removeItem(item: T)
}
