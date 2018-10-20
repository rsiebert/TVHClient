package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import timber.log.Timber;

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
    public static String convertUrlToHashString(@NonNull final String url) {
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
    public static int getThemeId(@NonNull final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean theme = prefs.getBoolean("light_theme_enabled", true);
        return (theme ? R.style.CustomTheme_Light : R.style.CustomTheme);
    }


    /**
     * Change the language to the defined setting. If the default is set then
     * let the application decide which language shall be used.
     *
     * @param context Context context
     */
    public static void setLanguage(@NonNull final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String locale = prefs.getString("language", "default");
        if (!locale.equals("default")) {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.locale = new Locale(locale);
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
    }

    public static boolean isServerProfileEnabled(ServerProfile serverProfile, @NonNull ServerStatus serverStatus) {
        return serverProfile != null && serverStatus.getHtspVersion() >= 16;
    }

    public static int getDaylightSavingOffset() {
        // Current timezone and date
        TimeZone timeZone = TimeZone.getDefault();
        Date nowDate = new Date();
        int offsetFromUtc = timeZone.getOffset(nowDate.getTime());
        Timber.d("Offset from UTC is " + offsetFromUtc);

        if (timeZone.useDaylightTime()) {
            Timber.d("Daylight saving is used");
            int dstOffset = timeZone.getDSTSavings();
            if (timeZone.inDaylightTime(nowDate)) {
                Timber.d("Daylight saving offset is " + dstOffset);
                return dstOffset;
            }
        }
        Timber.d("Daylight saving is not used");
        return 0;
    }

    public static CastSession getCastSession(Context context) {
        CastContext castContext = getCastContext(context);
        if (castContext != null) {
            try {
                return castContext.getSessionManager().getCurrentCastSession();
            } catch (IllegalStateException e) {
                Timber.e("Could not get current cast session");
            }
        }
        return null;
    }

    public static CastContext getCastContext(Context context) {
        int playServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (playServicesAvailable == ConnectionResult.SUCCESS) {
            try {
                return CastContext.getSharedInstance(context);
            } catch (IllegalStateException e) {
                Timber.e("Could not get cast context");
            }
        }
        return null;
    }
}
