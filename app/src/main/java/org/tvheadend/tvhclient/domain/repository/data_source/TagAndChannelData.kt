package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.TagAndChannel
import java.util.*

class TagAndChannelData(private val db: AppRoomDatabase) : DataSourceInterface<TagAndChannel> {

    override fun addItem(item: TagAndChannel) {
        AsyncTask.execute { db.tagAndChannelDao.insert(item) }
    }

    override fun updateItem(item: TagAndChannel) {
        AsyncTask.execute { db.tagAndChannelDao.update(item) }
    }

    override fun removeItem(item: TagAndChannel) {
        AsyncTask.execute { db.tagAndChannelDao.delete(item) }
    }

    fun addAndRemoveItems(newItems: List<TagAndChannel>, oldItems: List<TagAndChannel>) {
        AsyncTask.execute {
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
        AsyncTask.execute { db.tagAndChannelDao.deleteByTagId(id) }
    }
}
