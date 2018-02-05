package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.tvheadend.tvhclient.BuildConfig;

public class MigrateUtils {
    private static final String TAG = MigrateUtils.class.getSimpleName();
    private static final int VERSION_101 = 101;

    private MigrateUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static void doMigrate(Context context) {
        // Lookup the current version and the last migrated version
        int currentApplicationVersion = BuildConfig.BUILD_VERSION;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int lastInstalledApplicationVersion = sharedPreferences.getInt("build_version_for_migration", 0);
        Log.i(TAG, "Migrating from " + lastInstalledApplicationVersion + " to " + currentApplicationVersion);

        if (currentApplicationVersion != lastInstalledApplicationVersion) {
            if (lastInstalledApplicationVersion < VERSION_101) {
                migrateConvertStartScreenPreference(context);
            }
        }

        // Store the current version as the last installed version
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("build_version_for_migration", currentApplicationVersion);
        editor.apply();
    }

    private static void migrateConvertStartScreenPreference(Context context) {
        // migrate preferences from old names to new
        // names to have a consistent naming scheme afterwards
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            int value = Integer.valueOf(sharedPreferences.getString("defaultMenuPositionPref", "0"));
            if (value > 8) {
                // If the value is anything above the status screen
                // menu entry, default to the channel screen
                value = 0;
            } else if (value == 7) {
                // The program guide screen moved from position 7 to 1
                value = 1;
            } else if (value != 0) {
                // If any screens except the channel and program guide
                // were set increase the value by one because the
                // program guide moved to position 2
                value++;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("defaultMenuPositionPref", String.valueOf(value));
            editor.apply();
        } catch (NumberFormatException e) {
            // NOP
        }
    }
}
