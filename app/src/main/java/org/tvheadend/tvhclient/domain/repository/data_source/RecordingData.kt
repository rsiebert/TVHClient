package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.Recording
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException

class RecordingData(private val db: AppRoomDatabase) : DataSourceInterface<Recording> {

    val itemCount: Int
        get() {
            try {
                return RecordingCountTask(db).execute().get()
            } catch (e: InterruptedException) {
                Timber.d(e, "Loading recording count task got interrupted")
            } catch (e: ExecutionException) {
                Timber.d(e, "Loading recording count task aborted")
            }

            return 0
        }

    override fun addItem(item: Recording) {
        AsyncTask.execute { db.recordingDao.insert(item) }
    }

    fun addItems(items: List<Recording>) {
        AsyncTask.execute { db.recordingDao.insert(ArrayList(items)) }
    }

    override fun updateItem(item: Recording) {
        AsyncTask.execute { db.recordingDao.update(item) }
    }

    override fun removeItem(item: Recording) {
        AsyncTask.execute { db.recordingDao.delete(item) }
    }

    fun removeItems() {
        AsyncTask.execute { db.recordingDao.deleteAll() }
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

    override fun getItemById(id: Any): Recording {
        if ((id as Int) > 0) {
            try {
                val recording = RecordingByIdTask(db, id, LOAD_BY_ID).execute().get()
                return recording ?: Recording()
            } catch (e: InterruptedException) {
                Timber.d(e, "Loading recording by id task got interrupted")
            } catch (e: ExecutionException) {
                Timber.d(e, "Loading recording by id task aborted")
            }
        }
        return Recording()
    }

    override fun getItems(): List<Recording> {
        return ArrayList()
    }

    fun getItemByEventId(id: Int): Recording? {
        try {
            return RecordingByIdTask(db, id, LOAD_BY_EVENT_ID).execute().get()
        } catch (e: InterruptedException) {
            Timber.d(e, "Loading recording by event id task got interrupted")
        } catch (e: ExecutionException) {
            Timber.d(e, "Loading recording by event id task aborted")
        }

        return null
    }

    private class RecordingByIdTask internal constructor(private val db: AppRoomDatabase, private val id: Int, private val type: Int) : AsyncTask<Void, Void, Recording>() {

        override fun doInBackground(vararg voids: Void): Recording? {
            return when (type) {
                LOAD_BY_ID -> db.recordingDao.loadRecordingByIdSync(id)
                LOAD_BY_EVENT_ID -> db.recordingDao.loadRecordingByEventIdSync(id)
                else -> null
            }
        }
    }

    private class RecordingCountTask internal constructor(private val db: AppRoomDatabase) : AsyncTask<Void, Void, Int>() {

        override fun doInBackground(vararg voids: Void): Int? {
            return db.recordingDao.itemCountSync
        }
    }

    companion object {

        private const val LOAD_BY_ID = 1
        private const val LOAD_BY_EVENT_ID = 2
    }
}
