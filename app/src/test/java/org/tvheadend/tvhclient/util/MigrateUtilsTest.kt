package org.tvheadend.tvhclient.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.AppRepository
import org.tvheadend.tvhclient.BuildConfig

/**
 * Tests for [MigrateUtils], which upgrades preferences (and some database rows) once per app
 * version change.
 *
 * `MigrateUtils` runs its migration steps according to the `build_version_for_migration`
 * preference: every step whose threshold version is greater than the stored version is applied.
 * The tests therefore seed the stored version and then assert the effect of the resulting steps on
 * a real (Robolectric) [SharedPreferences].
 *
 * The [AppRepository] is a `mockk` stub: the preference-only steps do not need it, and the
 * database-only steps (`setSyncRequiredForAllConnections`, `convertConnectionHostAndPortValuesToUrl`,
 * `clearAllPlaybackProfiles`, `updateRecordingProfiles`, ...) iterate `connectionData.getItems()`,
 * which is stubbed to an empty list so they are harmless. The real application graph (Firebase,
 * Cast, Billing, Room database) is deliberately not built. A plain [Application] is substituted for
 * `MainApplication`, which performs that database work in `onCreate` and cannot run in a unit test.
 *
 * The oldest migration bundle (`lastInstalledApplicationVersion < 101`) is **not** covered: it
 * includes `copyConnectionsFromOldToNewDatabase`, which opens the legacy `tvhclient` SQLite database
 * through `DatabaseHelperForMigration` (version 9). That database collides with the Room database
 * (`AppRoomDatabase`, same file name, version 15), so it is not feasible to drive that path in
 * isolation. See the report.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MigrateUtilsTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("migration_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    // ---- driver ---------------------------------------------------------------------

    /** A `mockk` stub of the repository with no connections, which every DB step tolerates. */
    private fun repository(): AppRepository {
        val repository = mockk<AppRepository>(relaxed = true)
        every { repository.connectionData.getItems() } returns emptyList()
        return repository
    }

    private fun migrateFrom(version: Int) {
        prefs.edit().putInt("build_version_for_migration", version).apply()
        MigrateUtils(context, repository(), prefs).doMigrate()
    }

    // ---- version handling -----------------------------------------------------------

    @Test
    fun doesNotRunAnyStepWhenTheStoredVersionMatches() {
        prefs.edit()
            .putInt("build_version_for_migration", BuildConfig.BUILD_VERSION)
            .putString("hours_of_epg_data_per_screen", "0")
            .apply()

        MigrateUtils(context, repository(), prefs).doMigrate()

        // The EPG migration would reset a "0"; it must not run at all.
        assertThat(prefs.getString("hours_of_epg_data_per_screen", null)).isEqualTo("0")
    }

    @Test
    fun storesTheCurrentVersionAfterMigrating() {
        migrateFrom(196)

        assertThat(prefs.getInt("build_version_for_migration", -1)).isEqualTo(BuildConfig.BUILD_VERSION)
    }

    // ---- 116: channel icon action ---------------------------------------------------

    @Test
    fun convertChannelIconActionPreferenceEnablesPlaybackWhenTheOldFlagWasSet() {
        prefs.edit().putBoolean("channel_icon_starts_playback_enabled", true).apply()

        migrateFrom(109)

        assertThat(prefs.getString("channel_icon_action", null)).isEqualTo("2")
    }

    @Test
    fun convertChannelIconActionPreferenceDisablesPlaybackWhenTheOldFlagWasUnset() {
        prefs.edit().putBoolean("channel_icon_starts_playback_enabled", false).apply()

        migrateFrom(109)

        assertThat(prefs.getString("channel_icon_action", null)).isEqualTo("0")
    }

    // ---- 143/144: channel sort order ------------------------------------------------

    @Test
    fun duplicateInternalPlayerSettingForRecordingsCopiesTheValueToBothKeys() {
        prefs.edit().putBoolean("internal_player_enabled", true).apply()

        migrateFrom(120)

        assertThat(prefs.getBoolean("internal_player_for_channels_enabled", false)).isTrue()
        assertThat(prefs.getBoolean("internal_player_for_recordings_enabled", false)).isTrue()
    }

    @Test
    fun increaseChannelSortOrderPreferenceValueShiftsLargeValuesByTwo() {
        prefs.edit().putString("channel_sort_order", "3").apply()

        migrateFrom(143)

        assertThat(prefs.getString("channel_sort_order", null)).isEqualTo("5")
    }

    @Test
    fun increaseChannelSortOrderPreferenceValueLeavesSmallValuesUntouched() {
        prefs.edit().putString("channel_sort_order", "1").apply()

        migrateFrom(143)

        assertThat(prefs.getString("channel_sort_order", null)).isEqualTo("1")
    }

    @Test
    fun channelSortOrderIsDoubledForTheDescendingOrderAndThenShifted() {
        prefs.edit().putString("channel_sort_order", "3").apply()

        // Coming from 120 both the 143 step (3 * 2 + 1 = 7) and the 144 step (7 + 2 = 9) run.
        migrateFrom(120)

        assertThat(prefs.getString("channel_sort_order", null)).isEqualTo("9")
    }

    // ---- 189: hours of EPG data per screen ------------------------------------------

    @Test
    fun convertInvalidEpgPreferenceResetsZeroToOne() {
        prefs.edit().putString("hours_of_epg_data_per_screen", "0").apply()

        migrateFrom(176)

        assertThat(prefs.getString("hours_of_epg_data_per_screen", null)).isEqualTo("1")
    }

    @Test
    fun convertInvalidEpgPreferenceKeepsAValidValue() {
        prefs.edit().putString("hours_of_epg_data_per_screen", "6").apply()

        migrateFrom(176)

        assertThat(prefs.getString("hours_of_epg_data_per_screen", null)).isEqualTo("6")
    }

    // ---- 196: low storage space threshold -------------------------------------------

    @Test
    fun convertInvalidLowSpaceThresholdPreferenceResetsGarbageToTheDefault() {
        prefs.edit().putString("low_storage_space_threshold", "not-a-number").apply()

        migrateFrom(189)

        assertThat(prefs.getString("low_storage_space_threshold", null)).isEqualTo("1")
    }

    @Test
    fun convertInvalidLowSpaceThresholdPreferenceKeepsAValidValue() {
        prefs.edit().putString("low_storage_space_threshold", "10").apply()

        migrateFrom(189)

        assertThat(prefs.getString("low_storage_space_threshold", null)).isEqualTo("10")
    }

    // ---- 205: theme -----------------------------------------------------------------

    @Test
    fun convertThemePreferenceWritesTheLightThemeWhenLightWasEnabled() {
        prefs.edit().putBoolean("light_theme_enabled", true).apply()

        migrateFrom(196)

        assertThat(prefs.getString("selected_theme", null)).isEqualTo("light")
        assertThat(prefs.contains("light_theme_enabled")).isFalse()
    }

    @Test
    fun convertThemePreferenceWritesTheDarkThemeWhenLightWasDisabled() {
        prefs.edit().putBoolean("light_theme_enabled", false).apply()

        migrateFrom(196)

        assertThat(prefs.getString("selected_theme", null)).isEqualTo("dark")
    }
}
