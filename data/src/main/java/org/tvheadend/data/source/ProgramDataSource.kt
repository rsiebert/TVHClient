package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.Program

class ProgramDataSource(private val db: AppRoomDatabase) : DataSourceInterface<Program> {

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

    fun getLiveDataItemsFromTime(time: Long): LiveData<List<Program>> {
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

    fun getItemsByChannelId(id: Int): List<Program> {
        val programs = ArrayList<Program>()
        runBlocking(Dispatchers.IO) {
            programs.addAll(db.programDao.loadProgramsFromChannelSync(id))
        }
        return programs
    }
}
