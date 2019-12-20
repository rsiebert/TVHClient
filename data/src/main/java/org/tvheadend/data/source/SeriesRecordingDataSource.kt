package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.SeriesRecording
import java.util.*

class SeriesRecordingDataSource(private val db: AppRoomDatabase) : DataSourceInterface<SeriesRecording> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: SeriesRecording) {
        ioScope.launch { db.seriesRecordingDao.insert(item) }
    }

    override fun updateItem(item: SeriesRecording) {
        ioScope.launch { db.seriesRecordingDao.update(item) }
    }

    override fun removeItem(item: SeriesRecording) {
        ioScope.launch { db.seriesRecordingDao.delete(item) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.seriesRecordingDao.itemCount
    }

    override fun getLiveDataItems(): LiveData<List<SeriesRecording>> {
        return db.seriesRecordingDao.loadAllRecordings()
    }

    override fun getLiveDataItemById(id: Any): LiveData<SeriesRecording> {
        return db.seriesRecordingDao.loadRecordingById(id as String)
    }

    override fun getItemById(id: Any): SeriesRecording? {
        var seriesRecording: SeriesRecording? = null
        if ((id as String).isNotEmpty()) {
            runBlocking(Dispatchers.IO) {
                seriesRecording = db.seriesRecordingDao.loadRecordingByIdSync(id)
            }
        }
        return seriesRecording
    }

    override fun getItems(): List<SeriesRecording> {
        return ArrayList()
    }
}
