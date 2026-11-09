package org.tvheadend.tvhclient.ui.features.programs

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
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory

/**
 * Tests for [ProgramViewModel].
 *
 * The interesting behaviour is the two `switchMap` chains: `programs` follows the selected channel
 * and time, `recordings` follows the selected channel; both fall back to "everything" when the
 * channel id is 0. Programs are seeded with a future start so they always fall inside the
 * "from now on" window the queries use.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ProgramViewModelTest {

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

    private fun seedFutureProgram(eventId: Int, connectionId: Int, channelId: Int, title: String) {
        val start = System.currentTimeMillis() + 3_600_000
        graph.appRepository.programData.addItem(
            Program(eventId = eventId, connectionId = connectionId, channelId = channelId, start = start, stop = start + 600_000, title = title))
    }

    // ---- program --------------------------------------------------------------------

    @Test
    fun programIsLoadedWhenTheEventIdChanges() {
        val connectionId = seedActiveConnection()
        graph.appRepository.programData.addItem(
            Program(eventId = 42, connectionId = connectionId, channelId = 1, title = "Show"))
        val vm = ProgramViewModel(context)
        vm.program.observeForever { }

        assertThat(vm.program.value).isNull()

        vm.eventIdLiveData.value = 42

        assertThat(vm.program.value!!.title).isEqualTo("Show")
    }

    // ---- programs -------------------------------------------------------------------

    @Test
    fun programsAreLoadedFromNowWhenNoChannelIsSelected() {
        val connectionId = seedActiveConnection()
        seedFutureProgram(eventId = 1, connectionId = connectionId, channelId = 7, title = "OnSeven")
        seedFutureProgram(eventId = 2, connectionId = connectionId, channelId = 8, title = "OnEight")
        val vm = ProgramViewModel(context)
        vm.programs.observeForever { }

        assertThat(vm.programs.value!!.map { it.title }).containsExactly("OnSeven", "OnEight")
    }

    @Test
    fun programsAreLoadedForTheSelectedChannel() {
        val connectionId = seedActiveConnection()
        seedFutureProgram(eventId = 1, connectionId = connectionId, channelId = 7, title = "OnSeven")
        seedFutureProgram(eventId = 2, connectionId = connectionId, channelId = 8, title = "OnEight")
        val vm = ProgramViewModel(context)
        vm.programs.observeForever { }

        vm.channelIdLiveData.value = 7

        assertThat(vm.programs.value!!.map { it.title }).containsExactly("OnSeven")
    }

    // ---- recordings -----------------------------------------------------------------

    @Test
    fun recordingsFollowTheSelectedChannel() {
        val connectionId = seedActiveConnection()
        graph.appRepository.recordingData.addItem(Recording(id = 1, connectionId = connectionId, channelId = 7))
        graph.appRepository.recordingData.addItem(Recording(id = 2, connectionId = connectionId, channelId = 8))
        val vm = ProgramViewModel(context)
        vm.recordings.observeForever { }

        assertThat(vm.recordings.value).hasSize(2)

        vm.channelIdLiveData.value = 7

        assertThat(vm.recordings.value!!.map { it.channelId }).containsExactly(7)
    }

    // ---- preferences ----------------------------------------------------------------

    @Test
    fun showFlagsFollowThePreferences() {
        prefs().edit()
            .putBoolean("genre_colors_for_programs_enabled", true)
            .putBoolean("program_subtitle_enabled", true)
            .putBoolean("program_artwork_enabled", false)
            .commit()

        val vm = ProgramViewModel(context)

        assertThat(vm.showGenreColor.value).isTrue()
        assertThat(vm.showProgramSubtitles.value).isTrue()
        assertThat(vm.showProgramArtwork.value).isFalse()
    }

    // ---- recording profiles ---------------------------------------------------------

    @Test
    fun getRecordingProfileResolvesTheProfileFromTheActiveServerStatus(): Unit = runBlocking {
        val connectionId = seedActiveConnection()
        graph.appRepository.serverProfileData.addItem(
            ServerProfile(id = 5, connectionId = connectionId, name = "RecProfile", type = ServerProfile.RECORDING_PROFILE))
        val status = graph.appRepository.serverStatusData.getActiveItem()
        status.recordingServerProfileId = 5
        graph.appRepository.serverStatusData.updateItem(status)
        val vm = ProgramViewModel(context)

        assertThat(vm.getRecordingProfile()!!.name).isEqualTo("RecProfile")
    }

    @Test
    fun getRecordingProfileNamesReturnsTheNamesOfTheRecordingProfiles(): Unit = runBlocking {
        val connectionId = seedActiveConnection()
        graph.appRepository.serverProfileData.addItem(
            ServerProfile(id = 5, connectionId = connectionId, name = "RecProfile", type = ServerProfile.RECORDING_PROFILE))
        val vm = ProgramViewModel(context)

        assertThat(vm.getRecordingProfileNames().asList()).containsExactly("RecProfile")
    }
}
