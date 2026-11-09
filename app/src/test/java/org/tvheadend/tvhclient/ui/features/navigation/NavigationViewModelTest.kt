package org.tvheadend.tvhclient.ui.features.navigation

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
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.testutil.observedValue

/**
 * Tests for [NavigationViewModel].
 *
 * Every ViewModel resolves its dependencies through [AppGraphProvider], so a [TestAppGraph] over an
 * in-memory database is installed before the ViewModel is built. A plain [Application] is
 * substituted for `MainApplication` because the latter's `onCreate` performs Firebase/Cast/Billing
 * work (and a migration) that must not run in a unit test.
 *
 * `NavigationViewModel` reads the start screen from the `start_screen` preference at construction
 * and exposes the navigation menu id as an [org.tvheadend.tvhclient.util.livedata.Event].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NavigationViewModelTest {

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

    // ---- start screen ---------------------------------------------------------------

    @Test
    fun usesTheDefaultStartScreenWhenNoPreferenceIsSet() {
        val expected = context.getString(R.string.pref_default_start_screen).toInt()

        val vm = NavigationViewModel(context)

        assertThat(vm.currentNavigationMenuId).isEqualTo(expected)
        assertThat(vm.getNavigationMenuId().value!!.peekContent()).isEqualTo(expected)
    }

    @Test
    fun usesTheStartScreenFromThePreference() {
        prefs().edit().putString("start_screen", "5").commit()

        val vm = NavigationViewModel(context)

        assertThat(vm.currentNavigationMenuId).isEqualTo(5)
        assertThat(vm.getNavigationMenuId().value!!.peekContent()).isEqualTo(5)
    }

    // ---- setNavigationMenuId --------------------------------------------------------

    @Test
    fun setNavigationMenuIdEmitsForANewIdButNotForTheSameId() {
        val vm = NavigationViewModel(context)
        val emissions = mutableListOf<Int>()
        vm.getNavigationMenuId().observeForever { emissions.add(it.peekContent()) }

        vm.setNavigationMenuId(3)
        vm.setNavigationMenuId(3)

        assertThat(emissions).containsExactly(0, 3).inOrder()
        assertThat(vm.currentNavigationMenuId).isEqualTo(3)
    }

    @Test
    fun setNavigationMenuIdAlwaysEmitsForTheSettingsMenu() {
        val vm = NavigationViewModel(context)
        val emissions = mutableListOf<Int>()
        vm.getNavigationMenuId().observeForever { emissions.add(it.peekContent()) }

        vm.setNavigationMenuId(NavigationDrawer.MENU_SETTINGS)
        vm.setNavigationMenuId(NavigationDrawer.MENU_SETTINGS)

        assertThat(emissions).containsExactly(0, NavigationDrawer.MENU_SETTINGS, NavigationDrawer.MENU_SETTINGS).inOrder()
    }

    // ---- setSelectedMenuItemId ------------------------------------------------------

    @Test
    fun setSelectedMenuItemIdChangesTheCurrentIdWithoutEmitting() {
        val vm = NavigationViewModel(context)
        val emissions = mutableListOf<Int>()
        vm.getNavigationMenuId().observeForever { emissions.add(it.peekContent()) }

        vm.setSelectedMenuItemId(7)
        // The id is already current now, so re-setting it must not emit again.
        vm.setNavigationMenuId(7)

        assertThat(vm.currentNavigationMenuId).isEqualTo(7)
        assertThat(emissions).containsExactly(0).inOrder()
    }

    // ---- setSelectedConnectionAsActive ----------------------------------------------

    @Test
    fun setSelectedConnectionAsActiveSwitchesToTheOtherConnection(): Unit = runBlocking {
        graph.appRepository.connectionData.addItem(Connection(name = "A", isActive = true))
        graph.appRepository.connectionData.addItem(Connection(name = "B"))
        val activeId = graph.appRepository.connectionData.getActiveItem().id
        val otherId = graph.appRepository.connectionData.getItems().first { it.id != activeId }.id
        val vm = NavigationViewModel(context)

        val switched = vm.setSelectedConnectionAsActive(otherId)

        assertThat(switched).isTrue()
        assertThat(graph.appRepository.connectionData.getActiveItem().id).isEqualTo(otherId)
    }

    @Test
    fun setSelectedConnectionAsActiveDoesNothingForTheAlreadyActiveConnection(): Unit = runBlocking {
        graph.appRepository.connectionData.addItem(Connection(name = "A", isActive = true))
        val activeId = graph.appRepository.connectionData.getActiveItem().id
        val vm = NavigationViewModel(context)

        val switched = vm.setSelectedConnectionAsActive(activeId)

        assertThat(switched).isFalse()
    }

    @Test
    fun setSelectedConnectionAsActiveDoesNothingForAnUnknownConnection(): Unit = runBlocking {
        graph.appRepository.connectionData.addItem(Connection(name = "A", isActive = true))
        val vm = NavigationViewModel(context)

        val switched = vm.setSelectedConnectionAsActive(9999)

        assertThat(switched).isFalse()
    }

    // ---- connections ----------------------------------------------------------------

    @Test
    fun exposesTheConnectionsAndTheActiveConnection() {
        graph.appRepository.connectionData.addItem(Connection(name = "A", isActive = true))
        graph.appRepository.connectionData.addItem(Connection(name = "B"))

        val vm = NavigationViewModel(context)

        assertThat(vm.connections.observedValue()!!.map { it.name }).containsExactly("A", "B")
    }
}
