package org.tvheadend.tvhclient.ui.features.information

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
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.testutil.observedValue

/**
 * Tests for [StatusViewModel].
 *
 * It exposes the various row counts and two "should a notification be shown" mediators that depend
 * on a preference plus the running-recording count / the server's free disk space. Rows are only
 * visible for the active connection, so one is seeded first (which also creates its server status).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StatusViewModelTest {

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

    // ---- counts ---------------------------------------------------------------------

    @Test
    fun exposesTheVariousCountsOfTheActiveConnection() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId))
        graph.appRepository.channelData.addItem(Channel(id = 2, connectionId = connectionId))
        graph.appRepository.programData.addItem(Program(eventId = 10, connectionId = connectionId))
        graph.appRepository.timerRecordingData.addItem(TimerRecording(id = "t1", connectionId = connectionId))
        graph.appRepository.seriesRecordingData.addItem(SeriesRecording(id = "s1", connectionId = connectionId))
        graph.appRepository.recordingData.addItem(Recording(id = 1, connectionId = connectionId, state = "completed"))
        graph.appRepository.recordingData.addItem(Recording(id = 2, connectionId = connectionId, state = "scheduled"))

        val vm = StatusViewModel(context)

        assertThat(vm.channelCount.observedValue()).isEqualTo(2)
        assertThat(vm.programCount.observedValue()).isEqualTo(1)
        assertThat(vm.timerRecordingCount.observedValue()).isEqualTo(1)
        assertThat(vm.seriesRecordingCount.observedValue()).isEqualTo(1)
        assertThat(vm.completedRecordingCount.observedValue()).isEqualTo(1)
        assertThat(vm.scheduledRecordingCount.observedValue()).isEqualTo(1)
        assertThat(vm.failedRecordingCount.observedValue()).isEqualTo(0)
        assertThat(vm.removedRecordingCount.observedValue()).isEqualTo(0)
    }

    // ---- running recording notification ---------------------------------------------

    @Test
    fun showRunningRecordingCountIsFalseByDefault() {
        val connectionId = seedActiveConnection()
        graph.appRepository.recordingData.addItem(Recording(id = 1, connectionId = connectionId, state = "recording"))

        val vm = StatusViewModel(context)

        assertThat(vm.showRunningRecordingCount.observedValue()).isFalse()
    }

    @Test
    fun showRunningRecordingCountIsTrueWhenEnabledAndARecordingIsRunning() {
        val connectionId = seedActiveConnection()
        prefs().edit().putBoolean("notify_running_recording_count_enabled", true).commit()
        graph.appRepository.recordingData.addItem(Recording(id = 1, connectionId = connectionId, state = "recording"))

        val vm = StatusViewModel(context)

        assertThat(vm.showRunningRecordingCount.observedValue()).isTrue()
    }

    @Test
    fun disablingNotificationsHidesTheRunningRecordingNotification() {
        val connectionId = seedActiveConnection()
        prefs().edit().putBoolean("notify_running_recording_count_enabled", true).commit()
        graph.appRepository.recordingData.addItem(Recording(id = 1, connectionId = connectionId, state = "recording"))
        val vm = StatusViewModel(context)
        vm.showRunningRecordingCount.observeForever { }
        assertThat(vm.runningRecordingCount).isEqualTo(1)

        // The "notifications_enabled" preference defaults to false, so this disables them.
        vm.onSharedPreferenceChanged(prefs(), "notifications_enabled")

        assertThat(vm.showRunningRecordingCount.value).isFalse()
    }

    // ---- low storage space notification ---------------------------------------------

    @Test
    fun showLowStorageSpaceIsFalseByDefault() {
        seedActiveConnection()

        val vm = StatusViewModel(context)

        assertThat(vm.showLowStorageSpace.observedValue()).isFalse()
    }

    @Test
    fun showLowStorageSpaceIsTrueWhenEnabledAndBelowTheThreshold() {
        seedActiveConnection()
        prefs().edit().putBoolean("notify_low_storage_space_enabled", true).commit()

        val vm = StatusViewModel(context)

        // The auto-created server status has 0 free disk space, below the default threshold (1 GB).
        assertThat(vm.showLowStorageSpace.observedValue()).isTrue()
    }

    // ---- channels -------------------------------------------------------------------

    @Test
    fun getChannelByIdReturnsTheStoredChannelOrNull(): Unit = runBlocking {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId, name = "One"))

        val vm = StatusViewModel(context)

        assertThat(vm.getChannelById(1)!!.name).isEqualTo("One")
        assertThat(vm.getChannelById(999)).isNull()
    }

    // ---- disk space update handler --------------------------------------------------

    @Test
    fun startingAndStoppingTheDiskSpaceUpdateHandlerDoesNotThrow() {
        seedActiveConnection()
        val vm = StatusViewModel(context)

        vm.startDiskSpaceUpdateHandler()
        vm.stopDiskSpaceUpdateHandler()
    }
}
