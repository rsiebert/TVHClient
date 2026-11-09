package org.tvheadend.tvhclient.ui.features.settings

import android.app.Application
import android.content.Intent
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
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.testutil.observedValue

/**
 * Tests for [SettingsViewModel].
 *
 * Unlike the other ViewModels it does not extend `BaseViewModel`; it resolves the graph itself and
 * is used by the settings screens to read and mutate connections, server profiles and server
 * status. A [TestAppGraph] over an in-memory database is installed before it is constructed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SettingsViewModelTest {

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

    // ---- navigation -----------------------------------------------------------------

    @Test
    fun navigationMenuIdDefaultsToDefaultAndCanBeChanged() {
        seedActiveConnection()
        val vm = SettingsViewModel(context)
        val values = mutableListOf<String>()
        vm.getNavigationMenuId().observeForever { values.add(it.peekContent()) }

        assertThat(values).containsExactly("default")

        vm.setNavigationMenuId("connections")

        assertThat(values).containsExactly("default", "connections").inOrder()
    }

    // ---- connections ----------------------------------------------------------------

    @Test
    fun exposesTheConnectionCountListAndActiveConnection() {
        seedActiveConnection()
        graph.appRepository.connectionData.addItem(Connection(name = "Other"))
        val vm = SettingsViewModel(context)

        assertThat(vm.connectionCountLiveData.observedValue()).isEqualTo(2)
        assertThat(vm.connectionListLiveData.observedValue()!!.map { it.name }).containsExactly("Active", "Other")
        assertThat(vm.activeConnectionLiveData.observedValue()!!.name).isEqualTo("Active")
    }

    @Test
    fun loadConnectionByIdSelectsTheConnectionToEdit(): Unit = runBlocking {
        seedActiveConnection()
        graph.appRepository.connectionData.addItem(Connection(name = "Other"))
        val otherId = graph.appRepository.connectionData.getItems().first { it.name == "Other" }.id
        val vm = SettingsViewModel(context)

        vm.loadConnectionById(otherId)

        assertThat(vm.connectionToEdit.name).isEqualTo("Other")
    }

    @Test
    fun loadConnectionByIdWithAnUnknownIdFallsBackToAnEmptyConnection(): Unit = runBlocking {
        seedActiveConnection()
        val vm = SettingsViewModel(context)

        vm.loadConnectionById(9999)

        assertThat(vm.connectionToEdit.id).isEqualTo(0)
    }

    @Test
    fun addConnectionStoresTheConnectionToEditAsANewRow(): Unit = runBlocking {
        seedActiveConnection()
        val vm = SettingsViewModel(context)
        vm.loadConnectionById(9999)
        vm.connectionToEdit.name = "New"

        vm.addConnection()

        assertThat(vm.connectionCountLiveData.observedValue()).isEqualTo(2)
    }

    @Test
    fun updateConnectionPersistsTheChange(): Unit = runBlocking {
        seedActiveConnection()
        val vm = SettingsViewModel(context)
        val connection = vm.connectionToEdit
        connection.name = "Renamed"

        vm.updateConnection(connection)

        assertThat(graph.appRepository.connectionData.getActiveItem().name).isEqualTo("Renamed")
    }

    @Test
    fun removeConnectionDeletesTheRow(): Unit = runBlocking {
        seedActiveConnection()
        graph.appRepository.connectionData.addItem(Connection(name = "Other"))
        val vm = SettingsViewModel(context)
        val other = graph.appRepository.connectionData.getItems().first { it.name == "Other" }

        vm.removeConnection(other)

        assertThat(graph.appRepository.connectionData.getItems()).hasSize(1)
    }

    @Test
    fun setSyncRequiredForActiveConnectionMarksTheActiveConnection(): Unit = runBlocking {
        seedActiveConnection()
        val active = graph.appRepository.connectionData.getActiveItem()
        active.isSyncRequired = false
        graph.appRepository.connectionData.updateItem(active)
        val vm = SettingsViewModel(context)

        vm.setSyncRequiredForActiveConnection()

        assertThat(graph.appRepository.connectionData.getActiveItem().isSyncRequired).isTrue()
    }

    // ---- channels -------------------------------------------------------------------

    @Test
    fun getChannelListFollowsTheSortOrderPreference(): Unit = runBlocking {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId, serverOrder = 2))
        graph.appRepository.channelData.addItem(Channel(id = 2, connectionId = connectionId, serverOrder = 1))
        prefs().edit().putString("channel_sort_order", "0").commit()
        val vm = SettingsViewModel(context)

        assertThat(vm.getChannelList().map { it.id }).isEqualTo(listOf(2, 1))
    }

    // ---- server profiles ------------------------------------------------------------

    @Test
    fun exposesTheServerProfilesResolvedFromTheServerStatus(): Unit = runBlocking {
        val connectionId = seedActiveConnection()
        graph.appRepository.serverProfileData.addItem(
            ServerProfile(id = 5, connectionId = connectionId, name = "HTSP", type = ServerProfile.HTSP_PROFILE))
        graph.appRepository.serverProfileData.addItem(
            ServerProfile(id = 6, connectionId = connectionId, name = "Rec", type = ServerProfile.RECORDING_PROFILE))
        val status = graph.appRepository.serverStatusData.getActiveItem()
        status.htspPlaybackServerProfileId = 5
        status.recordingServerProfileId = 6
        graph.appRepository.serverStatusData.updateItem(status)
        val vm = SettingsViewModel(context)

        assertThat(vm.getHtspProfiles().map { it.name }).containsExactly("HTSP")
        assertThat(vm.getHtspProfile()!!.name).isEqualTo("HTSP")
        assertThat(vm.getRecordingProfiles().map { it.name }).containsExactly("Rec")
        assertThat(vm.getRecordingProfile()!!.name).isEqualTo("Rec")
        assertThat(vm.getHttpProfiles()).isEmpty()
    }

    @Test
    fun updateServerStatusPersistsTheChange(): Unit = runBlocking {
        seedActiveConnection()
        val vm = SettingsViewModel(context)
        val status = graph.appRepository.serverStatusData.getActiveItem()
        status.serverName = "MyServer"

        vm.updateServerStatus(status)

        assertThat(graph.appRepository.serverStatusData.getActiveItem().serverName).isEqualTo("MyServer")
    }

    // ---- misc -----------------------------------------------------------------------

    @Test
    fun clearDatabaseClearsTheRowsAndInvokesTheCallback(): Unit = runBlocking {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId))
        val vm = SettingsViewModel(context)
        var cleared = false
        val callback = object : MiscDataSource.DatabaseClearedCallback {
            override fun onDatabaseCleared() {
                cleared = true
            }
        }

        vm.clearDatabase(callback)

        assertThat(graph.appRepository.channelData.getItems()).isEmpty()
        assertThat(cleared).isTrue()
    }

    @Test
    fun setSnackbarMessageEmitsAnEvent() {
        seedActiveConnection()
        val vm = SettingsViewModel(context)
        val values = mutableListOf<Intent>()
        vm.snackbarMessageLiveData.observeForever { values.add(it.peekContent()) }

        val intent = Intent()
        vm.setSnackbarMessage(intent)

        assertThat(values).containsExactly(intent)
    }

    @Test
    fun isUnlockedReflectsTheBillingState() {
        seedActiveConnection()
        val vm = SettingsViewModel(context)

        assertThat(vm.isUnlockedLiveData.observedValue()).isFalse()
    }
}
