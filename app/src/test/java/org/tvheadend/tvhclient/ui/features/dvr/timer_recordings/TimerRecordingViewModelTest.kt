package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.testutil.observedValue
import java.util.Calendar

/**
 * Tests for [TimerRecordingViewModel]: loading timer recordings, the id-driven
 * [TimerRecordingViewModel.recordingLiveData], the millisecond/minute conversion of the start and
 * stop times and the intent that is handed to the connection service.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TimerRecordingViewModelTest {

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

    private fun seedActiveConnection(): Int {
        graph.appRepository.connectionData.addItem(Connection(name = "Active", isActive = true))
        return graph.appRepository.connectionData.activeItem.id
    }

    // ---- recordings -----------------------------------------------------------------

    @Test
    fun recordingsAreLoadedFromTheActiveConnection() {
        val connectionId = seedActiveConnection()
        graph.appRepository.timerRecordingData.addItem(
            TimerRecording(id = "t1", connectionId = connectionId, title = "Daily"))

        val vm = TimerRecordingViewModel(context)

        assertThat(vm.recordings.observedValue()!!.map { it.title }).containsExactly("Daily")
    }

    @Test
    fun recordingLiveDataLoadsTheSelectedRecording() {
        val connectionId = seedActiveConnection()
        graph.appRepository.timerRecordingData.addItem(
            TimerRecording(id = "t1", connectionId = connectionId, title = "Daily"))
        val vm = TimerRecordingViewModel(context)
        vm.recordingLiveData.observeForever { }

        vm.currentIdLiveData.value = "t1"

        assertThat(vm.recordingLiveData.value!!.title).isEqualTo("Daily")
    }

    @Test
    fun loadRecordingByIdSyncLoadsTheRecordingAndEnablesTime() {
        val connectionId = seedActiveConnection()
        graph.appRepository.timerRecordingData.addItem(
            TimerRecording(id = "t1", connectionId = connectionId, title = "Daily", start = 600, stop = 900))
        val vm = TimerRecordingViewModel(context)

        vm.loadRecordingByIdSync("t1")

        assertThat(vm.recording.title).isEqualTo("Daily")
        assertThat(vm.isTimeEnabled).isTrue()
    }

    @Test
    fun loadRecordingByIdSyncWithAnUnknownIdResetsToAnEmptyRecording() {
        seedActiveConnection()
        val vm = TimerRecordingViewModel(context)

        vm.loadRecordingByIdSync("nope")

        assertThat(vm.recording.id).isEqualTo("")
        assertThat(vm.isTimeEnabled).isFalse()
    }

    // ---- time conversion ------------------------------------------------------------

    @Test
    fun settingTheStartTimeStoresTheMinutesFromMidnight() {
        seedActiveConnection()
        val vm = TimerRecordingViewModel(context)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.SECOND, 0)

        vm.startTimeInMillis = calendar.timeInMillis

        assertThat(vm.recording.start).isEqualTo((10 * 60 + 30).toLong())
    }

    @Test
    fun disablingTheTimeResetsStartAndStopToTheCurrentMinute() {
        seedActiveConnection()
        val vm = TimerRecordingViewModel(context)
        vm.recording.start = 100
        vm.recording.stop = 200

        vm.isTimeEnabled = false

        // Both are recomputed from "now", so they share the same minute-of-day value. (Their
        // millisecond getters are intentionally not compared: `TimerRecording.startTimeInMillis`
        // leaves the Calendar millisecond field untouched, so the two conversions can differ by a
        // millisecond.)
        assertThat(vm.recording.start).isEqualTo(vm.recording.stop)
    }

    // ---- intent ---------------------------------------------------------------------

    @Test
    fun getIntentDataZeroesTheTimesWhenTimeIsDisabled() {
        seedActiveConnection()
        val vm = TimerRecordingViewModel(context)
        vm.isTimeEnabled = false
        val recording = TimerRecording(
            id = "t1", title = "Daily", name = "DailyName", directory = "/rec", start = 600, stop = 900, isEnabled = true)

        val intent = vm.getIntentData(context, recording)

        assertThat(intent.getStringExtra("title")).isEqualTo("Daily")
        assertThat(intent.getStringExtra("name")).isEqualTo("DailyName")
        assertThat(intent.getStringExtra("directory")).isEqualTo("/rec")
        assertThat(intent.getLongExtra("start", -1)).isEqualTo(0L)
        assertThat(intent.getLongExtra("stop", -1)).isEqualTo(0L)
        assertThat(intent.getIntExtra("enabled", -1)).isEqualTo(1)
        assertThat(intent.hasExtra("channelId")).isFalse()
    }

    @Test
    fun getIntentDataKeepsTheTimesAndChannelWhenTimeIsEnabled() {
        seedActiveConnection()
        val vm = TimerRecordingViewModel(context)
        vm.isTimeEnabled = true
        val recording = TimerRecording(id = "t1", start = 600, stop = 900, channelId = 3)

        val intent = vm.getIntentData(context, recording)

        assertThat(intent.getLongExtra("start", -1)).isEqualTo(600L)
        assertThat(intent.getLongExtra("stop", -1)).isEqualTo(900L)
        assertThat(intent.getIntExtra("channelId", -1)).isEqualTo(3)
    }

    // ---- channels and profiles ------------------------------------------------------

    @Test
    fun getChannelListFollowsTheSortOrderPreference() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId, serverOrder = 2))
        graph.appRepository.channelData.addItem(Channel(id = 2, connectionId = connectionId, serverOrder = 1))
        prefs().edit().putString("channel_sort_order", "0").commit()
        val vm = TimerRecordingViewModel(context)

        assertThat(vm.getChannelList().map { it.id }).isEqualTo(listOf(2, 1))
    }

    @Test
    fun getRecordingProfileResolvesTheTimerRecordingProfile() {
        val connectionId = seedActiveConnection()
        graph.appRepository.serverProfileData.addItem(
            ServerProfile(id = 6, connectionId = connectionId, name = "Rec", type = ServerProfile.RECORDING_PROFILE))
        val status = graph.appRepository.serverStatusData.activeItem
        status.timerRecordingServerProfileId = 6
        graph.appRepository.serverStatusData.updateItem(status)
        val vm = TimerRecordingViewModel(context)

        assertThat(vm.getRecordingProfile()!!.name).isEqualTo("Rec")
        assertThat(vm.getRecordingProfileNames().asList()).containsExactly("Rec")
    }
}
