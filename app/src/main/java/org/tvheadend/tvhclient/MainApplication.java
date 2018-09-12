package org.tvheadend.tvhclient;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.facebook.stetho.Stetho;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.tvheadend.tvhclient.features.logging.CrashlyticsTree;
import org.tvheadend.tvhclient.features.logging.DebugTree;
import org.tvheadend.tvhclient.features.logging.FileLoggingTree;
import org.tvheadend.tvhclient.features.logging.ReleaseTree;
import org.tvheadend.tvhclient.features.purchase.BillingUtils;
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
        Timber.d("start");

        instance = this;
        // Create the component upon start of the app. This component
        // is used by all other classes to inject certain fields
        component = buildComponent();
        // Inject the shared preferences
        component.inject(this);

        if (BuildConfig.DEBUG_MODE) {
            Stetho.initialize(Stetho.newInitializerBuilder(this)
                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                    .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                    .build());
        }

        if (sharedPreferences.getBoolean("crash_reports_enabled", true)) {
            Fabric.with(this, new Crashlytics());
            Crashlytics.setString("Git commit", BuildConfig.GIT_SHA);
            Crashlytics.setString("Build time", BuildConfig.BUILD_TIME);
        }

        if (sharedPreferences.getBoolean("usage_statistics_enabled", true)) {
            Fabric.with(this, new Answers());
        }

        // This process is dedicated to LeakCanary for heap analysis.
        // You should not init your app in this process.
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        refWatcher = LeakCanary.install(this);

        initLogging();
        initBilling();

        // Migrates existing connections from the old database to the new room database.
        // Migrates existing preferences or remove old ones before starting the actual application
        new MigrateUtils().doMigrate();

        Timber.d("end");
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
        if (!BillingProcessor.isIabServiceAvailable(this)) {
            Timber.d("Billing not available");
        } else {
            if (!billingProcessor.loadOwnedPurchasesFromGoogle()) {
                Timber.d("Could not load purchase information");
            }
        }
    }

    private void initLogging() {
        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(new DebugTree());
        } else {
            Timber.plant(new ReleaseTree());
        }

        if (sharedPreferences.getBoolean("debug_mode_enabled", false)) {
            Timber.plant(new FileLoggingTree(getApplicationContext()));
        }
        if (sharedPreferences.getBoolean("crash_reports_enabled", true)) {
            Timber.plant(new CrashlyticsTree());
        }
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
        String msg;
        if (billingProcessor.isValidTransactionDetails(details)) {
            msg = getString(R.string.unlocker_purchase_successful);
        } else {
            msg = getString(R.string.unlocker_purchase_not_successful);
        }

        Intent intent = new Intent("message");
        intent.putExtra("message", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onPurchaseHistoryRestored() {
        // NOP
    }

    @Override
    public CastOptions getCastOptions(Context context) {
        // TODO check if the CAST_ID can be moved to build configs
        return new CastOptions.Builder()
                .setReceiverApplicationId("0531DF56")
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
