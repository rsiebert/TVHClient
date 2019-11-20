package org.tvheadend.tvhclient.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.data.entity.ChannelTag
import java.util.*

class ChannelTagDataSource(private val db: AppRoomDatabase) : DataSourceInterface<ChannelTag> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val liveDataSelectedItemIds: LiveData<List<Int>?>
        get() = db.channelTagDao.loadAllSelectedItemIds()

    val itemCount: Int
        get() {
            var count = 0
            runBlocking(Dispatchers.IO) {
                count = db.channelTagDao.itemCountSync
            }
            return count
        }

    override fun addItem(item: ChannelTag) {
        ioScope.launch { db.channelTagDao.insert(item) }
    }

    fun addItems(items: List<ChannelTag>) {
        ioScope.launch { db.channelTagDao.insert(ArrayList(items)) }
    }

    override fun updateItem(item: ChannelTag) {
        ioScope.launch { db.channelTagDao.update(item) }
    }

    override fun removeItem(item: ChannelTag) {
        ioScope.launch { db.channelTagDao.delete(item) }
    }

    fun updateSelectedChannelTags(ids: Set<Int>) {
        ioScope.launch {
            val channelTags = db.channelTagDao.loadAllChannelTagsSync()
            for (channelTag in channelTags) {
                channelTag.isSelected = false
                if (ids.contains(channelTag.tagId)) {
                    channelTag.isSelected = true
                }
            }
            db.channelTagDao.update(channelTags)
        }
    }


    override fun getLiveDataItemCount(): LiveData<Int> {
        return MutableLiveData()
    }

    override fun getLiveDataItems(): LiveData<List<ChannelTag>> {
        return db.channelTagDao.loadAllChannelTags()
    }

    override fun getLiveDataItemById(id: Any): LiveData<ChannelTag> {
        return MutableLiveData()
    }

    override fun getItemById(id: Any): ChannelTag? {
        var channelTag: ChannelTag? = null
        runBlocking(Dispatchers.IO) {
            channelTag = db.channelTagDao.loadChannelTagByIdSync(id as Int)
        }
        return channelTag
    }

    override fun getItems(): List<ChannelTag> {
        var channelTags: List<ChannelTag> = ArrayList()
        runBlocking(Dispatchers.IO) {
            channelTags = db.channelTagDao.loadAllChannelTagsSync()
        }
        return channelTags
    }
}
