package org.tvheadend.tvhclient;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.acra.ACRA;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.utils.BillingUtils;
import org.tvheadend.tvhclient.utils.MigrateUtils;

import java.util.ArrayList;
import java.util.List;

public class TVHClientApplication extends Application implements BillingProcessor.IBillingHandler, OptionsProvider {
    private final static String TAG = TVHClientApplication.class.getSimpleName();

    private final List<HTSListener> listeners = new ArrayList<>();
    // This handles all billing related activities like purchasing and checking
    // if a purchase was made 
    private BillingProcessor billingProcessor;

    private Logger logger = null;
    private static TVHClientApplication instance;

    public static synchronized TVHClientApplication getInstance() {
        return instance;
    }

    /**
     * Adds a single listener to the list.
     *
     * @param listener Listener class
     */
    public void addListener(HTSListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a single listener from the list.
     *
     * @param listener Listener class
     */
    public void removeListener(HTSListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }


    /**
     * Sends the given action and possible object with the data to all
     * registered listeners.
     *
     * @param action String that defines the action
     * @param obj Object that contains data
     */
    public void broadcastMessage(final String action, final Object obj) {
        synchronized (listeners) {
            for (HTSListener l : listeners) {
                l.onMessage(action, obj);
            }
        }
    }

    private RefWatcher refWatcher;

    public static RefWatcher getRefWatcher(Context context) {
        TVHClientApplication application = (TVHClientApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        refWatcher = LeakCanary.install(this);

        instance = this;
        logger = Logger.getInstance();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("pref_debug_mode", false)) {
            logger.enableLogToFile();
        }

        billingProcessor = new BillingProcessor(this, BillingUtils.getPublicKey(this), this);
        if (!BillingProcessor.isIabServiceAvailable(this)) {
            logger.log(TAG, "onCreate: billing not available");
        } else {
            if (!billingProcessor.loadOwnedPurchasesFromGoogle()) {
                logger.log(TAG, "onCreate: Could not load purchase information");
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        if (BuildConfig.ACRA_ENABLED) {
            Log.d(TAG, "attachBaseContext: Initializing ACRA");
            try {
                final ACRAConfiguration config = new ConfigurationBuilder(this)
                        .setHttpMethod(HttpSender.Method.PUT)
                        .setReportType(HttpSender.Type.JSON)
                        .setFormUri(BuildConfig.ACRA_REPORT_URI)
                        .setLogcatArguments("-t", "500", "-v", "time", "*:D")
                        .setBuildConfigClass(BuildConfig.class)
                        .build();
                ACRA.init(this, config);
            } catch (ACRAConfigurationException e) {
                Log.d(TAG, "attachBaseContext: Failed to init ACRA " + e.getLocalizedMessage());
            }
        }
        // Migrate existing preferences or remove old
        // ones before starting the actual application
        MigrateUtils.doMigrate(getBaseContext());
    }

    /**
     * Checks if the user has purchased the unlocker from the play store. If yes
     * then all extra features shall be accessible. The application is unlocked.
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

        logger.disableLogToFile();
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
        logger.log(TAG, "onBillingError() called with: errorCode = [" + errorCode + "], error = [" + error + "]");
    }

    @Override
    public void onBillingInitialized() {
        logger.log(TAG, "onBillingInitialized() called");
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        if (billingProcessor.isValidTransactionDetails(details)) {
            Snackbar.make(null, getString(R.string.unlocker_purchase_successful),
                    Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(null, getString(R.string.unlocker_purchase_not_successful), 
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPurchaseHistoryRestored() {
        logger.log(TAG, "onPurchaseHistoryRestored() called");
    }

    /**
     * Check if wifi or mobile network is available. If none of these two are
     * available show the status page otherwise continue and show the desired
     * screen.
     * 
     * @return True if the application is connected somehow with the network, otherwise false
     */
    @SuppressLint("InlinedApi")
    public boolean isConnected() {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        final boolean wifiConnected = (wifi != null) && wifi.isConnected();
        final boolean mobileConnected = (mobile != null) && mobile.isConnected();

        // Get the status of the Ethernet connection, some tablets can use an ethernet cable
        final NetworkInfo eth = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        boolean ethConnected = (eth != null) && eth.isConnected();

        return (wifiConnected || mobileConnected || ethConnected);
    }

    @Override
    public CastOptions getCastOptions(Context context) {
        String CAST_APPLICATION_ID = "0531DF56";
        CastOptions castOptions = new CastOptions.Builder()
                .setReceiverApplicationId(CAST_APPLICATION_ID)
                .build();
        return castOptions;
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
