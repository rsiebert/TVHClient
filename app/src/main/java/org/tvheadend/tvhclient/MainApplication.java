package org.tvheadend.tvhclient;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.tvheadend.tvhclient.features.logging.AnswersWrapper;
import org.tvheadend.tvhclient.features.logging.CrashlyticsTree;
import org.tvheadend.tvhclient.features.logging.DebugTree;
import org.tvheadend.tvhclient.features.logging.FileLoggingTree;
import org.tvheadend.tvhclient.features.purchase.BillingUtils;
import org.tvheadend.tvhclient.features.streaming.external.ExpandedControlsActivity;
import org.tvheadend.tvhclient.injection.DaggerMainApplicationComponent;
import org.tvheadend.tvhclient.injection.MainApplicationComponent;
import org.tvheadend.tvhclient.injection.modules.EpgSyncHandlerModule;
import org.tvheadend.tvhclient.injection.modules.MainApplicationModule;
import org.tvheadend.tvhclient.injection.modules.RepositoryModule;
import org.tvheadend.tvhclient.injection.modules.SharedPreferencesModule;
import org.tvheadend.tvhclient.utils.Constants;
import org.tvheadend.tvhclient.utils.MigrateUtils;

import java.util.List;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class MainApplication extends Application implements BillingProcessor.IBillingHandler, OptionsProvider {

    private BillingProcessor billingProcessor;
    private static MainApplication instance;
    private RefWatcher refWatcher;
    @Inject
    protected SharedPreferences sharedPreferences;

    private static MainApplicationComponent component;

    public static synchronized MainApplication getInstance() {
        return instance;
    }

    public static MainApplicationComponent getComponent() {
        return component;
    }

    public static RefWatcher getRefWatcher(Context context) {
        MainApplication application = (MainApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        // Create the component upon start of the app. This component
        // is used by all other classes to inject certain fields
        component = buildComponent();
        // Inject the shared preferences
        component.inject(this);

        // This process is dedicated to LeakCanary for heap analysis.
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        refWatcher = LeakCanary.install(this);

        // Enable stetho to enable accessing the database
        // and other resources via the chrome browser
        if (BuildConfig.DEBUG_MODE) {
            Stetho.initialize(Stetho.newInitializerBuilder(this)
                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                    .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                    .build());
        }

        initCrashlytics();
        initTimber();
        initBilling();

        Timber.d("Application build time is " + BuildConfig.BUILD_TIME + ", git commit hash is " + BuildConfig.GIT_SHA);

        // Migrates existing connections from the old database to the new room database.
        // Migrates existing preferences or remove old ones before starting the actual application
        new MigrateUtils().doMigrate();
    }

    private MainApplicationComponent buildComponent() {
        return DaggerMainApplicationComponent.builder()
                .mainApplicationModule(new MainApplicationModule(this))
                .sharedPreferencesModule(new SharedPreferencesModule())
                .repositoryModule(new RepositoryModule(this))
                .epgSyncHandlerModule(new EpgSyncHandlerModule())
                .build();
    }

    private void initBilling() {
        billingProcessor = new BillingProcessor(this, BillingUtils.getPublicKey(this), this);
        billingProcessor.initialize();
        if (!BillingProcessor.isIabServiceAvailable(this)) {
            Timber.d("Billing not available");
        } else {
            if (!billingProcessor.loadOwnedPurchasesFromGoogle()) {
                Timber.d("Could not load purchase information");
            }
        }
    }

    private void initTimber() {
        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(new DebugTree());
        }

        if (sharedPreferences.getBoolean("debug_mode_enabled", false)) {
            Timber.plant(new FileLoggingTree(getApplicationContext()));
        }
        if (sharedPreferences.getBoolean("crash_reports_enabled", true)) {
            Timber.plant(new CrashlyticsTree());
        }
    }

    private void initCrashlytics() {
        if (sharedPreferences.getBoolean("crash_reports_enabled", true)) {
            Fabric.with(this, new Crashlytics());
        }
        AnswersWrapper.init(this);
    }

    /**
     * Checks if the user has purchased the unlocker from the play store.
     * If yes the application is unlocked then all extra features are accessible.
     *
     * @return True if the application is unlocked otherwise false
     */
    public boolean isUnlocked() {
        return BuildConfig.DEBUG_MODE || billingProcessor.isPurchased(Constants.UNLOCKER);
    }

    @Override
    public void onTerminate() {
        if (billingProcessor != null) {
            billingProcessor.release();
        }
        super.onTerminate();
    }

    /**
     * Returns the billing processor object that can be used by other classes to
     * access billing related features
     *
     * @return The billing processor object
     */
    public BillingProcessor getBillingProcessor() {
        return billingProcessor;
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        // NOP
    }

    @Override
    public void onBillingInitialized() {
        // NOP
    }

    @Override
    public void onProductPurchased(@NonNull String productId, TransactionDetails details) {
        // NOP
    }

    @Override
    public void onPurchaseHistoryRestored() {
        // NOP
    }

    @Override
    public CastOptions getCastOptions(Context context) {
        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setTargetActivityClassName(ExpandedControlsActivity.class.getName())
                .build();
        CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .setExpandedControllerActivityClassName(ExpandedControlsActivity.class.getName())
                .build();

        return new CastOptions.Builder()
                .setReceiverApplicationId(BuildConfig.CAST_ID)
                .setCastMediaOptions(mediaOptions)
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
