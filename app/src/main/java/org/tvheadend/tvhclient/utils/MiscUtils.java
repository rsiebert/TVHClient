package org.tvheadend.tvhclient.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

// TODO Split into UI and others

public class MiscUtils {

    private MiscUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Converts the given url into a unique hash value.
     *
     * @param url The url that shall be converted
     * @return The hash value or the url or an empty string if an error occurred
     */
    public static String convertUrlToHashString(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            byte messageDigest[] = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte md : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & md));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // NOP
        }
        return null;
    }

    /**
     * Returns the id of the theme that is currently set in the settings.
     *
     * @param context Context
     * @return Id of the light or dark theme
     */
    public static int getThemeId(final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean theme = prefs.getBoolean("light_theme_enabled", true);
        return (theme ? R.style.CustomTheme_Light : R.style.CustomTheme);
    }


    /**
     * Change the language to the defined setting. If the default is set then
     * let the application decide which language shall be used.
     *
     * @param context Activity context
     */
    public static void setLanguage(final Activity context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String locale = prefs.getString("language", "default");
        if (!locale.equals("default")) {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.locale = new Locale(locale);
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
    }

    public static boolean isServerProfileEnabled(ServerProfile serverProfile, ServerStatus serverStatus) {
        return serverProfile != null
                //&& serverProfile.isEnabled()
                && serverStatus.getHtspVersion() >= 16
                && MainApplication.getInstance().isUnlocked();
    }
}
