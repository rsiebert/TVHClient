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
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import io.fabric.sdk.android.Fabric
import org.tvheadend.tvhclient.di.DaggerMainApplicationComponent
import org.tvheadend.tvhclient.di.MainApplicationComponent
import org.tvheadend.tvhclient.di.modules.MainApplicationModule
import org.tvheadend.tvhclient.di.modules.RepositoryModule
import org.tvheadend.tvhclient.di.modules.SharedPreferencesModule
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.ui.features.playback.external.ExpandedControlsActivity
import org.tvheadend.tvhclient.util.MigrateUtils
import org.tvheadend.tvhclient.util.billing.BillingHandler
import org.tvheadend.tvhclient.util.billing.BillingManager
import org.tvheadend.tvhclient.util.billing.BillingManager.UNLOCKER
import org.tvheadend.tvhclient.util.billing.BillingUpdatesListener
import org.tvheadend.tvhclient.util.logging.CrashlyticsTree
import org.tvheadend.tvhclient.util.logging.DebugTree
import org.tvheadend.tvhclient.util.logging.FileLoggingTree
import timber.log.Timber
import javax.inject.Inject

// TODO move diffutils to background thread
// TODO use coroutines for certain stuff
// TODO Use paged loading
// TODO Move the variable programIdToBeEditedWhenBeingRecorded into the viewmodels
// TODO use a base viewmodel with generics
// TODO consolidate the dialog strings
// TODO make the 12 hour check to start the epg background worker a setting

// TODO load startup fragment in main activity, continue to load first fragment if all ok and viewmodel loaded data
// TODO use viewpager2 in epg
// TODO reduce number of used async loads in source_data, use livedata where possible

class MainApplication : MultiDexApplication(), OptionsProvider, BillingUpdatesListener {

    lateinit var billingManager: BillingManager
    lateinit var billingHandler: BillingHandler
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * Checks if the user has purchased the unlocker from the play store.
     * If yes the application is unlocked then all extra features are accessible.
     *
     * @return True if the application is unlocked otherwise false
     */
    var isUnlocked: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Create the component upon start of the app. This component
        // is used by all other classes to inject certain fields
        component = buildComponent()
        component.inject(this)

        billingHandler = BillingHandler()
        billingHandler.addListener(this)

        // This process is dedicated to LeakCanary for heap analysis.
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        refWatcher = LeakCanary.install(this)

        // Enable stetho to enable accessing the database
        // and other resources via the chrome browser
        if (BuildConfig.DEBUG) {
            Stetho.initialize(Stetho.newInitializerBuilder(this)
                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                    .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                    .build())
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        initCrashlytics()
        initTimber()
        initBilling()

        Timber.d("Application build time is ${BuildConfig.BUILD_TIME}, git commit hash is ${BuildConfig.GIT_SHA}")

        // Migrates existing connections from the old database to the new room database.
        // Migrates existing preferences or remove old ones before starting the actual application
        MigrateUtils().doMigrate()
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    private fun buildComponent(): MainApplicationComponent {
        return DaggerMainApplicationComponent.builder()
                .mainApplicationModule(MainApplicationModule(this))
                .sharedPreferencesModule(SharedPreferencesModule())
                .repositoryModule(RepositoryModule(this))
                .build()
    }

    private fun initBilling() {
        billingManager = BillingManager(this.applicationContext, billingHandler)
        billingManager.queryPurchases()
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG || BuildConfig.DEBUG_LOG) {
            Timber.plant(DebugTree())
        }

        if (sharedPreferences.getBoolean("debug_mode_enabled", resources.getBoolean(R.bool.pref_default_debug_mode_enabled))) {
            Timber.plant(FileLoggingTree(applicationContext))
        }
        if (sharedPreferences.getBoolean("crash_reports_enabled", resources.getBoolean(R.bool.pref_default_crash_reports_enabled))) {
            Timber.plant(CrashlyticsTree())
        }
    }

    private fun initCrashlytics() {
        // Initialize Fabric with the debug-disabled crashlytics.
        if (!BuildConfig.DEBUG && sharedPreferences.getBoolean("crash_reports_enabled", resources.getBoolean(R.bool.pref_default_crash_reports_enabled))) {
            Fabric.with(this, Crashlytics())
        }
    }

    override fun onTerminate() {
        billingHandler.removeListener(this)
        super.onTerminate()
    }

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

    override fun onPurchaseSuccessful(purchases: List<Purchase>?) {
        Timber.d("Purchase was successful")
        if (purchases != null) {
            for (purchase in purchases) {
                if (purchase.sku == UNLOCKER) {
                    Timber.d("Received purchase item $UNLOCKER")
                    isUnlocked = true
                }
            }
        }
    }

    override fun onPurchaseCancelled() {
        Timber.d("Purchase was successful")
    }

    override fun onPurchaseError(errorCode: Int) {
        Timber.d("Purchase was not successful")
    }

    companion object {

        @get:Synchronized
        lateinit var instance: MainApplication

        lateinit var component: MainApplicationComponent

        lateinit var refWatcher: RefWatcher
    }
}
