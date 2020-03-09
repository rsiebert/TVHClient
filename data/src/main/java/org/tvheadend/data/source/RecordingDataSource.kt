package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
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
        return Transformations.map(db.recordingDao.loadRecordings()) { entities ->
            entities.map { it.toRecording() }
        }
    }

    override fun getLiveDataItemById(id: Any): LiveData<Recording> {
        return Transformations.map(db.recordingDao.loadRecordingById(id as Int)) { entity ->
            entity.toRecording()
        }
    }

    fun getLiveDataItemsByChannelId(channelId: Int): LiveData<List<Recording>> {
        return Transformations.map(db.recordingDao.loadRecordingsByChannelId(channelId)) { entities ->
            entities.map { it.toRecording() }
        }
    }

    fun getCompletedRecordings(sortOrder: Int): LiveData<List<Recording>> {
        return Transformations.map(db.recordingDao.loadCompletedRecordings(sortOrder)) { entities ->
            entities.map { it.toRecording() }
        }
    }

    fun getScheduledRecordings(hideDuplicates: Boolean): LiveData<List<Recording>> {
        return if (hideDuplicates) {
            Transformations.map(db.recordingDao.loadUniqueScheduledRecordings()) { entities ->
                entities.map { it.toRecording() }
            }
        } else {
            Transformations.map(db.recordingDao.loadScheduledRecordings()) { entities ->
                entities.map { it.toRecording() }
            }
        }
    }

    fun getFailedRecordings(): LiveData<List<Recording>> {
        return Transformations.map(db.recordingDao.loadFailedRecordings()) { entities ->
            entities.map { it.toRecording() }
        }
    }

    fun getRemovedRecordings(): LiveData<List<Recording>> {
        return Transformations.map(db.recordingDao.loadRemovedRecordings()) { entities ->
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
                recording = db.recordingDao.loadRecordingByIdSync(id).toRecording()
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
                recording = db.recordingDao.loadRecordingByEventIdSync(id).toRecording()
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
