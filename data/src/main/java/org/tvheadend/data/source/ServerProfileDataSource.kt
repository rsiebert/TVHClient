package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ServerProfile
import java.util.*

class ServerProfileDataSource(private val db: AppRoomDatabase) : DataSourceInterface<ServerProfile> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val recordingProfileNames: Array<String>
        get() = getProfileNames(recordingProfiles)

    val htspPlaybackProfileNames: Array<String>
        get() = getProfileNames(htspPlaybackProfiles)

    val httpPlaybackProfileNames: Array<String>
        get() = getProfileNames(httpPlaybackProfiles)

    val recordingProfiles: List<ServerProfile>
        get() {
            val serverProfiles = ArrayList<ServerProfile>()
            runBlocking(Dispatchers.IO) {
                serverProfiles.addAll(db.serverProfileDao.loadAllRecordingProfilesSync())
            }
            return serverProfiles
        }

    val htspPlaybackProfiles: List<ServerProfile>
        get() {
            val serverProfiles = ArrayList<ServerProfile>()
            runBlocking(Dispatchers.IO) {
                serverProfiles.addAll(db.serverProfileDao.loadHtspPlaybackProfilesSync())
            }
            return serverProfiles
        }

    val httpPlaybackProfiles: List<ServerProfile>
        get() {
            val serverProfiles = ArrayList<ServerProfile>()
            runBlocking(Dispatchers.IO) {
                serverProfiles.addAll(db.serverProfileDao.loadHttpPlaybackProfilesSync())
            }
            return serverProfiles
        }

    override fun addItem(item: ServerProfile) {
        ioScope.launch { db.serverProfileDao.insert(item) }
    }

    override fun updateItem(item: ServerProfile) {
        ioScope.launch { db.serverProfileDao.update(item) }
    }

    override fun removeItem(item: ServerProfile) {
        ioScope.launch { db.serverProfileDao.delete(item) }
    }

    fun removeAll() {
        ioScope.launch { db.serverProfileDao.deleteAll() }
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
        var serverProfile: ServerProfile? = null
        runBlocking(Dispatchers.IO) {
            if (id is Int) {
                serverProfile = db.serverProfileDao.loadProfileByIdSync(id)
            } else if (id is String) {
                serverProfile = db.serverProfileDao.loadProfileByUuidSync(id)
            }
        }
        return serverProfile
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
}
