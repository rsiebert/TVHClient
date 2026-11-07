package org.tvheadend.tvhclient.ui.features.dvr.recordings

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
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.testutil.observedValue

/**
 * Tests for [RecordingViewModel]: the per-state recording lists, the id-driven
 * [RecordingViewModel.recordingLiveData], the genre-colour preference and the intent that is handed
 * to the connection service.
 *
 * Recordings are classified by their `state`/`error` columns, so the tests seed one row per state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RecordingViewModelTest {

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

    // ---- lists ----------------------------------------------------------------------

    @Test
    fun exposesTheRecordingListsByState() {
        val connectionId = seedActiveConnection()
        graph.appRepository.recordingData.addItem(Recording(id = 1, connectionId = connectionId, state = "completed"))
        graph.appRepository.recordingData.addItem(Recording(id = 2, connectionId = connectionId, state = "scheduled"))
        graph.appRepository.recordingData.addItem(Recording(id = 3, connectionId = connectionId, state = "recording"))
        graph.appRepository.recordingData.addItem(Recording(id = 4, connectionId = connectionId, state = "missed", error = null))

        val vm = RecordingViewModel(context)

        assertThat(vm.completedRecordings.observedValue()!!.map { it.id }).containsExactly(1)
        assertThat(vm.scheduledRecordings.observedValue()!!.map { it.id }).containsExactly(2, 3)
        assertThat(vm.failedRecordings.observedValue()!!.map { it.id }).containsExactly(4)
        assertThat(vm.removedRecordings.observedValue()).isEmpty()
    }

    @Test
    fun recordingLiveDataLoadsTheRecordingWhenTheIdChanges() {
        val connectionId = seedActiveConnection()
        graph.appRepository.recordingData.addItem(Recording(id = 7, connectionId = connectionId, title = "Seven"))
        val vm = RecordingViewModel(context)
        vm.recordingLiveData.observeForever { }

        vm.currentIdLiveData.value = 7

        assertThat(vm.recordingLiveData.value!!.title).isEqualTo("Seven")
    }

    @Test
    fun loadRecordingByIdSyncLoadsTheRecording() {
        val connectionId = seedActiveConnection()
        graph.appRepository.recordingData.addItem(Recording(id = 7, connectionId = connectionId, title = "Seven"))
        val vm = RecordingViewModel(context)

        vm.loadRecordingByIdSync(7)

        assertThat(vm.recording.title).isEqualTo("Seven")
    }

    // ---- preferences ----------------------------------------------------------------

    @Test
    fun showGenreColorFollowsThePreference() {
        seedActiveConnection()
        prefs().edit().putBoolean("genre_colors_for_recordings_enabled", true).commit()

        val vm = RecordingViewModel(context)

        assertThat(vm.showGenreColor.value).isTrue()
    }

    // ---- intent ---------------------------------------------------------------------

    @Test
    fun getIntentDataIncludesTheChannelAndStartForANonRunningRecording() {
        seedActiveConnection()
        val vm = RecordingViewModel(context)
        val recording = Recording(
            id = 1, title = "T", subtitle = "S", stop = 30_000, start = 10_000, state = "scheduled",
            isEnabled = true, channelId = 4, stopExtra = 5, priority = 2)

        val intent = vm.getIntentData(context, recording)

        assertThat(intent.getStringExtra("title")).isEqualTo("T")
        assertThat(intent.getStringExtra("subtitle")).isEqualTo("S")
        assertThat(intent.getLongExtra("stop", -1)).isEqualTo(30L)
        assertThat(intent.getIntExtra("channelId", -1)).isEqualTo(4)
        assertThat(intent.getLongExtra("start", -1)).isEqualTo(10L)
        assertThat(intent.getIntExtra("enabled", -1)).isEqualTo(1)
    }

    @Test
    fun getIntentDataOmitsTheChannelAndStartForARunningRecording() {
        seedActiveConnection()
        val vm = RecordingViewModel(context)
        val recording = Recording(id = 2, title = "T", stop = 30_000, state = "recording", error = null, channelId = 4)

        val intent = vm.getIntentData(context, recording)

        assertThat(intent.getLongExtra("stop", -1)).isEqualTo(30L)
        assertThat(intent.hasExtra("channelId")).isFalse()
        assertThat(intent.hasExtra("start")).isFalse()
    }
}
