package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ChannelTag
import org.tvheadend.data.entity.ChannelTagEntity
import java.util.*

class ChannelTagDataSource(
        private val db: AppRoomDatabase,
        private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DataSourceInterface<ChannelTag> {

    // See ChannelDataSource: reads follow the injected scope's dispatcher (Dispatchers.IO in
    // production, Unconfined in the unit tests).
    private val ioDispatcher: CoroutineDispatcher =
            ioScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.IO

    val liveDataSelectedItemIds: LiveData<List<Int>?>
        get() = db.channelTagDao.loadAllSelectedItemIds()

    suspend fun getItemCount(): Int {
        var count: Int
        withContext(ioDispatcher) {
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
        return db.channelTagDao.loadAllChannelTags().map { entities ->
            entities.map { it.toChannelTag() }
        }
    }

    override suspend fun getItemById(id: Any): ChannelTag? {
        var channelTag: ChannelTag?
        withContext(ioDispatcher) {
            channelTag = db.channelTagDao.loadChannelTagByIdSync(id as Int)?.toChannelTag()
        }
        return channelTag
    }

    override suspend fun getItems(): List<ChannelTag> {
        var channelTags: List<ChannelTag> = ArrayList()
        withContext(ioDispatcher) {
            channelTags = db.channelTagDao.loadAllChannelTagsSync().map { it.toChannelTag() }
        }
        return channelTags
    }

    suspend fun getNonEmptyItems(loadAll: Boolean = true): List<ChannelTag> {
        var channelTags: List<ChannelTag> = ArrayList()
        withContext(ioDispatcher) {
            channelTags = if (loadAll) {
                db.channelTagDao.loadAllChannelTagsSync().map { it.toChannelTag() }
            } else {
                db.channelTagDao.loadOnlyNonEmptyChannelTagsSync().map { it.toChannelTag() }
            }
        }
        return channelTags
    }
}
