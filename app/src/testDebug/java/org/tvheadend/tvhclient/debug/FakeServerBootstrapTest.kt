package org.tvheadend.tvhclient.debug

import android.app.Application
import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.source.ConnectionDataSource
import org.tvheadend.htsp.FakeTvheadendServer
import org.tvheadend.tvhclient.MainApplication
import timber.log.Timber
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

/**
 * Tests for [FakeServerBootstrap], the debug-only helper that starts the in-process
 * [FakeTvheadendServer] and adds the demo connections pointing at it (adding each one that is
 * missing).
 *
 * The bootstrap is only reachable through a real [MainApplication] (`context.applicationContext as?
 * MainApplication`), but `MainApplication.onCreate` builds the Room database and runs the legacy
 * SQLite migration, which cannot be set up in a unit test. A `mockk` [MainApplication] is therefore
 * substituted (this is exactly why every other `:app` Robolectric test uses a plain [Application]).
 * The repository/connection data source are `mockk`s over a small in-memory list, so the tests can
 * assert precisely which connection rows the bootstrap writes.
 *
 * `start` is suspending (it runs inside `MainApplication`'s migration coroutine so it never blocks
 * the main thread), so the tests drive it from [runTest]. The bootstrap keeps the started server in
 * a private field of its (singleton) object, which Robolectric does not reset between test methods.
 * It is therefore closed and cleared before and after every test via reflection.
 */
@RunWith(RobolectricTestRunner::class)
// Use a plain Application instead of MainApplication: the latter does database work in onCreate.
@Config(sdk = [34], application = Application::class)
class FakeServerBootstrapTest {

    private lateinit var context: Context
    private lateinit var app: MainApplication
    private lateinit var repository: AppRepository
    private lateinit var connectionData: ConnectionDataSource

    private val storedConnections = mutableListOf<Connection>()
    private val addedConnection = slot<Connection>()

    @Before
    fun setUp() {
        Timber.plant(NOOP_TREE)
        resetBootstrapState()

        context = mockk()
        app = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        connectionData = mockk(relaxed = true)
        storedConnections.clear()

        every { context.applicationContext } returns app
        every { app.appRepository } returns repository
        every { repository.connectionData } returns connectionData
        coEvery { connectionData.getItems() } answers { storedConnections.toList() }
        every { connectionData.addItem(capture(addedConnection)) } answers {
            storedConnections.add(addedConnection.captured)
        }
    }

    @After
    fun tearDown() {
        resetBootstrapState()
    }

    @Test
    fun startsTheFakeServerAndSeedsBothDemoConnections() = runTest {
        FakeServerBootstrap.start(context)

        assertThat(storedConnections.map { it.name })
            .containsExactly("Demo (fake server)", "Demo 2 (fake server)")

        val active = storedConnections.single { it.name == "Demo (fake server)" }
        assertThat(active.isActive).isTrue()
        assertThat(active.hostname).isEqualTo(FakeTvheadendServer.DEFAULT_HOST)
        assertThat(active.username).isEqualTo("demo")
        assertThat(active.password).isEqualTo("demo")
        assertThat(active.isSyncRequired).isTrue()
        assertThat(active.lastUpdate).isEqualTo(0L)
        assertThat(active.port).isEqualTo(active.streamingPort)
        assertThat(active.port).isGreaterThan(0)
        assertThat(active.serverUrl)
            .isEqualTo("http://${FakeTvheadendServer.DEFAULT_HOST}:${active.port}")
        assertThat(active.streamingUrl).isEqualTo(active.serverUrl)

        // The second connection points at the very same fake server but is left inactive.
        val second = storedConnections.single { it.name == "Demo 2 (fake server)" }
        assertThat(second.isActive).isFalse()
        assertThat(second.hostname).isEqualTo(FakeTvheadendServer.DEFAULT_HOST)
        assertThat(second.port).isEqualTo(active.port)
        assertThat(second.streamingPort).isEqualTo(active.port)
        assertThat(second.serverUrl).isEqualTo(active.serverUrl)
        assertThat(second.streamingUrl).isEqualTo(active.streamingUrl)
        assertThat(second.isSyncRequired).isTrue()
    }

