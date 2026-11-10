package org.tvheadend.tvhclient

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.tvheadend.data.AppRepository
import org.tvheadend.data.di.DataContainer
import org.tvheadend.tvhclient.debug.FakeServerBootstrap
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.ui.features.playback.external.ExpandedControlsActivity
import org.tvheadend.tvhclient.util.HtspExecutor
import org.tvheadend.tvhclient.util.MigrateUtils
import org.tvheadend.tvhclient.util.billing.BillingDataSource
import org.tvheadend.tvhclient.util.logging.DebugTree
import org.tvheadend.tvhclient.util.logging.FileLoggingTree
import timber.log.Timber
import java.util.concurrent.ScheduledExecutorService

// TODO snackbar locale changes
// TODO when a notification is dismissed, it reappears when the recording gets updated,
//  save the dismissed id in the viewmodel and don't add another notification if the id was already dismissed

class MainApplication : Application(), OptionsProvider, AppGraph {

    lateinit var appContainer: AppContainer

    /** Root of the hand rolled dependency graph, replacing the former Dagger components. */
    val dataContainer: DataContainer by lazy { DataContainer(this) }

    override val appRepository: AppRepository
        get() = dataContainer.appRepository

    override val mainRepository: MainRepository
        get() = appContainer.mainRepository

    override val htspExecutor: ScheduledExecutorService = HtspExecutor.create()

    val appSharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    /**
     * Application-lifetime scope for work that must outlive any screen.
     *
     * Backed by a [SupervisorJob] so a failure in one child does not cancel the whole scope, and by
     * [Dispatchers.Default] so launches start off the main thread. Replaces the previous
     * `GlobalScope`, which was unstructured and could never be cancelled.
     */
    internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * The job that runs the one-off database migration (and the debug bootstrap) off the main
     * thread. The startup flow waits for it before it lets the UI query the database, so nothing
     * observes pre-migration data.
     */
    lateinit var migrationJob: Job
        private set

    inner class AppContainer {
        private val billingDataSource = BillingDataSource.getInstance(
            this@MainApplication,
            applicationScope,
            MainRepository.INAPP_SKUS,
            null,
            null
        )
        val mainRepository = MainRepository(
            billingDataSource,
            applicationScope
        )
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        // Log to the console only in debug builds. Planting the debug tree unconditionally keeps
        // every Timber call site (and the string it builds) alive and printing in release builds.
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        // Log to a file when in release mode and the user has activated the setting
        if (!BuildConfig.DEBUG && appSharedPreferences.getBoolean("debug_mode_enabled", resources.getBoolean(R.bool.pref_default_debug_mode_enabled))) {
            Timber.plant(FileLoggingTree(applicationContext))
        }

        appContainer = AppContainer()
        // Publish the graph so ViewModels can resolve it without casting the Application.
        AppGraphProvider.graph = this
        Timber.d("Application build time is ${BuildConfig.BUILD_TIME}, git commit hash is ${BuildConfig.GIT_SHA}")

        // Build the data graph eagerly on the main thread. AppRepository writes to a LiveData in its
        // constructor, and LiveData may only be written from the main thread, so it must not be
        // created for the first time from the migration coroutine below.
        val repository = appRepository

        // Execute some additional tasks before starting the application.
        // These tasks are for example migrating connections, updating or
        // removing preferences, removing old information from the database and others.
        // They run off the main thread; the startup flow waits for [migrationJob] so that the
        // database is never read before the migration that rewrites it has finished.
        migrationJob = applicationScope.launch {
            MigrateUtils(applicationContext, repository, appSharedPreferences).doMigrate()

            // Debug builds only (no-op in release). Starts an in-process fake TVHeadend server and
            // adds the demo connections pointing at it (adding any that are missing) so a fresh
            // debug install shows channels, EPG data and recordings without a real server, and so
            // the multi-connection flows can be exercised. It runs after the migration because it
            // depends on the migrated connection data.
            FakeServerBootstrap.start(this@MainApplication)
        }
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    /**
     * Provides CastOptions, which affects discovery and session management of a Cast device
     *
     * @param context Required context object
     * @return CastOptions
     */
    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
                .setTargetActivityClassName(ExpandedControlsActivity::class.java.name)
                .build()
        val mediaOptions = CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .setExpandedControllerActivityClassName(ExpandedControlsActivity::class.java.name)
                .build()

        return CastOptions.Builder()
                .setReceiverApplicationId(BuildConfig.CAST_ID)
                .setCastMediaOptions(mediaOptions)
                .build()
    }

    /**
     * Provides a list of custom SessionProvider instances for non-Cast devices.
     *
     * @param context Required context object
     * @return List of session providers
     */
    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }

    companion object {

        lateinit var instance: MainApplication
    }
}
