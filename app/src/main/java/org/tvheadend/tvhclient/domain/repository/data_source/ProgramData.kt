package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.EpgProgram
import org.tvheadend.tvhclient.domain.entity.Program
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException

class ProgramData(private val db: AppRoomDatabase) : DataSourceInterface<Program> {

    val itemCount: Int
        get() {
            try {
                return ProgramCountTask(db).execute().get()
            } catch (e: InterruptedException) {
                Timber.d("Loading program count task got interrupted", e)
            } catch (e: ExecutionException) {
                Timber.d("Loading program count task aborted", e)
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
            Timber.d("Loading program by id task got interrupted", e)
        } catch (e: ExecutionException) {
            Timber.d("Loading program by id task aborted", e)
        }

        return null
    }

    override fun getItems(): List<Program> {
        val programs = ArrayList<Program>()
        try {
            programs.addAll(ProgramListTask(db).execute().get())
        } catch (e: InterruptedException) {
            Timber.d("Loading all programs task got interrupted", e)
        } catch (e: ExecutionException) {
            Timber.d("Loading all programs task aborted", e)
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
        try {
            programs.addAll(EpgProgramByChannelAndTimeTask(db, channelId, startTime, endTime).execute().get())
        } catch (e: InterruptedException) {
            Timber.d("Loading programs by channel and time task got interrupted", e)
        } catch (e: ExecutionException) {
            Timber.d("Loading programs by channel and time task aborted", e)
        }

        return programs
    }

    fun getLastItemByChannelId(channelId: Int): Program? {
        try {
            return ProgramByIdTask(db, channelId, LOAD_LAST_IN_CHANNEL).execute().get()
        } catch (e: InterruptedException) {
            Timber.d("Loading last programs in channel task got interrupted", e)
        } catch (e: ExecutionException) {
            Timber.d("Loading last program in channel task aborted", e)
        }

        return null
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