    @Test
    fun startsAServerThatActuallyAcceptsConnections() = runTest {
        FakeServerBootstrap.start(context)

        val port = storedConnections.first().port
        val socket = SocketChannel.open()
        try {
            socket.connect(InetSocketAddress(FakeTvheadendServer.DEFAULT_HOST, port))
            assertThat(socket.isConnected).isTrue()
        } finally {
            socket.close()
        }
    }

    @Test
    fun onlyAddsTheDemoConnectionThatIsMissing() = runTest {
        // An existing install already has the first demo connection (e.g. from a previous run).
        val existing = Connection(name = "Demo (fake server)", isActive = true)
        storedConnections.add(existing)

        FakeServerBootstrap.start(context)

        // The pre-existing row is untouched and only the missing second one is added.
        assertThat(storedConnections).hasSize(2)
        assertThat(storedConnections).contains(existing)
        assertThat(storedConnections.map { it.name })
            .containsExactly("Demo (fake server)", "Demo 2 (fake server)")
    }

    @Test
    fun addsTheDemoConnectionsEvenWhenAnUnrelatedConnectionExists() = runTest {
        // A user-added (real) connection must not stop the demo connections being seeded.
        val realServer = Connection(name = "My real server", isActive = true)
        storedConnections.add(realServer)

        FakeServerBootstrap.start(context)

        assertThat(storedConnections.map { it.name })
            .containsExactly("My real server", "Demo (fake server)", "Demo 2 (fake server)")
        assertThat(storedConnections).contains(realServer)
    }

    @Test
    fun doesNothingWhenAllDemoConnectionsAlreadyExist() = runTest {
        storedConnections.add(Connection(name = "Demo (fake server)", isActive = true))
        storedConnections.add(Connection(name = "Demo 2 (fake server)", isActive = false))

        FakeServerBootstrap.start(context)

        assertThat(storedConnections).hasSize(2)
        verify(exactly = 0) { connectionData.addItem(any()) }
    }

    @Test
    fun doesNothingForANonMainApplicationContext() = runTest {
        every { context.applicationContext } returns mockk<Application>()

        FakeServerBootstrap.start(context)

        assertThat(storedConnections).isEmpty()
        verify(exactly = 0) { connectionData.addItem(any()) }
    }

    @Test
    fun startingTwiceOnlyStartsOneServerAndSeedsTheDemoConnectionsOnce() = runTest {
        FakeServerBootstrap.start(context)
        val afterFirstStart = storedConnections.toList()

        FakeServerBootstrap.start(context)

        assertThat(storedConnections).hasSize(2)
        assertThat(storedConnections).containsExactlyElementsIn(afterFirstStart)
    }

    @Test
    fun waitsUntilTheInsertedRowsBecomeVisible() = runTest {
        // addItem() inserts asynchronously in production, so the bootstrap polls - with a delay -
        // until the rows are visible. Make them appear only on the third read: the first read is
        // the "which demo connections already exist?" check, the second is the first poll.
        var reads = 0
        coEvery { connectionData.getItems() } answers {
            reads++
            if (reads <= 2) emptyList() else storedConnections.toList()
        }

        FakeServerBootstrap.start(context)

        assertThat(storedConnections).hasSize(2)
        assertThat(reads).isAtLeast(3)
    }

    @Test
    fun startsTheServerButSkipsSeedingWhenTheRepositoryFails() = runTest {
        // The seeding is wrapped in its own try/catch; a repository failure must not propagate.
        coEvery { connectionData.getItems() } throws IllegalStateException("database is broken")

        FakeServerBootstrap.start(context)

        assertThat(storedConnections).isEmpty()
        verify(exactly = 0) { connectionData.addItem(any()) }
    }

    /**
     * Closes the server the bootstrap started (if any) and clears its private singleton field, so
     * tests do not leak a running server / an occupied port into the next test method.
     */
    private fun resetBootstrapState() {
        val field = FakeServerBootstrap::class.java.getDeclaredField("server")
        field.isAccessible = true
        (field.get(FakeServerBootstrap) as? AutoCloseable)?.close()
        field.set(FakeServerBootstrap, null)
    }

    companion object {
        private val NOOP_TREE = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
        }
    }
}
