package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ChannelTag
import org.tvheadend.data.entity.ChannelTagEntity
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
        ioScope.launch { db.channelTagDao.insert(ChannelTagEntity.from(item)) }
    }

    fun addItems(items: List<ChannelTag>) {
        ioScope.launch { db.channelTagDao.insert(ArrayList(items).map { ChannelTagEntity.from(it) }) }
    }

    override fun updateItem(item: ChannelTag) {
        ioScope.launch { db.channelTagDao.update(ChannelTagEntity.from(item)) }
    }

    override fun removeItem(item: ChannelTag) {
        ioScope.launch { db.channelTagDao.delete(ChannelTagEntity.from(item)) }
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
        return Transformations.map(db.channelTagDao.loadAllChannelTags()) { entities ->
            entities.map { it.toChannelTag() }
        }
    }

    override fun getLiveDataItemById(id: Any): LiveData<ChannelTag> {
        return MutableLiveData()
    }

    override fun getItemById(id: Any): ChannelTag? {
        var channelTag: ChannelTag? = null
        runBlocking(Dispatchers.IO) {
            channelTag = db.channelTagDao.loadChannelTagByIdSync(id as Int).toChannelTag()
        }
        return channelTag
    }

    override fun getItems(): List<ChannelTag> {
        var channelTags: List<ChannelTag> = ArrayList()
        runBlocking(Dispatchers.IO) {
            channelTags = db.channelTagDao.loadAllChannelTagsSync().map { it.toChannelTag() }
        }
        return channelTags
    }

    fun getNonEmptyItems(loadAll: Boolean = true): List<ChannelTag> {
        var channelTags: List<ChannelTag> = ArrayList()
        runBlocking(Dispatchers.IO) {
            channelTags = if (loadAll) {
                db.channelTagDao.loadAllChannelTagsSync().map { it.toChannelTag() }
            } else {
                db.channelTagDao.loadOnlyNonEmptyChannelTagsSync().map { it.toChannelTag() }
            }
        }
        return channelTags
    }
}
