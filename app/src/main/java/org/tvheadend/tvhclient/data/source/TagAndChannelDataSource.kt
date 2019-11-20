package org.tvheadend.tvhclient.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.data.entity.TagAndChannel
import java.util.*

class TagAndChannelDataSource(private val db: AppRoomDatabase) : DataSourceInterface<TagAndChannel> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: TagAndChannel) {
        ioScope.launch { db.tagAndChannelDao.insert(item) }
    }

    override fun updateItem(item: TagAndChannel) {
        ioScope.launch { db.tagAndChannelDao.update(item) }
    }

    override fun removeItem(item: TagAndChannel) {
        ioScope.launch { db.tagAndChannelDao.delete(item) }
    }

    fun addAndRemoveItems(newItems: List<TagAndChannel>, oldItems: List<TagAndChannel>) {
        ioScope.launch {
            db.tagAndChannelDao.insertAndDelete(
                    ArrayList(newItems),
                    ArrayList(oldItems))
        }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return MutableLiveData()
    }

    override fun getLiveDataItems(): LiveData<List<TagAndChannel>> {
        return MutableLiveData()
    }

    override fun getLiveDataItemById(id: Any): LiveData<TagAndChannel> {
        return MutableLiveData()
    }

    override fun getItemById(id: Any): TagAndChannel? {
        return null
    }

    override fun getItems(): List<TagAndChannel> {
        return ArrayList()
    }

    fun removeItemByTagId(id: Int) {
        ioScope.launch { db.tagAndChannelDao.deleteByTagId(id) }
    }
}
