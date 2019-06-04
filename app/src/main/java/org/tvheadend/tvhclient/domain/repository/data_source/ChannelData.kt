package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.EpgChannel
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException

class ChannelData(private val db: AppRoomDatabase) : DataSourceInterface<Channel> {

    val itemCount: Int
        get() {
            try {
                return ChannelCountTask(db).execute().get()
            } catch (e: InterruptedException) {
                Timber.e(e, "Loading channel count task got interrupted")
            } catch (e: ExecutionException) {
                Timber.e(e, "Loading channel count task aborted")
            }

            return 0
        }

    override fun addItem(item: Channel) {
        AsyncTask.execute { db.channelDao.insert(item) }
    }

    fun addItems(items: List<Channel>) {
        AsyncTask.execute { db.channelDao.insert(ArrayList(items)) }
    }

    override fun updateItem(item: Channel) {
        AsyncTask.execute { db.channelDao.update(item) }
    }

    override fun removeItem(item: Channel) {
        AsyncTask.execute { db.channelDao.delete(item) }
    }

    fun removeItemById(id: Int) {
        AsyncTask.execute { db.channelDao.deleteById(id) }
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
        try {
            return ChannelByIdTask(db, id as Int).execute().get()
        } catch (e: InterruptedException) {
            Timber.e(e, "Loading channel by id task got interrupted")
        } catch (e: ExecutionException) {
            Timber.e(e, "Loading channel by id task aborted")
        }

        return null
    }

    fun getChannels(sortOrder: Int = 0): List<Channel> {
        val channels = ArrayList<Channel>()
        try {
            channels.addAll(ChannelListTask(db, sortOrder).execute().get())
        } catch (e: InterruptedException) {
            Timber.e(e, "Loading all channels task got interrupted")
        } catch (e: ExecutionException) {
            Timber.e(e, "Loading all channels task aborted")
        }

        return channels
    }

    override fun getItems(): List<Channel> {
        return getChannels()
    }

    fun getItemByIdWithPrograms(id: Int, selectedTime: Long): Channel? {
        try {
            return ChannelByIdTask(db, id, selectedTime).execute().get()
        } catch (e: InterruptedException) {
            Timber.e(e, "Loading channel by id task got interrupted")
        } catch (e: ExecutionException) {
            Timber.e(e, "Loading channel by id task aborted")
        }

        return null
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

    private class ChannelByIdTask : AsyncTask<Void, Void, Channel> {
        private val db: AppRoomDatabase
        private val id: Int
        private val selectedTime: Long

        internal constructor(db: AppRoomDatabase, id: Int) {
            this.db = db
            this.id = id
            this.selectedTime = 0
        }

        internal constructor(db: AppRoomDatabase, id: Int, selectedTime: Long) {
            this.db = db
            this.id = id
            this.selectedTime = selectedTime
        }

        override fun doInBackground(vararg voids: Void): Channel {
            return if (selectedTime > 0) {
                db.channelDao.loadChannelByIdWithProgramsSync(id, selectedTime)
            } else {
                db.channelDao.loadChannelByIdSync(id)
            }
        }
    }

    private class ChannelListTask internal constructor(private val db: AppRoomDatabase, private val sortOrder: Int) : AsyncTask<Void, Void, List<Channel>>() {

        override fun doInBackground(vararg voids: Void): List<Channel> {
            return db.channelDao.loadAllChannelsSync(sortOrder)
        }
    }

    private class ChannelCountTask internal constructor(private val db: AppRoomDatabase) : AsyncTask<Void, Void, Int>() {

        override fun doInBackground(vararg voids: Void): Int {
            return db.channelDao.itemCountSync
        }
    }
}
