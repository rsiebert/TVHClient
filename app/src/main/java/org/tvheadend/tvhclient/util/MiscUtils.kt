@file:JvmName("MiscUtils")

package org.tvheadend.tvhclient.util

import android.content.Context
import android.preference.PreferenceManager
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


/**
 * Converts the given url into a unique hash value.
 *
 * @param url The url that shall be converted
 * @return The hash value or the url or an empty string if an error occurred
 */
fun convertUrlToHashString(url: String?): String {
    if (url.isNullOrEmpty()) return ""
    try {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(url.toByteArray())
        val messageDigest = digest.digest()
        val hexString = StringBuilder()
        for (md in messageDigest) {
            hexString.append(Integer.toHexString(0xFF and md.toInt()))
        }
        return hexString.toString()
    } catch (e: NoSuchAlgorithmException) {
        // NOP
    }
    return ""
}

fun getIconUrl(context: Context, url: String?): String {
    return "file://" + context.cacheDir + "/" + convertUrlToHashString(url) + ".png"
}

/**
 * Returns the id of the theme that is currently set in the settings.
 *
 * @param context Context
 * @return Id of the light or dark theme
 */
fun getThemeId(context: Context): Int {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val theme = prefs.getBoolean("light_theme_enabled", true)
    return if (theme) R.style.CustomTheme_Light else R.style.CustomTheme
}

fun isServerProfileEnabled(serverProfile: ServerProfile?, serverStatus: ServerStatus): Boolean {
    return serverProfile != null && serverStatus.htspVersion >= 16
}

// Current timezone and date
val daylightSavingOffset: Int
    get() {
        val timeZone = TimeZone.getDefault()
        val nowDate = Date()
        val offsetFromUtc = timeZone.getOffset(nowDate.time)
        Timber.d("Offset from UTC is $offsetFromUtc")

        if (timeZone.useDaylightTime()) {
            Timber.d("Daylight saving is used")
            val dstOffset = timeZone.dstSavings
            if (timeZone.inDaylightTime(nowDate)) {
                Timber.d("Daylight saving offset is $dstOffset")
                return dstOffset
            }
        }
        Timber.d("Daylight saving is not used")
        return 0
    }

fun getCastSession(context: Context): CastSession? {
    val castContext = getCastContext(context)
    if (castContext != null) {
        try {
            return castContext.sessionManager.currentCastSession
        } catch (e: IllegalStateException) {
            Timber.e("Could not get current cast session")
        }

    }
    return null
}

fun getCastContext(context: Context): CastContext? {
    val playServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    if (playServicesAvailable == ConnectionResult.SUCCESS) {
        try {
            return CastContext.getSharedInstance(context)
        } catch (e: RuntimeException) {
            Timber.e("Could not get cast context", e)
        }

    }
    return null
}
