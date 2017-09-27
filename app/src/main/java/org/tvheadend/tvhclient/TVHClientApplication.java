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
import android.util.SparseArray;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.acra.ACRA;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;
import org.tvheadend.tvhclient.data.DataContentProviderHelper;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.ChannelTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TVHClientApplication extends Application implements BillingProcessor.IBillingHandler {
    private final static String TAG = TVHClientApplication.class.getSimpleName();

    private final List<HTSListener> listeners = new ArrayList<>();
    // This handles all billing related activities like purchasing and checking
    // if a purchase was made 
    private BillingProcessor bp;


    private Logger logger = null;
    private DataContentProviderHelper mDataContentProviderHelper = null;
    private static TVHClientApplication mInstance;

    public static synchronized TVHClientApplication getInstance() {
        return mInstance;
    }

    public synchronized DataContentProviderHelper getContentProviderHelper() {
        if (mDataContentProviderHelper == null) {
            mDataContentProviderHelper = new DataContentProviderHelper(getApplicationContext());
        }
        return mDataContentProviderHelper;
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

    public static SparseArray<String> getContentTypes(Context ctx) {
        SparseArray<String> ret = new SparseArray<>();

        String[] s = ctx.getResources().getStringArray(R.array.pr_content_type0);
        for (int i = 0; i < s.length; i++) {
            ret.append(i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type1);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x10 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type2);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x20 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type3);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x30 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type4);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x40 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type5);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x50 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type6);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x60 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type7);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x70 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type8);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x80 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type9);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x90 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type10);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xa0 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type11);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xb0 + i, s[i]);
        }

        return ret;
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

        mInstance = this;
        logger = Logger.getInstance();
        DataStorage ds = DataStorage.getInstance();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("pref_debug_mode", false)) {
            logger.enableLogToFile();
        }

        bp = new BillingProcessor(this, Utils.getPublicKey(this), this);
        if (!BillingProcessor.isIabServiceAvailable(this)) {
            logger.log(TAG, "onCreate: billing not available");
        } else {
            if (!bp.loadOwnedPurchasesFromGoogle()) {
                logger.log(TAG, "onCreate: Could not load purchase information");
            }
        }

        // Add the default tag (all channels) to the list
        ChannelTag tag = new ChannelTag();
        tag.id = 0;
        tag.name = getString(R.string.all_channels);
        ds.addChannelTag(tag);

        // Build a CastConfiguration object and initialize VideoCastManager
        CastConfiguration options = new CastConfiguration.Builder(Constants.CAST_APPLICATION_ID)
                .enableAutoReconnect()
                .enableCaptionManagement()
                .enableDebug()
                .enableLockScreen()
                .enableNotification()
                .enableWifiReconnection()
                .setCastControllerImmersive(true)
                .setLaunchOptions(false, Locale.getDefault())
                .setNextPrevVisibilityPolicy(CastConfiguration.NEXT_PREV_VISIBILITY_POLICY_HIDDEN)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_REWIND, false)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_PLAY_PAUSE, true)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_DISCONNECT, true)
                .setForwardStep(10)
                .build();
        VideoCastManager.initialize(this, options);
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
                Log.e(TAG, "attachBaseContext: Failed to init ACRA " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Checks if the user has purchased the unlocker from the play store. If yes
     * then all extra features shall be accessible. The application is unlocked.
     * 
     * @return True if the application is unlocked otherwise false
     */
    public boolean isUnlocked() {
        return BuildConfig.DEBUG_MODE || bp.isPurchased(Constants.UNLOCKER);
    }

    @Override
    public void onTerminate() {
        if (bp != null) {
            bp.release();
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
        return bp;
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
        if (bp.isValidTransactionDetails(details)) {
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

}
