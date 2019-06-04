package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.SeriesRecording
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException

class SeriesRecordingData(private val db: AppRoomDatabase) : DataSourceInterface<SeriesRecording> {

    override fun addItem(item: SeriesRecording) {
        AsyncTask.execute { db.seriesRecordingDao.insert(item) }
    }

    override fun updateItem(item: SeriesRecording) {
        AsyncTask.execute { db.seriesRecordingDao.update(item) }
    }

    override fun removeItem(item: SeriesRecording) {
        AsyncTask.execute { db.seriesRecordingDao.delete(item) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.seriesRecordingDao.recordingCount
    }

    override fun getLiveDataItems(): LiveData<List<SeriesRecording>> {
        return db.seriesRecordingDao.loadAllRecordings()
    }

    override fun getLiveDataItemById(id: Any): LiveData<SeriesRecording> {
        return db.seriesRecordingDao.loadRecordingById(id as String)
    }

    override fun getItemById(id: Any): SeriesRecording {
        if ((id as String).isNotEmpty()) {
            try {
                val recording = SeriesRecordingByIdTask(db, id).execute().get()
                return recording ?: SeriesRecording()
            } catch (e: InterruptedException) {
                Timber.e(e, "Loading series recording by id task got interrupted")
            } catch (e: ExecutionException) {
                Timber.e(e, "Loading series recording by id task aborted")
            }
        }
        return SeriesRecording()
    }

    override fun getItems(): List<SeriesRecording> {
        return ArrayList()
    }

    private class SeriesRecordingByIdTask internal constructor(private val db: AppRoomDatabase, private val id: String) : AsyncTask<Void, Void, SeriesRecording>() {

        override fun doInBackground(vararg voids: Void): SeriesRecording {
            return db.seriesRecordingDao.loadRecordingByIdSync(id)
        }
    }
}
