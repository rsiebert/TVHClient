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
 * Tests for [NavigationViewModel] and the [NavDestination] model it is built on.
 *
 * Every ViewModel resolves its dependencies through [AppGraphProvider], so a [TestAppGraph] over an
 * in-memory database is installed before the ViewModel is built. A plain [Application] is
 * substituted for `MainApplication` because the latter's `onCreate` performs Firebase/Cast/Billing
 * work (and a migration) that must not run in a unit test.
 *
 * `NavigationViewModel` reads the start screen from the `start_screen` preference at construction
 * and exposes the navigation destination as an [org.tvheadend.tvhclient.util.livedata.Event].
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

    // ---- destinations ---------------------------------------------------------------

    /**
     * Guards the single source of truth: every destination must resolve back from both its drawer
     * menu item and the fragment it shows. This is what replaces the four hand-kept `when`
     * expressions, so it must stay consistent.
     */
    @Test
    fun everyDestinationResolvesBackFromItsMenuItemIdAndFragment() {
        NavDestination.entries.forEach { destination ->
            assertThat(NavDestination.fromMenuItemId(destination.menuItemId)).isEqualTo(destination)
            destination.fragmentFactory?.let { factory ->
                assertThat(NavDestination.fromFragment(factory())).isEqualTo(destination)
            }
        }
    }

    @Test
    fun anUnknownOrMissingIdFallsBackToTheChannelList() {
        assertThat(NavDestination.fromId("does_not_exist")).isEqualTo(NavDestination.CHANNELS)
        assertThat(NavDestination.fromId(null)).isEqualTo(NavDestination.CHANNELS)
    }

    // ---- start screen ---------------------------------------------------------------

    @Test
    fun usesTheDefaultStartScreenWhenNoPreferenceIsSet() {
        val default = NavDestination.fromId(context.getString(R.string.pref_default_start_screen))

        val vm = NavigationViewModel(context)

        assertThat(vm.currentDestination).isEqualTo(default)
        assertThat(vm.getNavigationMenuId().value!!.peekContent()).isEqualTo(default)
    }

    @Test
    fun usesTheStartScreenFromThePreference() {
        prefs().edit().putString("start_screen", "timer_recordings").commit()

        val vm = NavigationViewModel(context)

        assertThat(vm.currentDestination).isEqualTo(NavDestination.TIMER_RECORDINGS)
        assertThat(vm.getNavigationMenuId().value!!.peekContent()).isEqualTo(NavDestination.TIMER_RECORDINGS)
    }

    // ---- setNavigationMenuId --------------------------------------------------------

    @Test
    fun setNavigationMenuIdEmitsForANewDestinationButNotForTheSameOne() {
        val vm = NavigationViewModel(context)
        val emissions = mutableListOf<NavDestination>()
        vm.getNavigationMenuId().observeForever { emissions.add(it.peekContent()) }

        vm.setNavigationMenuId(NavDestination.SCHEDULED_RECORDINGS)
        vm.setNavigationMenuId(NavDestination.SCHEDULED_RECORDINGS)

        assertThat(emissions).containsExactly(NavDestination.CHANNELS, NavDestination.SCHEDULED_RECORDINGS).inOrder()
        assertThat(vm.currentDestination).isEqualTo(NavDestination.SCHEDULED_RECORDINGS)
    }

    @Test
    fun setNavigationMenuIdAlwaysEmitsForTheSettingsDestination() {
        val vm = NavigationViewModel(context)
        val emissions = mutableListOf<NavDestination>()
        vm.getNavigationMenuId().observeForever { emissions.add(it.peekContent()) }

        vm.setNavigationMenuId(NavDestination.SETTINGS)
        vm.setNavigationMenuId(NavDestination.SETTINGS)

        assertThat(emissions).containsExactly(NavDestination.CHANNELS, NavDestination.SETTINGS, NavDestination.SETTINGS).inOrder()
    }

    // ---- setSelectedMenuItemId ------------------------------------------------------

    @Test
    fun setSelectedMenuItemIdChangesTheCurrentDestinationWithoutEmitting() {
        val vm = NavigationViewModel(context)
        val emissions = mutableListOf<NavDestination>()
        vm.getNavigationMenuId().observeForever { emissions.add(it.peekContent()) }

        vm.setSelectedMenuItemId(NavDestination.REMOVED_RECORDINGS)
        // The destination is already current now, so re-setting it must not emit again.
        vm.setNavigationMenuId(NavDestination.REMOVED_RECORDINGS)

        assertThat(vm.currentDestination).isEqualTo(NavDestination.REMOVED_RECORDINGS)
        assertThat(emissions).containsExactly(NavDestination.CHANNELS).inOrder()
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
