package org.tvheadend.tvhclient.util

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.R
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

/**
 * Returns the url to the file of the channel icon.
 *
 * @param context Context
 * @param url The url to the icon
 * @return A string of the url to the local icon file
 */
fun getIconUrl(context: Context, url: String?): String {
    // Replace all occurrences of + with the utf-8 value
    val urlEncoded = url?.replace("\\+", "%2b") ?: ""
    return "file://" + context.cacheDir + "/" + convertUrlToHashString(urlEncoded) + ".png"
}

/**
 * Returns the id of the theme that is currently set in the settings.
 *
 * @param context Context
 * @return Id of the light or dark theme
 */
fun getThemeId(context: Context): Int {
    return when (PreferenceManager.getDefaultSharedPreferences(context).getString("selected_theme", context.resources.getString(R.string.pref_default_theme))) {
        "light" -> {
            Timber.d("Theme is set to light, returning light theme")
            R.style.CustomTheme_Light
        }
        "dark" -> {
            Timber.d("Theme is set to dark, returning dark theme")
            R.style.CustomTheme
        }
        "auto" -> {
            Timber.d("Theme is set to auto")
            return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    Timber.d("Night mode is not active, we're in day time, return the light theme")
                    R.style.CustomTheme_Light
                }
                Configuration.UI_MODE_NIGHT_YES -> {
                    Timber.d("Night mode is active, we're at night, return the dark theme")
                    R.style.CustomTheme
                }
                else -> {
                    Timber.d("Night mode is undefined, return the light theme")
                    R.style.CustomTheme_Light
                }
            }
        }
        else -> R.style.CustomTheme_Light
    }
}
