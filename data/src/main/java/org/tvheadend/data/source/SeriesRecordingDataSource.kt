package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.data.entity.SeriesRecordingEntity
import java.util.*

class SeriesRecordingDataSource(private val db: AppRoomDatabase) : DataSourceInterface<SeriesRecording> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: SeriesRecording) {
        ioScope.launch { db.seriesRecordingDao.insert(SeriesRecordingEntity.from(item)) }
    }

    override fun updateItem(item: SeriesRecording) {
        ioScope.launch { db.seriesRecordingDao.update(SeriesRecordingEntity.from(item)) }
    }

    override fun removeItem(item: SeriesRecording) {
        ioScope.launch { db.seriesRecordingDao.delete(SeriesRecordingEntity.from(item)) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.seriesRecordingDao.itemCount
    }

    override fun getLiveDataItems(): LiveData<List<SeriesRecording>> {
        return Transformations.map(db.seriesRecordingDao.loadAllRecordings()) { entities ->
            entities.map { it.toRecording() }
        }
    }

    override fun getLiveDataItemById(id: Any): LiveData<SeriesRecording> {
        return Transformations.map(db.seriesRecordingDao.loadRecordingById(id as String)) { entity ->
            entity.toRecording()
        }
    }

    override fun getItemById(id: Any): SeriesRecording? {
        var seriesRecording: SeriesRecording? = null
        if ((id as String).isNotEmpty()) {
            runBlocking(Dispatchers.IO) {
                seriesRecording = db.seriesRecordingDao.loadRecordingByIdSync(id)?.toRecording()
            }
        }
        return seriesRecording
    }

    override fun getItems(): List<SeriesRecording> {
        return ArrayList()
    }
}
