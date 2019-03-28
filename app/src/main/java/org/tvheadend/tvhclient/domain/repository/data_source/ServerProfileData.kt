package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException

class ServerProfileData(private val db: AppRoomDatabase) : DataSourceInterface<ServerProfile> {

    val recordingProfileNames: Array<String>
        get() = getProfileNames(recordingProfiles)

    val htspPlaybackProfileNames: Array<String>
        get() = getProfileNames(htspPlaybackProfiles)

    val httpPlaybackProfileNames: Array<String>
        get() = getProfileNames(httpPlaybackProfiles)

    val recordingProfiles: List<ServerProfile>
        get() {
            val serverProfiles = ArrayList<ServerProfile>()
            try {
                serverProfiles.addAll(ServerProfileTask(db, RECORDINGS).execute().get())
            } catch (e: InterruptedException) {
                Timber.d(e, "Loading recording server profile task got interrupted")
            } catch (e: ExecutionException) {
                Timber.d(e, "Loading recording server profile task aborted")
            }

            return serverProfiles
        }

    val htspPlaybackProfiles: List<ServerProfile>
        get() {
            val serverProfiles = ArrayList<ServerProfile>()
            try {
                serverProfiles.addAll(ServerProfileTask(db, HTSP_PLAYBACK).execute().get())
            } catch (e: InterruptedException) {
                Timber.d(e, "Loading htsp playback server profile task got interrupted")
            } catch (e: ExecutionException) {
                Timber.d(e, "Loading htsp playback server profile task aborted")
            }

            return serverProfiles
        }

    val httpPlaybackProfiles: List<ServerProfile>
        get() {
            val serverProfiles = ArrayList<ServerProfile>()
            try {
                serverProfiles.addAll(ServerProfileTask(db, HTTP_PLAYBACK).execute().get())
            } catch (e: InterruptedException) {
                Timber.d(e, "Loading http playback server profile task got interrupted")
            } catch (e: ExecutionException) {
                Timber.d(e, "Loading http playback server profile task aborted")
            }

            return serverProfiles
        }

    override fun addItem(item: ServerProfile) {
        AsyncTask.execute { db.serverProfileDao.insert(item) }
    }

    override fun updateItem(item: ServerProfile) {
        AsyncTask.execute { db.serverProfileDao.update(item) }
    }

    override fun removeItem(item: ServerProfile) {
        AsyncTask.execute { db.serverProfileDao.delete(item) }
    }

    fun removeAll() {
        AsyncTask.execute { db.serverProfileDao.deleteAll() }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return MutableLiveData()
    }

    override fun getLiveDataItems(): LiveData<List<ServerProfile>> {
        return MutableLiveData()
    }

    override fun getLiveDataItemById(id: Any): LiveData<ServerProfile> {
        return MutableLiveData()
    }

    override fun getItemById(id: Any): ServerProfile? {
        try {
            return ServerProfileByIdTask(db, id).execute().get()
        } catch (e: InterruptedException) {
            Timber.d(e, "Loading server profile by id task got interrupted")
        } catch (e: ExecutionException) {
            Timber.d(e, "Loading server profile by id task aborted")
        }

        return null
    }

    override fun getItems(): List<ServerProfile> {
        return ArrayList()
    }

    private fun getProfileNames(serverProfiles: List<ServerProfile>): Array<String> {
        if (serverProfiles.isNotEmpty()) {
            return Array(serverProfiles.size) { i -> serverProfiles[i].name ?: "" }
        }
        return Array(0) { "" }
    }

    private class ServerProfileByIdTask internal constructor(private val db: AppRoomDatabase, private val id: Any) : AsyncTask<Void, Void, ServerProfile>() {

        override fun doInBackground(vararg voids: Void): ServerProfile? {
            if (id is Int) {
                return db.serverProfileDao.loadProfileByIdSync(id)
            } else if (id is String) {
                return db.serverProfileDao.loadProfileByUuidSync(id)
            }
            return null
        }
    }

    private class ServerProfileTask internal constructor(private val db: AppRoomDatabase, private val type: Int) : AsyncTask<Void, Void, List<ServerProfile>>() {

        override fun doInBackground(vararg voids: Void): List<ServerProfile>? {
            return when (type) {
                HTSP_PLAYBACK -> db.serverProfileDao.loadHtspPlaybackProfilesSync()
                HTTP_PLAYBACK -> db.serverProfileDao.loadHttpPlaybackProfilesSync()
                RECORDINGS -> db.serverProfileDao.loadAllRecordingProfilesSync()
                else -> null
            }
        }
    }

    companion object {

        private const val RECORDINGS = 1
        private const val HTSP_PLAYBACK = 2
        private const val HTTP_PLAYBACK = 3
    }
}
