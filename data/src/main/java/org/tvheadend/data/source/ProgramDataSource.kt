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
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.ProgramEntity

class ProgramDataSource(
        private val db: AppRoomDatabase,
        private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DataSourceInterface<Program> {

    // See ChannelDataSource: reads follow the injected scope's dispatcher (Dispatchers.IO in
    // production, Unconfined in the unit tests).
    private val ioDispatcher: CoroutineDispatcher =
            ioScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.IO

    suspend fun getItemCount(): Int {
        var count: Int
        withContext(ioDispatcher) {
            count = db.programDao.itemCountSync
        }
        return count
    }

    override fun addItem(item: Program) {
        ioScope.launch { db.programDao.insert(ProgramEntity.from(item)) }
    }

    fun addItems(items: List<Program>) {
        if (items.isNotEmpty()) {
            ioScope.launch {
                db.programDao.insert(ArrayList(items).mapNotNull { program -> program.let { item ->
                    ProgramEntity.from(item) }
                })
            }
        }
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
        return db.programDao.loadPrograms().map { entities ->
            entities.map { it.toProgram() }
        }
    }

    override suspend fun getItemById(id: Any): Program? {
        var program: Program?
        withContext(ioDispatcher) {
            program = db.programDao.loadProgramByIdSync(id as Int)?.toProgram()
        }
        return program
    }

    override suspend fun getItems(): List<Program> {
        val programs = ArrayList<Program>()
        withContext(ioDispatcher) {
            programs.addAll(db.programDao.loadProgramsSync().map { it.toProgram() })
        }
        return programs
    }

    fun getLiveDataItemsFromTime(time: Long): LiveData<List<Program>> {
        return db.programDao.loadProgramsFromTime(time).map { entities ->
            entities.map { it.toProgram() }
        }
    }

    fun getLiveDataItemByChannelIdAndTime(channelId: Int, time: Long): LiveData<List<Program>> {
        return db.programDao.loadProgramsFromChannelFromTime(channelId, time).map { entities ->
            entities.map { it.toProgram() }
        }
    }

    suspend fun getItemByChannelIdAndBetweenTime(channelId: Int, startTime: Long, endTime: Long): List<EpgProgram> {
        val programs = ArrayList<EpgProgram>()
        withContext(ioDispatcher) {
            programs.addAll(db.programDao.loadEpgProgramsFromChannelBetweenTimeSync(channelId, startTime, endTime).map { it.toEpgProgram() })
        }
        return programs
    }

    suspend fun getLastItemByChannelId(channelId: Int): Program? {
        var program: Program?
        withContext(ioDispatcher) {
            program = db.programDao.loadLastProgramFromChannelSync(channelId)?.toProgram()
        }
        return program
    }

    suspend fun getItemsByChannelId(channelId: Int): List<Program> {
        val programs = ArrayList<Program>()
        withContext(ioDispatcher) {

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

    suspend fun getDuplicatePrograms(channelId: Int): List<EpgProgram> {
        val programs = ArrayList<EpgProgram>()
        withContext(ioDispatcher) {
            programs.addAll(db.programDao.loadDuplicateProgramsSync(channelId).map { it.toEpgProgram() })
        }
        return programs
    }
}
