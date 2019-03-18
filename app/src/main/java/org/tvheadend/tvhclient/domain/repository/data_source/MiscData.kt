package org.tvheadend.tvhclient.domain.repository.data_source

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.ui.features.settings.DatabaseClearedCallback
import timber.log.Timber
import java.lang.ref.WeakReference

class MiscData(private val db: AppRoomDatabase) {

    fun clearDatabase(context: Context, callback: DatabaseClearedCallback) {
        MiscData.callback = WeakReference(callback)
        ClearDatabaseTask(context, db).execute()
    }

    private class ClearDatabaseTask internal constructor(context: Context, private val db: AppRoomDatabase) : AsyncTask<Void, Void, Void>() {
        private val dialog: ProgressDialog = ProgressDialog(context)
        private val msg: String = context.getString(R.string.deleting_database_contents)

        override fun onPreExecute() {
            dialog.setMessage(msg)
            dialog.isIndeterminate = true
            dialog.show()
        }

        override fun doInBackground(vararg voids: Void): Void? {
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
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            Timber.d("Deleting database contents finished")
            dialog.dismiss()
            callback?.get()?.onDatabaseCleared()
        }
    }

    companion object {
        private var callback: WeakReference<DatabaseClearedCallback>? = null
    }
}
