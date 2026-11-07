package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.RecordingEntity
import java.util.*

class RecordingDataSource(private val db: AppRoomDatabase) : DataSourceInterface<Recording> {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: Recording) {
        scope.launch { db.recordingDao.insert(RecordingEntity.from(item)) }
    }

    fun addItems(items: List<Recording>) {
        scope.launch { db.recordingDao.insert(ArrayList(items.map { RecordingEntity.from(it) })) }
    }

    override fun updateItem(item: Recording) {
        scope.launch { db.recordingDao.update(RecordingEntity.from(item)) }
    }

    override fun removeItem(item: Recording) {
        scope.launch { db.recordingDao.delete(RecordingEntity.from(item)) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return MutableLiveData()
    }

    override fun getLiveDataItems(): LiveData<List<Recording>> {
        return db.recordingDao.loadRecordings().map { entities ->
            entities.map { it.toRecording() }
        }
    }

    override fun getLiveDataItemById(id: Any): LiveData<Recording> {
        return db.recordingDao.loadRecordingById(id as Int).map { entity ->
            entity.toRecording()
        }
    }

    fun getLiveDataItemsByChannelId(channelId: Int): LiveData<List<Recording>> {
        return db.recordingDao.loadRecordingsByChannelId(channelId).map { entities ->
            entities.map { it.toRecording() }
        }
    }

    fun getCompletedRecordings(sortOrder: Int): LiveData<List<Recording>> {
        return db.recordingDao.loadCompletedRecordings(sortOrder).map { entities ->
            entities.map { it.toRecording() }
        }
    }

    fun getScheduledRecordings(hideDuplicates: Boolean): LiveData<List<Recording>> {
        return if (hideDuplicates) {
            db.recordingDao.loadUniqueScheduledRecordings().map { entities ->
                entities.map { it.toRecording() }
            }
        } else {
            db.recordingDao.loadScheduledRecordings().map { entities ->
                entities.map { it.toRecording() }
            }
        }
    }

    fun getFailedRecordings(): LiveData<List<Recording>> {
        return db.recordingDao.loadFailedRecordings().map { entities ->
            entities.map { it.toRecording() }
        }
    }

    fun getRemovedRecordings(): LiveData<List<Recording>> {
        return db.recordingDao.loadRemovedRecordings().map { entities ->
            entities.map { it.toRecording() }
        }
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
                recording = db.recordingDao.loadRecordingByIdSync(id)?.toRecording()
            }
        }
        return recording
    }

    override fun getItems(): List<Recording> {
        return ArrayList()
    }

    fun getItemByEventId(id: Int): Recording? {
        var recording: Recording? = null
        if (id > 0) {
            runBlocking(Dispatchers.IO) {
                recording = db.recordingDao.loadRecordingByEventIdSync(id)?.toRecording()
            }
        }
        return recording
    }

    fun removeAndAddItems(items: ArrayList<Recording>) {
        scope.launch {
            db.recordingDao.deleteAll()
            db.recordingDao.insert(items.map { RecordingEntity.from(it) })
        }
    }
}
