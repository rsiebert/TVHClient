package org.tvheadend.tvhclient.features.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.PurchaseEvent;
import com.crashlytics.android.answers.SearchEvent;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class AnswersWrapper {

    private static AnswersWrapper instance;
    private static boolean isUsageStatisticsEnabled;

    public static void init(Context context) {
        Timber.d("Initializing wrapper for Answers");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        isUsageStatisticsEnabled = Fabric.isInitialized() && sharedPreferences.getBoolean("usage_statistics_enabled", false);

        if (instance == null) {
            instance = new AnswersWrapper();
        }
        Timber.d("Usage statistics are " + (isUsageStatisticsEnabled ? "enabled" : "disabled"));
    }

    public static AnswersWrapper getInstance() {
        return instance;
    }

    public void logCustom(CustomEvent customEvent) {
        if (isUsageStatisticsEnabled) {
            Answers.getInstance().logCustom(customEvent);
        }
    }

    public void logSearch(SearchEvent searchEvent) {
        if (isUsageStatisticsEnabled) {
            Answers.getInstance().logSearch(searchEvent);
        }
    }

    public void logContentView(ContentViewEvent contentViewEvent) {
        if (isUsageStatisticsEnabled) {
            Answers.getInstance().logContentView(contentViewEvent);
        }
    }

    public void logPurchase(PurchaseEvent purchaseEvent) {
        if (isUsageStatisticsEnabled) {
            Answers.getInstance().logPurchase(purchaseEvent);
        }
    }
}
