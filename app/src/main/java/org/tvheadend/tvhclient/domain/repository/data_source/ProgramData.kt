package org.tvheadend.tvhclient.domain.repository.data_source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.EpgProgram
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.SearchResultProgram
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

class ProgramData(private val db: AppRoomDatabase) : DataSourceInterface<Program> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val itemCount: Int
        get() {
            var count = 0
            runBlocking(Dispatchers.IO) {
                count = db.programDao.itemCountSync
            }
            return count
        }

    override fun addItem(item: Program) {
        ioScope.launch { db.programDao.insert(item) }
    }

    fun addItems(items: List<Program>) {
        ioScope.launch { db.programDao.insert(ArrayList(items)) }
    }

    override fun updateItem(item: Program) {
        ioScope.launch { db.programDao.update(item) }
    }

    override fun removeItem(item: Program) {
        ioScope.launch { db.programDao.delete(item) }
    }

    fun removeItemsByTime(time: Long) {
        ioScope.launch { db.programDao.deleteProgramsByTime(time) }
    }

    fun removeItemById(id: Int) {
        ioScope.launch { db.programDao.deleteById(id) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.programDao.itemCount
    }

    override fun getLiveDataItems(): LiveData<List<Program>> {
        return db.programDao.loadPrograms()
    }

    override fun getLiveDataItemById(id: Any): LiveData<Program> {
        return db.programDao.loadProgramById(id as Int)
    }

    override fun getItemById(id: Any): Program? {
        var program: Program? = null
        runBlocking(Dispatchers.IO) {
            program = db.programDao.loadProgramByIdSync(id as Int)
        }
        return program
    }

    override fun getItems(): List<Program> {
        val programs = ArrayList<Program>()
        runBlocking(Dispatchers.IO) {
            programs.addAll(db.programDao.loadProgramsSync())
        }
        return programs
    }

    fun getLiveDataItemsFromTime(time: Long): LiveData<List<SearchResultProgram>> {
        return db.programDao.loadProgramsFromTime(time)
    }

    fun getLiveDataItemByChannelIdAndTime(channelId: Int, time: Long): LiveData<List<Program>> {
        return db.programDao.loadProgramsFromChannelFromTime(channelId, time)
    }

    fun getItemByChannelIdAndBetweenTime(channelId: Int, startTime: Long, endTime: Long): List<EpgProgram> {
        val programs = ArrayList<EpgProgram>()
        runBlocking(Dispatchers.IO) {
            programs.addAll(db.programDao.loadProgramsFromChannelBetweenTimeSync(channelId, startTime, endTime))
        }
        return programs
    }

    fun getLastItemByChannelId(channelId: Int): Program? {
        var program: Program? = null
        runBlocking(Dispatchers.IO) {
            program = db.programDao.loadLastProgramFromChannelSync(channelId)
        }
        return program
    }

    suspend fun getEpgItemsBetweenTime(order: Int, hours: Int, days: Int): HashMap<Int, List<EpgProgram>> {
        // Get the current time in milliseconds without the seconds but in 30
        // minute slots. If the current time is later then 16:30 start from
        // 16:30 otherwise from 16:00.
        val minutes = if (Calendar.getInstance().get(Calendar.MINUTE) > 30) 30 else 0
        val calendarStartTime = Calendar.getInstance()
        calendarStartTime.set(Calendar.MINUTE, minutes)
        calendarStartTime.set(Calendar.SECOND, 0)

        // Get the offset time in milliseconds without the minutes and seconds
        val offsetTime = (hours * 60 * 60 * 1000).toLong()
        val startTime = calendarStartTime.timeInMillis
        val endTime = startTime + (offsetTime * (days * (24 / hours))) - 1

        val epgData: HashMap<Int, List<EpgProgram>> = HashMap()
        val channels = db.channelDao.loadAllEpgChannelsSync(order)

        Timber.d("Loading programs for ${channels.size} channels between $startTime and $endTime")
        (0 until channels.size).forEach { i ->
            val programs = db.programDao.loadProgramsFromChannelBetweenTimeSyncSuspendable(channels[i].id, startTime, endTime)
            Timber.d("Loaded ${programs.size} programs for channel ${channels[i].name}")
            epgData[channels[i].id] = programs
        }
        return epgData
    }
}
