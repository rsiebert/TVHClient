package org.tvheadend.tvhclient.domain.repository.data_source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.Recording
import java.util.*

class RecordingData(private val db: AppRoomDatabase) : DataSourceInterface<Recording> {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: Recording) {
        scope.launch { db.recordingDao.insert(item) }
    }

    fun addItems(items: List<Recording>) {
        scope.launch { db.recordingDao.insert(ArrayList(items)) }
    }

    override fun updateItem(item: Recording) {
        scope.launch { db.recordingDao.update(item) }
    }

    override fun removeItem(item: Recording) {
        scope.launch { db.recordingDao.delete(item) }
    }

    fun removeItems() {
        scope.launch { db.recordingDao.deleteAll() }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return MutableLiveData()
    }

    override fun getLiveDataItems(): LiveData<List<Recording>> {
        return db.recordingDao.loadRecordings()
    }

    override fun getLiveDataItemById(id: Any): LiveData<Recording> {
        return db.recordingDao.loadRecordingById(id as Int)
    }

    fun getLiveDataItemsByChannelId(channelId: Int): LiveData<List<Recording>> {
        return db.recordingDao.loadRecordingsByChannelId(channelId)
    }

    suspend fun getItemsByChannelIdSuspendable(channelId: Int): List<Recording> {
        return db.recordingDao.loadRecordingsByChannelIdSuspendable(channelId)
    }

    fun getCompletedRecordings(): LiveData<List<Recording>> {
        return db.recordingDao.loadCompletedRecordings()
    }

    fun getScheduledRecordings(hideDuplicates: Boolean): LiveData<List<Recording>> {
        return if (hideDuplicates) {
            db.recordingDao.loadUniqueScheduledRecordings()
        } else {
            db.recordingDao.loadScheduledRecordings()
        }
    }

    fun getFailedRecordings(): LiveData<List<Recording>> {
        return db.recordingDao.loadFailedRecordings()
    }

    fun getRemovedRecordings(): LiveData<List<Recording>> {
        return db.recordingDao.loadRemovedRecordings()
    }

    fun getLiveDataCountByType(type: String): LiveData<Int> {
        return when (type) {
            "completed" -> db.recordingDao.completedRecordingCount
            "scheduled" -> db.recordingDao.scheduledRecordingCount
            "running" -> db.recordingDao.runningRecordingCount
            "failed" -> db.recordingDao.failedRecordingCount
            "removed" -> db.recordingDao.removedRecordingCount
            else -> MutableLiveData()
        }
    }

    override fun getItemById(id: Any): Recording? {
        var recording: Recording? = null
        if ((id as Int) > 0) {
            runBlocking(Dispatchers.IO) {
                recording = db.recordingDao.loadRecordingByIdSync(id)
            }
        }
        return recording
    }

    override fun getItems(): List<Recording> {
        return ArrayList()
    }

    fun getItemByEventId(id: Int): Recording? {
        var recording: Recording? = null
        runBlocking(Dispatchers.IO) {
            recording = db.recordingDao.loadRecordingByEventIdSync(id)
        }
        return recording
    }

    fun removeAndAddItems(items: ArrayList<Recording>) {
        scope.launch {
            db.recordingDao.deleteAll()
            db.recordingDao.insert(items)
        }
    }
}
