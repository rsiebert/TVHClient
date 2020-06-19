package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.data.entity.TimerRecordingEntity
import java.util.*

class TimerRecordingDataSource(private val db: AppRoomDatabase) : DataSourceInterface<TimerRecording> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

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
        return Transformations.map(db.timerRecordingDao.loadAllRecordings()) { entities ->
            entities.map { it.toRecording() }
        }
    }

    override fun getLiveDataItemById(id: Any): LiveData<TimerRecording> {
        return Transformations.map(db.timerRecordingDao.loadRecordingById(id as String)) { entity ->
            entity.toRecording()
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
