package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerProfileEntity
import java.util.*

class ServerProfileDataSource(
        private val db: AppRoomDatabase,
        private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DataSourceInterface<ServerProfile> {

    // See ChannelDataSource: reads follow the injected scope's dispatcher (Dispatchers.IO in
    // production, Unconfined in the unit tests).
    private val ioDispatcher: CoroutineDispatcher =
            ioScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.IO

    suspend fun getRecordingProfileNames(): Array<String> {
        return getProfileNames(getRecordingProfiles())
    }

    suspend fun getRecordingProfiles(): List<ServerProfile> {
        val serverProfiles = ArrayList<ServerProfile>()
        withContext(ioDispatcher) {
            serverProfiles.addAll(db.serverProfileDao.loadAllRecordingProfilesSync().map { it.toServerProfile() })
        }
        return serverProfiles
    }

    suspend fun getHtspPlaybackProfiles(): List<ServerProfile> {
        val serverProfiles = ArrayList<ServerProfile>()
        withContext(ioDispatcher) {
            serverProfiles.addAll(db.serverProfileDao.loadHtspPlaybackProfilesSync().map { it.toServerProfile() })
        }
        return serverProfiles
    }

    suspend fun getHttpPlaybackProfiles(): List<ServerProfile> {
        val serverProfiles = ArrayList<ServerProfile>()
        withContext(ioDispatcher) {
            serverProfiles.addAll(db.serverProfileDao.loadHttpPlaybackProfilesSync().map { it.toServerProfile() })
        }
        return serverProfiles
    }

    override fun addItem(item: ServerProfile) {
        ioScope.launch { db.serverProfileDao.insert(ServerProfileEntity.from(item)) }
    }

    fun addItems(items: List<ServerProfile>) {
        ioScope.launch { db.serverProfileDao.insert(items.map { ServerProfileEntity.from(it) }) }
    }

    override fun updateItem(item: ServerProfile) {
        ioScope.launch { db.serverProfileDao.update(ServerProfileEntity.from(item)) }
    }

    override fun removeItem(item: ServerProfile) {
        ioScope.launch { db.serverProfileDao.delete(ServerProfileEntity.from(item)) }
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

    override suspend fun getItemById(id: Any): ServerProfile? {
        var serverProfile: ServerProfile? = null
        withContext(ioDispatcher) {
            if (id is Int) {
                serverProfile = db.serverProfileDao.loadProfileByIdSync(id)?.toServerProfile()
            } else if (id is String) {
                serverProfile = db.serverProfileDao.loadProfileByUuidSync(id)?.toServerProfile()
            }
        }
        return serverProfile
    }

    override suspend fun getItems(): List<ServerProfile> {
        return ArrayList()
    }

    private fun getProfileNames(serverProfiles: List<ServerProfile>): Array<String> {
        if (serverProfiles.isNotEmpty()) {
            return Array(serverProfiles.size) { i -> serverProfiles[i].name ?: "" }
        }
        return Array(0) { "" }
    }
}
