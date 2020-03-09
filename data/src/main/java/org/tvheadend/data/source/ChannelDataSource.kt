package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ChannelEntity
import org.tvheadend.data.entity.EpgChannel
import timber.log.Timber
import java.util.*

class ChannelDataSource(private val db: AppRoomDatabase) : DataSourceInterface<Channel> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

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

    override fun getLiveDataItemById(id: Any): LiveData<Channel> {
        return MutableLiveData()
    }

    override fun getItemById(id: Any): Channel? {
        var channel: Channel? = null
        runBlocking(Dispatchers.IO) {
            channel = db.channelDao.loadChannelByIdSync(id as Int).toChannel()
        }
        return channel
    }

    fun getChannels(sortOrder: Int = 0): List<Channel> {
        val channels = ArrayList<Channel>()
        runBlocking(Dispatchers.IO) {
            channels.addAll(db.channelDao.loadAllChannelsSync(sortOrder).map { it.toChannel() })
        }
        return channels
    }

    override fun getItems(): List<Channel> {
        return getChannels()
    }

    fun getItemByIdWithPrograms(id: Int, selectedTime: Long): Channel? {
        var channel: Channel? = null
        runBlocking(Dispatchers.IO) {
            channel = db.channelDao.loadChannelByIdWithProgramsSync(id, selectedTime).toChannel()
        }
        return channel
    }

    fun getAllEpgChannels(channelSortOrder: Int, tagIds: List<Int>): LiveData<List<EpgChannel>> {
        Timber.d("Loading epg channels with sort order $channelSortOrder and ${tagIds.size} tags")
        return if (tagIds.isEmpty()) {
            Transformations.map(db.channelDao.loadAllEpgChannels(channelSortOrder)) { entities ->
                entities.map { it.toEpgChannel() }
            }
        } else {
            Transformations.map(db.channelDao.loadAllEpgChannelsByTag(channelSortOrder, tagIds)) { entities ->
                entities.map { it.toEpgChannel() }
            }
        }
    }

    fun getAllChannelsByTime(selectedTime: Long, channelSortOrder: Int, tagIds: List<Int>): LiveData<List<Channel>> {
        Timber.d("Loading channels from time $selectedTime with sort order $channelSortOrder and ${tagIds.size} tags")
        return if (tagIds.isEmpty()) {
            Transformations.map(db.channelDao.loadAllChannelsByTime(selectedTime, channelSortOrder)) { entities ->
                entities.map { it.toChannel() }
            }
        } else {
            Transformations.map(db.channelDao.loadAllChannelsByTimeAndTag(selectedTime, channelSortOrder, tagIds)) { entities ->
                entities.map { it.toChannel() }
            }
        }
    }
}
