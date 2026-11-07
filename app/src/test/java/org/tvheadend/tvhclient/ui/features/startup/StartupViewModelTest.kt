package org.tvheadend.tvhclient.ui.features.startup

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.util.Pair
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.AppRepository
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.source.ConnectionDataSource
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.testutil.observedValue

/**
 * Tests for [StartupViewModel].
 *
 * The interesting part is `ConnectionStatusLiveData`, which folds the connection count and the
 * currently active connection into a `Pair`, which the ViewModel then narrows to
 * `Pair<count, isActive>`. Most tests drive the same code over the real in-memory repository by
 * writing through [org.tvheadend.data.source.ConnectionDataSource]; only the "count LiveData has no
 * value yet" case is supplied as a mock, because a Room count query always emits a value.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StartupViewModelTest {

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

    /** Installs a graph whose connection LiveData are the given, externally controlled values. */
    private fun installGraph(count: MutableLiveData<Int>, activeConnection: MutableLiveData<Connection>) {
        val connectionData = mockk<ConnectionDataSource>()
        every { connectionData.getLiveDataItemCount() } returns count
        every { connectionData.liveDataActiveItem } returns activeConnection
        val repository = mockk<AppRepository>()
        every { repository.connectionData } returns connectionData
        AppGraphProvider.graph = TestAppGraph(repository, TestAppGraphFactory.mainRepository())
    }

    /**
     * Drives the real in-memory repository instead of mocked source LiveData: adding connections
     * changes the count and activating one flips the active flag. Before the `LiveData` queries
     * were made nullable this was impossible, because the active-connection query threw an NPE as
     * soon as nothing was active - which is exactly the initial state here.
     */
    @Test
    fun combinesCountAndActiveFlagAsTheyChange() {
        val connectionData = graph.appRepository.connectionData
        val vm = StartupViewModel()
        val values = mutableListOf<Pair<Int, Boolean>>()
        vm.connectionStatus.observeForever { values.add(it) }

        // No connection at all, so nothing is active.
        assertThat(values.last()).isEqualTo(Pair(0, false))

        connectionData.addItem(Connection(name = "A"))
        assertThat(values.last()).isEqualTo(Pair(1, false))

        connectionData.addItem(Connection(name = "B"))
        assertThat(values.last()).isEqualTo(Pair(2, false))

        connectionData.updateItem(Connection(id = 2, name = "B", isActive = true))
        assertThat(values.last()).isEqualTo(Pair(2, true))
    }

    @Test
    fun treatsAMissingCountAsZero() {
        val count = MutableLiveData<Int>()
        val activeConnection = MutableLiveData(Connection(isActive = true))
        installGraph(count, activeConnection)

        val vm = StartupViewModel()

        assertThat(vm.connectionStatus.observedValue()).isEqualTo(Pair(0, true))
    }

    @Test
    fun readsTheRealRepositoryWhenAnActiveConnectionExists() {
        graph.appRepository.connectionData.addItem(Connection(name = "A", isActive = true))

        val vm = StartupViewModel()

        assertThat(vm.connectionStatus.observedValue()).isEqualTo(Pair(1, true))
    }
}
