package org.tvheadend.tvhclient

import android.content.Context
import android.content.SharedPreferences
import androidx.multidex.MultiDexApplication
import com.android.billingclient.api.Purchase
import com.crashlytics.android.Crashlytics
import com.facebook.stetho.Stetho
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.google.firebase.analytics.FirebaseAnalytics
import io.fabric.sdk.android.Fabric
import org.tvheadend.data.AppRepository
import org.tvheadend.data.di.DaggerRepositoryComponent
import org.tvheadend.data.di.RepositoryModule
import org.tvheadend.tvhclient.di.component.DaggerMainComponent
import org.tvheadend.tvhclient.di.component.MainComponent
import org.tvheadend.tvhclient.di.module.ContextModule
import org.tvheadend.tvhclient.di.module.SharedPreferencesModule
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.ui.features.playback.external.ExpandedControlsActivity
import org.tvheadend.tvhclient.util.MigrateUtils
import org.tvheadend.tvhclient.util.billing.BillingHandler
import org.tvheadend.tvhclient.util.billing.BillingManager
import org.tvheadend.tvhclient.util.billing.BillingUpdatesListener
import org.tvheadend.tvhclient.util.logging.CrashlyticsTree
import org.tvheadend.tvhclient.util.logging.DebugTree
import org.tvheadend.tvhclient.util.logging.FileLoggingTree
import timber.log.Timber
import javax.inject.Inject

// TODO snackbar locale changes
// TODO day night theme
// TODO replace material dialog calls with native ones
// TODO when a notification is dismissed, it reappears when the recording gets updated,
//  save the dismissed id in the viewmodel and dont add another notification if the id was already dismissed

class MainApplication : MultiDexApplication(), OptionsProvider, BillingUpdatesListener {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var fireBaseAnalytics: FirebaseAnalytics

    override fun onCreate() {
        super.onCreate()

        val repositoryComponent = DaggerRepositoryComponent
                .builder()
                .repositoryModule(RepositoryModule(applicationContext))
                .build()

        // Setup the required dependencies for injection
        component = DaggerMainComponent
                .builder()
                .contextModule(ContextModule(applicationContext))
                .sharedPreferencesModule(SharedPreferencesModule())
                .repositoryComponent(repositoryComponent)
                .build()
        component.inject(this)

        instance = this

        // Enable the database debugging bridge in debug mode to access
        // the database contents and other resources via the chrome browser
        if (BuildConfig.DEBUG) {
            Stetho.initialize(Stetho.newInitializerBuilder(this)
                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                    .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                    .build())
        }

        fireBaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Initialize Fabric with the debug-disabled crashlytics.
        if (!BuildConfig.DEBUG && sharedPreferences.getBoolean("crash_reports_enabled", resources.getBoolean(R.bool.pref_default_crash_reports_enabled))) {
            Fabric.with(this, Crashlytics())
        }
        // Initialize the logging. In release mode Log to theUse debug log
        if (BuildConfig.DEBUG || BuildConfig.DEBUG_LOG) {
            Timber.plant(DebugTree())
        }
        // Log to a file when in release mode and the user has activated the setting
        if (!BuildConfig.DEBUG && sharedPreferences.getBoolean("debug_mode_enabled", resources.getBoolean(R.bool.pref_default_debug_mode_enabled))) {
            Timber.plant(FileLoggingTree(applicationContext))
        }
        // Log any non fatal crashes via crashlytics when the app is in release mode and the user has activated the setting
        if (!BuildConfig.DEBUG && sharedPreferences.getBoolean("crash_reports_enabled", resources.getBoolean(R.bool.pref_default_crash_reports_enabled))) {
            Timber.plant(CrashlyticsTree())
        }

        billingHandler = BillingHandler()
        billingHandler.addListener(this)
        billingManager = BillingManager(billingHandler)
        billingManager.queryPurchases()

        Timber.d("Application build time is ${BuildConfig.BUILD_TIME}, git commit hash is ${BuildConfig.GIT_SHA}")

        // Migrates existing connections from the old database to the new room database.
        // Migrates existing preferences or remove old ones before starting the actual application
        MigrateUtils(applicationContext, appRepository, sharedPreferences).doMigrate()
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    override fun onTerminate() {
        billingHandler.removeListener(this)
        super.onTerminate()
    }

    /**
     * Provides CastOptions, which affects discovery and session management of a Cast device
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
     */
    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }

    override fun onBillingClientSetupFinished() {
        Timber.d("Billing client setup has finished")
        billingManager.queryPurchases()
    }

    override fun onConsumeFinished(token: String, result: Int) {
        Timber.d("Token $token has been consumed with result $result")
    }

    override fun onPurchaseSuccessful(purchases: List<Purchase>) {
        Timber.d("Purchase was successful")
    }

    override fun onPurchaseCancelled() {
        Timber.d("Purchase was successful")
    }

    override fun onPurchaseError(errorCode: Int) {
        Timber.d("Purchase was not successful")
    }

    companion object {

        lateinit var instance: MainApplication
        lateinit var component: MainComponent
        lateinit var billingManager: BillingManager
        lateinit var billingHandler: BillingHandler
    }
}
