package org.tvheadend.tvhclient.domain.repository.data_source

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.ui.features.settings.DatabaseClearedCallback
import timber.log.Timber
import java.lang.ref.WeakReference

class MiscData(private val db: AppRoomDatabase) {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun clearDatabase(callback: DatabaseClearedCallback) {
        MiscData.callback = WeakReference(callback)
        ioScope.launch {
            clearDatabase()
        }
    }

    private fun clearDatabase() {
        Timber.d("Deleting database contents...")

        db.channelDao.deleteAll()
        db.channelTagDao.deleteAll()
        db.tagAndChannelDao.deleteAll()
        db.programDao.deleteAll()
        db.recordingDao.deleteAll()
        db.seriesRecordingDao.deleteAll()
        db.timerRecordingDao.deleteAll()
        db.serverProfileDao.deleteAll()

        // Clear all assigned profiles
        for (connection in db.connectionDao.loadAllConnectionsSync()) {
            connection.lastUpdate = 0
            connection.isSyncRequired = true
            db.connectionDao.update(connection)

            val serverStatus = db.serverStatusDao.loadServerStatusByIdSync(connection.id)
            serverStatus.htspPlaybackServerProfileId = 0
            serverStatus.httpPlaybackServerProfileId = 0
            serverStatus.castingServerProfileId = 0
            serverStatus.recordingServerProfileId = 0
            db.serverStatusDao.update(serverStatus)
        }

        Timber.d("Deleting database contents finished")
        callback?.get()?.onDatabaseCleared()
    }

    companion object {
        private var callback: WeakReference<DatabaseClearedCallback>? = null
    }
}
