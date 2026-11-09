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
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ChannelEntity
import org.tvheadend.data.entity.EpgChannel
import timber.log.Timber
import java.util.*

class ChannelDataSource(
        private val db: AppRoomDatabase,
        private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DataSourceInterface<Channel> {

    // The reads run on the same dispatcher the writes use. In production that is
    // [Dispatchers.IO]; a test that injects an Unconfined scope therefore keeps the reads
    // (and the ViewModel coroutines that await them) synchronous and deterministic.
    private val ioDispatcher: CoroutineDispatcher =
            ioScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.IO

    override fun addItem(item: Channel) {
        ioScope.launch { db.channelDao.insert(ChannelEntity.from(item)) }
    }

    fun addItems(items: List<Channel>) {
        ioScope.launch { db.channelDao.insert(ArrayList(items.map { ChannelEntity.from(it) })) }
    }

    override fun updateItem(item: Channel) {
        ioScope.launch { db.channelDao.update(ChannelEntity.from(item)) }
    }

    override fun removeItem(item: Channel) {
        ioScope.launch { db.channelDao.delete(ChannelEntity.from(item)) }
    }

    fun removeItemById(id: Int) {
        ioScope.launch { db.channelDao.deleteById(id) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.channelDao.itemCount
    }

    override fun getLiveDataItems(): LiveData<List<Channel>> {
        return MutableLiveData()
    }

    override suspend fun getItemById(id: Any): Channel? {
        var channel: Channel?
        withContext(ioDispatcher) {
            channel = db.channelDao.loadChannelByIdSync(id as Int)?.toChannel()
        }
        return channel
    }

    suspend fun getChannels(sortOrder: Int = 0): List<Channel> {
        val channels = ArrayList<Channel>()
        withContext(ioDispatcher) {
            channels.addAll(db.channelDao.loadAllChannelsSync(sortOrder).map { it.toChannel() })
        }
        return channels
    }

    override suspend fun getItems(): List<Channel> {
        return getChannels()
    }

    suspend fun getItemByIdWithPrograms(id: Int, selectedTime: Long): Channel? {
        var channel: Channel?
        withContext(ioDispatcher) {
            channel = db.channelDao.loadChannelByIdWithProgramsSync(id, selectedTime)?.toChannel()
        }
        return channel
    }

    fun getAllEpgChannels(channelSortOrder: Int, tagIds: List<Int>): LiveData<List<EpgChannel>> {
        Timber.d("Loading epg channels with sort order $channelSortOrder and ${tagIds.size} tags")
        return if (tagIds.isEmpty()) {
            db.channelDao.loadAllEpgChannels(channelSortOrder).map { entities ->
                entities.map { it.toEpgChannel() }
            }
        } else {
            db.channelDao.loadAllEpgChannelsByTag(channelSortOrder, tagIds).map { entities ->
                entities.map { it.toEpgChannel() }
            }
        }
    }

    fun getAllChannelsByTime(selectedTime: Long, channelSortOrder: Int, tagIds: List<Int>): LiveData<List<Channel>> {
        Timber.d("Loading channels from time $selectedTime with sort order $channelSortOrder and ${tagIds.size} tags")
        return if (tagIds.isEmpty()) {
            db.channelDao.loadAllChannelsByTime(selectedTime, channelSortOrder).map { entities ->
                entities.map { it.toChannel() }
            }
        } else {
            db.channelDao.loadAllChannelsByTimeAndTag(selectedTime, channelSortOrder, tagIds).map { entities ->
                entities.map { it.toChannel() }
            }
        }
    }
}
