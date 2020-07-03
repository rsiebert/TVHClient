package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.ProgramEntity

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
        ioScope.launch { db.programDao.insert(ProgramEntity.from(item)) }
    }

    fun addItems(items: List<Program>) {
        ioScope.launch { db.programDao.insert(ArrayList(items).map { ProgramEntity.from(it) }) }
    }

    override fun updateItem(item: Program) {
        ioScope.launch { db.programDao.update(ProgramEntity.from(item)) }
    }

    override fun removeItem(item: Program) {
        ioScope.launch { db.programDao.delete(ProgramEntity.from(item)) }
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
        return Transformations.map(db.programDao.loadPrograms()) { entities ->
            entities.map { it.toProgram() }
        }
    }

    override fun getLiveDataItemById(id: Any): LiveData<Program> {
        return Transformations.map(db.programDao.loadProgramById(id as Int)) { entity ->
            entity.toProgram()
        }
    }

    override fun getItemById(id: Any): Program? {
        var program: Program? = null
        runBlocking(Dispatchers.IO) {
            program = db.programDao.loadProgramByIdSync(id as Int)?.toProgram()
        }
        return program
    }

    override fun getItems(): List<Program> {
        val programs = ArrayList<Program>()
        runBlocking(Dispatchers.IO) {
            programs.addAll(db.programDao.loadProgramsSync().map { it.toProgram() })
        }
        return programs
    }

    fun getLiveDataItemsFromTime(time: Long): LiveData<List<Program>> {
        return Transformations.map(db.programDao.loadProgramsFromTime(time)) { entities ->
            entities.map { it.toProgram() }
        }
    }

    fun getLiveDataItemByChannelIdAndTime(channelId: Int, time: Long): LiveData<List<Program>> {
        return Transformations.map(db.programDao.loadProgramsFromChannelFromTime(channelId, time)) { entities ->
            entities.map { it.toProgram() }
        }
    }

    fun getItemByChannelIdAndBetweenTime(channelId: Int, startTime: Long, endTime: Long): List<EpgProgram> {
        val programs = ArrayList<EpgProgram>()
        runBlocking(Dispatchers.IO) {
            programs.addAll(db.programDao.loadEpgProgramsFromChannelBetweenTimeSync(channelId, startTime, endTime).map { it.toEpgProgram() })
        }
        return programs
    }

    fun getLastItemByChannelId(channelId: Int): Program? {
        var program: Program? = null
        runBlocking(Dispatchers.IO) {
            program = db.programDao.loadLastProgramFromChannelSync(channelId)?.toProgram()
        }
        return program
    }

    fun getItemsByChannelId(channelId: Int): List<Program> {
        val programs = ArrayList<Program>()
        runBlocking(Dispatchers.IO) {

            val timeStep = 1000L * 3600 * 24 * 2
            val lastProgram = db.programDao.loadLastProgramFromChannelSync(channelId)?.toProgram()
            val startTime = System.currentTimeMillis()
            val endTime = lastProgram?.stop ?: startTime

            // Load the programs in chunks to avoid a SQLiteBlobTooBigException
            for (time in startTime until endTime step timeStep) {
                programs.addAll(db.programDao.loadProgramsFromChannelBetweenTimeSync(channelId, time, time + timeStep).map { it.toProgram() })
            }
        }
        return programs
    }
}
