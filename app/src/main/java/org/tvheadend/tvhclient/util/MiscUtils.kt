package org.tvheadend.tvhclient.util

import android.content.Context
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import timber.log.Timber
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

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
        Timber.e(e, "No algorithm was found to handle MD5")
    }
    return ""
}

fun getIconUrl(context: Context, url: String?): String {
    // Replace all occurrences of + with the utf-8 value
    val urlEncoded = url?.replace("\\+", "%2b") ?: ""
    val iconUrl = "file://" + context.cacheDir + "/" + convertUrlToHashString(urlEncoded) + ".png"
    Timber.d("Loaded icon url '$iconUrl' from original url '$url', encoded url is '$urlEncoded'")
    return iconUrl
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

fun isServerProfileEnabled(serverProfile: ServerProfile?, htspVersion: Int): Boolean {
    return serverProfile != null && htspVersion >= 16
}
