package org.tvheadend.tvhclient.ui.features.dvr

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import java.util.TimeZone

/**
 * Tests for the pure helper functions in `RecordingUtils.kt`. The dialog/fragment builders in the
 * same file need a live activity and are not exercised here.
 *
 * Robolectric supplies the `Context`/`Resources` for the priority and day name lookups. The two
 * date/time formatters use `SimpleDateFormat` with the JVM default time zone, which is pinned to
 * UTC so the expected strings are stable.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application is used instead of MainApplication, which does database work in onCreate.
@Config(sdk = [34], application = Application::class)
class RecordingUtilsTest {

    private lateinit var context: Context
    private lateinit var originalTimeZone: TimeZone

    // 2023-11-14T22:13:20Z
    private val referenceTimeMillis = 1_700_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun getPriorityNameMapsTheKnownPrioritiesToTheResourceNames() {
        val names = context.resources.getStringArray(R.array.dvr_priority_names)

        for (priority in 0..4) {
            assertThat(getPriorityName(context, priority)).isEqualTo(names[priority])
        }
        // Priority 6 ("not set") maps to the last entry.
        assertThat(getPriorityName(context, 6)).isEqualTo(names[5])
    }

    @Test
    fun getPriorityNameReturnsEmptyForAnUnknownPriority() {
        assertThat(getPriorityName(context, 5)).isEmpty()
        assertThat(getPriorityName(context, 7)).isEmpty()
        assertThat(getPriorityName(context, -1)).isEmpty()
    }

    @Test
    fun getDateStringFromTimeInMillisFormatsDayAndMonth() {
        assertThat(getDateStringFromTimeInMillis(referenceTimeMillis)).isEqualTo("14.11")
    }

    @Test
    fun getTimeStringFromTimeInMillisFormatsHoursAndMinutes() {
        assertThat(getTimeStringFromTimeInMillis(referenceTimeMillis)).isEqualTo("22:13")
    }

    @Test
    fun getSelectedDaysOfWeekTextJoinsTheSelectedDays() {
        val names = context.resources.getStringArray(R.array.day_short_names)

        // Bit 0 (Monday) and bit 2 (Wednesday).
        val text = getSelectedDaysOfWeekText(context, 0b101)

        assertThat(text).isEqualTo("${names[0]}, ${names[2]}")
    }

    @Test
    fun getSelectedDaysOfWeekTextIsEmptyWhenNoDayIsSelected() {
        assertThat(getSelectedDaysOfWeekText(context, 0)).isEmpty()
    }

    @Test
    fun getSelectedProfileIdFindsTheMatchingProfileByName() {
        val profile = ServerProfile(name = "matroska")

        assertThat(getSelectedProfileId(profile, arrayOf("htsp", "matroska", "webm"))).isEqualTo(1)
    }

    @Test
    fun getSelectedProfileIdFallsBackToZero() {
        assertThat(getSelectedProfileId(null, arrayOf("htsp", "webm"))).isEqualTo(0)
        assertThat(getSelectedProfileId(ServerProfile(name = "missing"), arrayOf("htsp", "webm"))).isEqualTo(0)
    }
}
