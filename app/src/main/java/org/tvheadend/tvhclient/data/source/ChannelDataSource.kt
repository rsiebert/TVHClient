package org.tvheadend.tvhclient.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.data.entity.Channel
import org.tvheadend.tvhclient.data.entity.EpgChannel
import timber.log.Timber
import java.util.*

class ChannelDataSource(private val db: AppRoomDatabase) : DataSourceInterface<Channel> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: Channel) {
        ioScope.launch { db.channelDao.insert(item) }
    }

    fun addItems(items: List<Channel>) {
        ioScope.launch { db.channelDao.insert(ArrayList(items)) }
    }

    override fun updateItem(item: Channel) {
        ioScope.launch { db.channelDao.update(item) }
    }

    override fun removeItem(item: Channel) {
        ioScope.launch { db.channelDao.delete(item) }
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
            channel = db.channelDao.loadChannelByIdSync(id as Int)
        }
        return channel
    }

    fun getChannels(sortOrder: Int = 0): List<Channel> {
        val channels = ArrayList<Channel>()
        runBlocking(Dispatchers.IO) {
            channels.addAll(db.channelDao.loadAllChannelsSync(sortOrder))
        }
        return channels
    }

    override fun getItems(): List<Channel> {
        return getChannels()
    }

    fun getItemByIdWithPrograms(id: Int, selectedTime: Long): Channel? {
        var channel: Channel? = null
        runBlocking(Dispatchers.IO) {
            channel = db.channelDao.loadChannelByIdWithProgramsSync(id, selectedTime)
        }
        return channel
    }

    fun getAllEpgChannels(channelSortOrder: Int, tagIds: List<Int>): LiveData<List<EpgChannel>> {
        Timber.d("Loading epg channels with sort order $channelSortOrder and ${tagIds.size} tags")
        return if (tagIds.isEmpty()) {
            db.channelDao.loadAllEpgChannels(channelSortOrder)
        } else {
            db.channelDao.loadAllEpgChannelsByTag(channelSortOrder, tagIds)
        }
    }

    fun getAllChannelsByTime(selectedTime: Long, channelSortOrder: Int, tagIds: List<Int>): LiveData<List<Channel>> {
        Timber.d("Loading channels from time $selectedTime with sort order $channelSortOrder and ${tagIds.size} tags")
        return if (tagIds.isEmpty()) {
            db.channelDao.loadAllChannelsByTime(selectedTime, channelSortOrder)
        } else {
            db.channelDao.loadAllChannelsByTimeAndTag(selectedTime, channelSortOrder, tagIds)
        }
    }
}
