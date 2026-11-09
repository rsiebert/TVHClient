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
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.data.entity.TimerRecordingEntity
import java.util.*

class TimerRecordingDataSource(
        private val db: AppRoomDatabase,
        private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DataSourceInterface<TimerRecording> {

    // See ChannelDataSource: reads follow the injected scope's dispatcher (Dispatchers.IO in
    // production, Unconfined in the unit tests).
    private val ioDispatcher: CoroutineDispatcher =
            ioScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.IO

    override fun addItem(item: TimerRecording) {
        ioScope.launch { db.timerRecordingDao.insert(TimerRecordingEntity.from(item)) }
    }

    override fun updateItem(item: TimerRecording) {
        ioScope.launch { db.timerRecordingDao.update(TimerRecordingEntity.from(item)) }
    }

    override fun removeItem(item: TimerRecording) {
        ioScope.launch { db.timerRecordingDao.delete(TimerRecordingEntity.from(item)) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.timerRecordingDao.itemCount
    }

    override fun getLiveDataItems(): LiveData<List<TimerRecording>> {
        return db.timerRecordingDao.loadAllRecordings().map { entities ->
            entities.map { it.toRecording() }
        }
    }

    override suspend fun getItemById(id: Any): TimerRecording? {
        var timerRecording: TimerRecording? = null
        if ((id as String).isNotEmpty()) {
            withContext(ioDispatcher) {
                timerRecording = db.timerRecordingDao.loadRecordingByIdSync(id)?.toRecording()
            }
        }
        return timerRecording
    }

    override suspend fun getItems(): List<TimerRecording> {
        return ArrayList()
    }
}
