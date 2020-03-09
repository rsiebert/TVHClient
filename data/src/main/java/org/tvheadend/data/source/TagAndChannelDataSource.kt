package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.TagAndChannel
import org.tvheadend.data.entity.TagAndChannelEntity
import java.util.*

class TagAndChannelDataSource(private val db: AppRoomDatabase) : DataSourceInterface<TagAndChannel> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: TagAndChannel) {
        ioScope.launch { db.tagAndChannelDao.insert(TagAndChannelEntity.from(item)) }
    }

    override fun updateItem(item: TagAndChannel) {
        ioScope.launch { db.tagAndChannelDao.update(TagAndChannelEntity.from(item)) }
    }

    override fun removeItem(item: TagAndChannel) {
        ioScope.launch { db.tagAndChannelDao.delete(TagAndChannelEntity.from(item)) }
    }

    fun addAndRemoveItems(newItems: List<TagAndChannel>, oldItems: List<TagAndChannel>) {
        ioScope.launch {
            db.tagAndChannelDao.insertAndDelete(
                    ArrayList(newItems.map { TagAndChannelEntity.from(it) }),
                    ArrayList(oldItems.map { TagAndChannelEntity.from(it) }))
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
