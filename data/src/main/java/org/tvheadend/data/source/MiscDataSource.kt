package org.tvheadend.data.source

import android.annotation.SuppressLint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tvheadend.data.db.AppRoomDatabase
import timber.log.Timber
import java.lang.ref.WeakReference

class MiscDataSource(private val db: AppRoomDatabase) {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun clearDatabase(callback: DatabaseClearedCallback) {
        MiscDataSource.callback = WeakReference(callback)
        ioScope.launch {
            clearDatabase()
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    private fun clearDatabase() {

        Timber.d("Deleting from database ${db.channelDao.itemCountSync} channels,\n" +
                "${db.channelTagDao.itemCountSync} channel tags,\n" +
                "${db.programDao.itemCountSync} programs,\n" +
                "${db.recordingDao.itemCountSync} recordings,\n" +
                "${db.seriesRecordingDao.itemCountSync} series recordings,\n" +
                "${db.timerRecordingDao.itemCountSync} timer recordings,\n" +
                "${db.serverProfileDao.itemCountSync} server profiles")

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

        Timber.d("Deleting of database contents done.\n" +
                "Database contains ${db.channelDao.itemCountSync} channels,\n" +
                "${db.channelTagDao.itemCountSync} channel tags,\n" +
                "${db.programDao.itemCountSync} programs,\n" +
                "${db.recordingDao.itemCountSync} recordings,\n" +
                "${db.seriesRecordingDao.itemCountSync} series recordings,\n" +
                "${db.timerRecordingDao.itemCountSync} timer recordings,\n" +
                "${db.serverProfileDao.itemCountSync} server profiles")

        callback?.get()?.onDatabaseCleared()
    }

    companion object {
        private var callback: WeakReference<DatabaseClearedCallback>? = null
    }

    interface DatabaseClearedCallback {
        fun onDatabaseCleared()
    }
}
