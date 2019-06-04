package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.EpgProgram
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.SearchResultProgram
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.collections.HashMap

class ProgramData(private val db: AppRoomDatabase) : DataSourceInterface<Program> {

    val itemCount: Int
        get() {
            try {
                return ProgramCountTask(db).execute().get()
            } catch (e: InterruptedException) {
                Timber.e(e, "Loading program count task got interrupted")
            } catch (e: ExecutionException) {
                Timber.e(e, "Loading program count task aborted")
            }

            return 0
        }

    override fun addItem(item: Program) {
        AsyncTask.execute { db.programDao.insert(item) }
    }

    fun addItems(items: List<Program>) {
        AsyncTask.execute { db.programDao.insert(ArrayList(items)) }
    }

    override fun updateItem(item: Program) {
        AsyncTask.execute { db.programDao.update(item) }
    }

    override fun removeItem(item: Program) {
        AsyncTask.execute { db.programDao.delete(item) }
    }

    fun removeItemsByTime(time: Long) {
        AsyncTask.execute { db.programDao.deleteProgramsByTime(time) }
    }

    fun removeItemById(id: Int) {
        AsyncTask.execute { db.programDao.deleteById(id) }
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
        try {
            return ProgramByIdTask(db, id as Int, LOAD_BY_ID).execute().get()
        } catch (e: InterruptedException) {
            Timber.e(e, "Loading program by id task got interrupted")
        } catch (e: ExecutionException) {
            Timber.e(e, "Loading program by id task aborted")
        }

        return null
    }

    override fun getItems(): List<Program> {
        val programs = ArrayList<Program>()
        try {
            programs.addAll(ProgramListTask(db).execute().get())
        } catch (e: InterruptedException) {
            Timber.e(e, "Loading all programs task got interrupted")
        } catch (e: ExecutionException) {
            Timber.e(e, "Loading all programs task aborted")
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
        try {
            programs.addAll(EpgProgramByChannelAndTimeTask(db, channelId, startTime, endTime).execute().get())
        } catch (e: InterruptedException) {
            Timber.e(e, "Loading programs by channel and time task got interrupted")
        } catch (e: ExecutionException) {
            Timber.e(e, "Loading programs by channel and time task aborted")
        }

        return programs
    }

    fun getLastItemByChannelId(channelId: Int): Program? {
        try {
            return ProgramByIdTask(db, channelId, LOAD_LAST_IN_CHANNEL).execute().get()
        } catch (e: InterruptedException) {
            Timber.e(e, "Loading last programs in channel task got interrupted")
        } catch (e: ExecutionException) {
            Timber.e(e, "Loading last program in channel task aborted")
        }

        return null
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

    private class ProgramByIdTask internal constructor(private val db: AppRoomDatabase, private val id: Int, private val type: Int) : AsyncTask<Void, Void, Program>() {

        override fun doInBackground(vararg voids: Void): Program? {
            when (type) {
                LOAD_LAST_IN_CHANNEL -> return db.programDao.loadLastProgramFromChannelSync(id)
                LOAD_BY_ID -> return db.programDao.loadProgramByIdSync(id)
            }
            return null
        }
    }

    private class EpgProgramByChannelAndTimeTask internal constructor(private val db: AppRoomDatabase, private val channelId: Int, private val startTime: Long, private val endTime: Long) : AsyncTask<Void, Void, List<EpgProgram>>() {

        override fun doInBackground(vararg voids: Void): List<EpgProgram> {
            return db.programDao.loadProgramsFromChannelBetweenTimeSync(channelId, startTime, endTime)
        }
    }

    private class ProgramListTask internal constructor(private val db: AppRoomDatabase) : AsyncTask<Void, Void, List<Program>>() {

        override fun doInBackground(vararg voids: Void): List<Program> {
            return db.programDao.loadProgramsSync()
        }
    }

    private class ProgramCountTask internal constructor(private val db: AppRoomDatabase) : AsyncTask<Void, Void, Int>() {

        override fun doInBackground(vararg voids: Void): Int? {
            return db.programDao.itemCountSync
        }
    }

    companion object {

        private const val LOAD_LAST_IN_CHANNEL = 1
        private const val LOAD_BY_ID = 2
    }
}
