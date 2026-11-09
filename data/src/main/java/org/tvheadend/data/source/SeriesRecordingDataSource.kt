package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.data.entity.SeriesRecordingEntity
import java.util.*

class SeriesRecordingDataSource(
        private val db: AppRoomDatabase,
        private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DataSourceInterface<SeriesRecording> {

    // See ChannelDataSource: reads follow the injected scope's dispatcher (Dispatchers.IO in
    // production, Unconfined in the unit tests).
    private val ioDispatcher: CoroutineDispatcher =
            ioScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.IO

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
        return db.seriesRecordingDao.loadAllRecordings().map { entities ->
            entities.map { it.toRecording() }
        }
    }

    override suspend fun getItemById(id: Any): SeriesRecording? {
        var seriesRecording: SeriesRecording? = null
        if ((id as String).isNotEmpty()) {
            withContext(ioDispatcher) {
                seriesRecording = db.seriesRecordingDao.loadRecordingByIdSync(id)?.toRecording()
            }
        }
        return seriesRecording
    }

    override suspend fun getItems(): List<SeriesRecording> {
        return ArrayList()
    }
}
