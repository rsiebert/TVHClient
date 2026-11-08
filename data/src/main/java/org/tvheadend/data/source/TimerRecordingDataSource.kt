package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.data.entity.TimerRecordingEntity
import java.util.*

class TimerRecordingDataSource(
        private val db: AppRoomDatabase,
        private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DataSourceInterface<TimerRecording> {

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

    override fun getItemById(id: Any): TimerRecording? {
        var timerRecording: TimerRecording? = null
        if ((id as String).isNotEmpty()) {
            runBlocking(Dispatchers.IO) {
                timerRecording = db.timerRecordingDao.loadRecordingByIdSync(id)?.toRecording()
            }
        }
        return timerRecording
    }

    override fun getItems(): List<TimerRecording> {
        return ArrayList()
    }
}
