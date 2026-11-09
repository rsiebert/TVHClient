package org.tvheadend.tvhclient.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.service.htsp.HtspIntentServiceHandler
import timber.log.Timber
import java.util.concurrent.ScheduledExecutorService

class ConnectionIntentService : JobIntentService() {

    private lateinit var appRepository: AppRepository

    private lateinit var execService: ScheduledExecutorService

    /**
     * Created lazily on the [onHandleWork] background thread so the (blocking) read of the active
     * connection never runs on the main thread. Written on the work thread and read on the main
     * thread in [onDestroy], hence [Volatile].
     */
    @Volatile
    private var serviceHandler: ServiceInterface? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("Starting intent service")
        val application = application as MainApplication
        appRepository = application.appRepository
        execService = application.htspExecutor
    }

    override fun onHandleWork(intent: Intent) {
        // onHandleWork() runs on a background thread, so the handler (and the connection it needs)
        // is created here rather than in onCreate().
        val handler = serviceHandler ?: createServiceHandler()
        handler.onHandleWork(intent)
    }

    private fun createServiceHandler(): ServiceInterface {
        val connection: Connection = runBlocking { appRepository.connectionData.getActiveItem() }
        return HtspIntentServiceHandler(this, appRepository, connection, execService)
                .also { serviceHandler = it }
    }

    override fun onDestroy() {
        Timber.d("Stopping service")
        serviceHandler?.onDestroy()
    }

    companion object {
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, ConnectionIntentService::class.java, 1, work)
        }
    }

    interface ServiceInterface {
        fun onHandleWork(intent: Intent)
        fun onDestroy()
    }
}
