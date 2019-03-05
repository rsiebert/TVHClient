package org.tvheadend.tvhclient.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.ServerProfile;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.TimeZone;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
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

    public static String getIconUrl(Context context, @Nullable final String url) {
        if (url == null) {
            return null;
        }
        return "file://" + context.getCacheDir() + "/" + convertUrlToHashString(url) + ".png";
    }

    /**
     * Returns the id of the theme that is currently set in the settings.
     *
     * @param context Context
     * @return Id of the light or dark theme
     */
    public static int getThemeId(@NonNull final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean theme = prefs.getBoolean("light_theme_enabled", true);
        return (theme ? R.style.CustomTheme_Light : R.style.CustomTheme);
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

    public static CastContext getCastContext(@NonNull Context context) {
        int playServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (playServicesAvailable == ConnectionResult.SUCCESS) {
            try {
                return CastContext.getSharedInstance(context);
            } catch (RuntimeException e) {
                Timber.e("Could not get cast context", e);
            }
        }
        return null;
    }
}
