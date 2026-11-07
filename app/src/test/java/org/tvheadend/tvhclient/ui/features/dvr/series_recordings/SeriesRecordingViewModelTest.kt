package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

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
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.testutil.observedValue

/**
 * Tests for [SeriesRecordingViewModel]: loading series (auto) recordings, the id-driven
 * [SeriesRecordingViewModel.recordingLiveData], the start-window handling and the intent that is
 * handed to the connection service.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SeriesRecordingViewModelTest {

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
        graph.appRepository.seriesRecordingData.addItem(
            SeriesRecording(id = "s1", connectionId = connectionId, title = "Series"))

        val vm = SeriesRecordingViewModel(context)

        assertThat(vm.recordings.observedValue()!!.map { it.title }).containsExactly("Series")
    }

    @Test
    fun recordingLiveDataLoadsTheSelectedRecording() {
        val connectionId = seedActiveConnection()
        graph.appRepository.seriesRecordingData.addItem(
            SeriesRecording(id = "s1", connectionId = connectionId, title = "Series"))
        val vm = SeriesRecordingViewModel(context)
        vm.recordingLiveData.observeForever { }

        vm.currentIdLiveData.value = "s1"

        assertThat(vm.recordingLiveData.value!!.title).isEqualTo("Series")
    }

    @Test
    fun loadRecordingByIdSyncLoadsTheRecordingAndEnablesTime() {
        val connectionId = seedActiveConnection()
        graph.appRepository.seriesRecordingData.addItem(
            SeriesRecording(id = "s1", connectionId = connectionId, title = "Series", start = 100, startWindow = 200))
        val vm = SeriesRecordingViewModel(context)

        vm.loadRecordingByIdSync("s1")

        assertThat(vm.recording.title).isEqualTo("Series")
        assertThat(vm.isTimeEnabled).isTrue()
    }

    @Test
    fun loadRecordingByIdSyncDisablesTimeForANegativeStart() {
        val connectionId = seedActiveConnection()
        graph.appRepository.seriesRecordingData.addItem(
            SeriesRecording(id = "s1", connectionId = connectionId, start = -1, startWindow = 0))
        val vm = SeriesRecordingViewModel(context)

        vm.loadRecordingByIdSync("s1")

        assertThat(vm.isTimeEnabled).isFalse()
    }

    // ---- duplicate detection --------------------------------------------------------

    @Test
    fun duplicateDetectionListComesFromTheResources() {
        seedActiveConnection()
        val vm = SeriesRecordingViewModel(context)

        assertThat(vm.duplicateDetectionList).isNotEmpty()
    }

    // ---- intent ---------------------------------------------------------------------

    @Test
    fun getIntentDataZeroesTheWindowWhenTimeIsDisabled() {
        seedActiveConnection()
        val vm = SeriesRecordingViewModel(context)
        vm.isTimeEnabled = false
        val recording = SeriesRecording(
            id = "s1", title = "Series", name = "N", directory = "/rec", minDuration = 5, maxDuration = 10,
            start = 0, startWindow = 0, startExtra = 1, stopExtra = 2, dupDetect = 3, isEnabled = true)

        val intent = vm.getIntentData(context, recording)

        assertThat(intent.getStringExtra("title")).isEqualTo("Series")
        assertThat(intent.getLongExtra("start", 123)).isEqualTo(-1L)
        assertThat(intent.getLongExtra("startWindow", 123)).isEqualTo(-1L)
        assertThat(intent.getIntExtra("minDuration", -1)).isEqualTo(5 * 60)
        assertThat(intent.getIntExtra("maxDuration", -1)).isEqualTo(10 * 60)
        assertThat(intent.getIntExtra("dupDetect", -1)).isEqualTo(3)
        assertThat(intent.getIntExtra("enabled", -1)).isEqualTo(1)
        assertThat(intent.hasExtra("channelId")).isFalse()
    }

    @Test
    fun getIntentDataKeepsTheWindowAndChannelWhenTimeIsEnabled() {
        seedActiveConnection()
        val vm = SeriesRecordingViewModel(context)
        vm.isTimeEnabled = true
        val recording = SeriesRecording(id = "s1", start = 100, startWindow = 200, channelId = 3)

        val intent = vm.getIntentData(context, recording)

        assertThat(intent.getLongExtra("start", 123)).isEqualTo(100L)
        assertThat(intent.getLongExtra("startWindow", 123)).isEqualTo(200L)
        assertThat(intent.getIntExtra("channelId", -1)).isEqualTo(3)
    }

    // ---- channels and profiles ------------------------------------------------------

    @Test
    fun getChannelListFollowsTheSortOrderPreference() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId, serverOrder = 2))
        graph.appRepository.channelData.addItem(Channel(id = 2, connectionId = connectionId, serverOrder = 1))
        prefs().edit().putString("channel_sort_order", "0").commit()
        val vm = SeriesRecordingViewModel(context)

        assertThat(vm.getChannelList().map { it.id }).isEqualTo(listOf(2, 1))
    }

    @Test
    fun getRecordingProfileResolvesTheSeriesRecordingProfile() {
        val connectionId = seedActiveConnection()
        graph.appRepository.serverProfileData.addItem(
            ServerProfile(id = 6, connectionId = connectionId, name = "Rec", type = ServerProfile.RECORDING_PROFILE))
        val status = graph.appRepository.serverStatusData.activeItem
        status.seriesRecordingServerProfileId = 6
        graph.appRepository.serverStatusData.updateItem(status)
        val vm = SeriesRecordingViewModel(context)

        assertThat(vm.getRecordingProfile()!!.name).isEqualTo("Rec")
        assertThat(vm.getRecordingProfileNames().asList()).containsExactly("Rec")
    }
}
