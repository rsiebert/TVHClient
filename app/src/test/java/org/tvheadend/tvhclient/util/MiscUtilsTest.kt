package org.tvheadend.tvhclient.util

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.tvhclient.R

/**
 * Tests for the small helpers in `MiscUtils.kt`.
 *
 * `convertUrlToHashString` is pure logic; `getIconUrl` and `getThemeId` need a real `Context`
 * (cache directory, resources and preferences), so Robolectric supplies them.
 */
@RunWith(RobolectricTestRunner::class)
// Use a plain Application instead of MainApplication: the latter builds the Room database and then
// runs the legacy SQLite migration in onCreate, which cannot be set up for a unit test.
@Config(sdk = [34], application = Application::class)
class MiscUtilsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun convertUrlToHashStringReturnsEmptyForAnEmptyOrNullUrl() {
        assertThat(convertUrlToHashString(null)).isEmpty()
        assertThat(convertUrlToHashString("")).isEmpty()
    }

    @Test
    fun convertUrlToHashStringComputesTheMd5OfTheUrl() {
        // MD5("http://example.com"), formatted the way the helper does (lower case hex, no
        // leading zero padding per byte).
        assertThat(convertUrlToHashString("http://example.com"))
            .isEqualTo("a9b9f04336ce181a08e774e1113b31")
    }

    @Test
    fun convertUrlToHashStringIsDeterministicAndUrlSpecific() {
        assertThat(convertUrlToHashString("http://tvheadend")).isEqualTo(convertUrlToHashString("http://tvheadend"))
        assertThat(convertUrlToHashString("http://tvheadend")).isNotEqualTo(convertUrlToHashString("http://other"))
    }

    @Test
    fun getIconUrlPointsIntoTheCacheDirectory() {
        assertThat(getIconUrl(context, "http://example.com"))
            .isEqualTo("file://" + context.cacheDir + "/a9b9f04336ce181a08e774e1113b31.png")
    }

    @Test
    fun getIconUrlHandlesANullUrl() {
        // A null url is encoded to "", whose hash is "".
        assertThat(getIconUrl(context, null)).isEqualTo("file://" + context.cacheDir + "/.png")
    }

    @Test
    fun getThemeIdReturnsTheLightThemeWhenLightIsSelected() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString("selected_theme", "light").apply()

        assertThat(getThemeId(context)).isEqualTo(R.style.CustomTheme_Light)
    }

    @Test
    fun getThemeIdReturnsTheDarkThemeWhenDarkIsSelected() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString("selected_theme", "dark").apply()

        assertThat(getThemeId(context)).isEqualTo(R.style.CustomTheme)
    }

    @Test
    fun getThemeIdDefaultsToTheLightThemeWhenNothingIsSelected() {
        // pref_default_theme is "light".
        assertThat(getThemeId(context)).isEqualTo(R.style.CustomTheme_Light)
    }

    @Test
    fun getThemeIdForAutoUsesTheLightThemeInDayMode() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString("selected_theme", "auto").apply()

        assertThat(getThemeId(context)).isEqualTo(R.style.CustomTheme_Light)
    }

    @Test
    @Config(qualifiers = "night")
    fun getThemeIdForAutoUsesTheDarkThemeInNightMode() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString("selected_theme", "auto").apply()

        assertThat(getThemeId(context)).isEqualTo(R.style.CustomTheme)
    }
}
