package org.tvheadend.tvhclient.ui.features.epg

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.Program
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.testutil.observedValue

/**
 * Tests for [EpgViewModel].
 *
 * The view-pager / time-window / pixels-per-minute bookkeeping is pure logic driven by the
 * `days_of_epg_data` and `hours_of_epg_data_per_screen` preferences, so it is covered here. The
 * `onClick`/`onLongClick` callbacks are *not*: they reach into a `View`'s `AppCompatActivity` and
 * its `FragmentManager`, which is the fragment's job, not the ViewModel's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EpgViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Application
    private lateinit var db: AppRoomDatabase
    private lateinit var graph: TestAppGraph

    @Before
    fun setUp() {
        TestAppGraphFactory.plantNoopTimber()
        context = ApplicationProvider.getApplicationContext()
        db = TestAppGraphFactory.database(context)
        graph = TestAppGraphFactory.install(db)
    }

    @After
    fun tearDown() {
        AppGraphProvider.graph = null
        db.close()
    }

    private fun prefs() = PreferenceManager.getDefaultSharedPreferences(context)

    private fun seedActiveConnection(): Int = runBlocking {
        graph.appRepository.connectionData.addItem(Connection(name = "Active", isActive = true))
        graph.appRepository.connectionData.getActiveItem().id
    }

    // ---- view pager bookkeeping -----------------------------------------------------

    @Test
    fun viewPagerFragmentCountFollowsThePreferences() {
        // Defaults: days_of_epg_data = 7, hours_of_epg_data_per_screen = 4.
        val vm = EpgViewModel(context)

        assertThat(vm.viewPagerFragmentCount.value).isEqualTo(7 * (24 / 4))
    }

    @Test
    fun startAndEndTimesCoverEveryViewPagerPage() {
        val vm = EpgViewModel(context)
        val pageLength = 4L * 60 * 60 * 1000
        val count = vm.viewPagerFragmentCount.value!!

        assertThat(vm.getEndTime(0) - vm.getStartTime(0)).isEqualTo(pageLength - 1)
        assertThat(vm.getStartTime(1) - vm.getStartTime(0)).isEqualTo(pageLength)
        assertThat(vm.getStartTime(count - 1)).isGreaterThan(vm.getStartTime(0))
    }

    @Test
    fun pixelsPerMinuteDependsOnTheDisplayWidthAndHoursToShow() {
        val vm = EpgViewModel(context)

        vm.displayWidth = 221 + 600

        assertThat(vm.pixelsPerMinute).isWithin(1e-4f).of(2.5f)
    }

    @Test
    fun hoursAndDaysToShowNeverReportZero() {
        val vm = EpgViewModel(context)

        vm.hoursToShow = 0
        vm.daysToShow = 0

        assertThat(vm.hoursToShow).isEqualTo(1)
        assertThat(vm.daysToShow).isEqualTo(1)
    }

    @Test
    fun changingTheHoursPreferenceRebuildsTheViewProperties() {
        val vm = EpgViewModel(context)
        vm.viewAndEpgDataIsInvalid.observeForever { }
        prefs().edit().putString("hours_of_epg_data_per_screen", "2").commit()

        vm.onSharedPreferenceChanged(prefs(), "hours_of_epg_data_per_screen")

        assertThat(vm.hoursToShow).isEqualTo(2)
        assertThat(vm.viewPagerFragmentCount.value).isEqualTo(7 * (24 / 2))
    }

    @Test
    fun showFlagsFollowThePreferences() {
        prefs().edit()
            .putBoolean("channel_number_enabled", true)
            .putBoolean("program_subtitle_enabled", true)
            .putBoolean("genre_colors_for_program_guide_enabled", true)
            .commit()

        val vm = EpgViewModel(context)

        assertThat(vm.showChannelNumber.value).isTrue()
        assertThat(vm.showProgramSubtitle.value).isTrue()
        assertThat(vm.showGenreColor.value).isTrue()
    }

    // ---- channels and programs ------------------------------------------------------

    @Test
    fun epgChannelsAreLoadedFromTheActiveConnection() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId, name = "One"))
        graph.appRepository.channelData.addItem(Channel(id = 2, connectionId = connectionId, name = "Two"))

        val vm = EpgViewModel(context)

        assertThat(vm.epgChannels.observedValue()!!.map { it.id }).containsExactly(1, 2)
    }

    @Test
    fun getProgramsByChannelAndBetweenTimeSyncReturnsProgramsInThePageWindow(): Unit = runBlocking {
        val connectionId = seedActiveConnection()
        val vm = EpgViewModel(context)
        val start = vm.getStartTime(0)
        graph.appRepository.programData.addItem(
            Program(eventId = 1, connectionId = connectionId, channelId = 3, start = start + 1000, stop = start + 2000, title = "InWindow"))

        assertThat(vm.getProgramsByChannelAndBetweenTimeSync(3, 0).map { it.title }).containsExactly("InWindow")
    }
}
