package org.tvheadend.tvhclient.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.data.entity.TimerRecording
import java.util.*

class TimerRecordingDataSource(private val db: AppRoomDatabase) : DataSourceInterface<TimerRecording> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun addItem(item: TimerRecording) {
        ioScope.launch { db.timerRecordingDao.insert(item) }
    }

    override fun updateItem(item: TimerRecording) {
        ioScope.launch { db.timerRecordingDao.update(item) }
    }

    override fun removeItem(item: TimerRecording) {
        ioScope.launch { db.timerRecordingDao.delete(item) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.timerRecordingDao.itemCount
    }

    override fun getLiveDataItems(): LiveData<List<TimerRecording>> {
        return db.timerRecordingDao.loadAllRecordings()
    }

    override fun getLiveDataItemById(id: Any): LiveData<TimerRecording> {
        return db.timerRecordingDao.loadRecordingById(id as String)
    }

    override fun getItemById(id: Any): TimerRecording? {
        var timerRecording: TimerRecording? = null
        if ((id as String).isNotEmpty()) {
            runBlocking(Dispatchers.IO) {
                timerRecording = db.timerRecordingDao.loadRecordingByIdSync(id)
            }
        }
        return timerRecording
    }

    override fun getItems(): List<TimerRecording> {
        return ArrayList()
    }
}
