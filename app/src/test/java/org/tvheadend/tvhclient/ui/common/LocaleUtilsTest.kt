package org.tvheadend.tvhclient.ui.common

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Tests for `LocaleUtils.kt`, which applies the user selected app language.
 *
 * `onAttach` persists the language and returns a localized context, `getLocale` reads the locale
 * back out of a context. Both need a real `Context`/`Resources` and `SharedPreferences`, so
 * Robolectric is used. A plain [Application] is substituted for `MainApplication` because the
 * latter performs database work in `onCreate` that a unit test cannot set up.
 *
 * The helpers mutate the JVM wide default locale (`Locale.setDefault`), so it is saved and
 * restored around every test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocaleUtilsTest {

    private lateinit var context: Context
    private lateinit var originalDefaultLocale: Locale

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        originalDefaultLocale = Locale.getDefault()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalDefaultLocale)
    }

    @Test
    fun onAttachUsesThePersistedLanguage() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString("language", "fr").commit()

        val localized = onAttach(context)

        assertThat(getLocale(localized).language).isEqualTo("fr")
    }

    @Test
    fun onAttachFallsBackToTheCurrentDefaultLanguage() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("language").commit()
        Locale.setDefault(Locale.GERMAN)

        val localized = onAttach(context)

        assertThat(getLocale(localized).language).isEqualTo("de")
    }

    @Test
    fun onAttachPersistsTheSelectedLanguage() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("language").commit()
        Locale.setDefault(Locale.ENGLISH)

        onAttach(context)

        val persisted = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("language", null)
        assertThat(persisted).isEqualTo("en")
    }

    @Test
    @Config(qualifiers = "de")
    fun getLocaleReadsTheLocaleFromTheConfiguration() {
        assertThat(getLocale(context).language).isEqualTo("de")
    }
}
