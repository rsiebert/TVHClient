package org.tvheadend.htsp

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import org.tvheadend.api.ServerMessageListener
import org.tvheadend.api.ServerResponseListener
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * JVM (Robolectric) tests that drive the real [HtspConnection] through the post-authentication
 * data flow against the reusable [FakeHtspServer]:
 *
 *  1. The client authenticates.
 *  2. The client sends `enableAsyncMetadata`.
 *  3. The server pushes `tagAdd`, `channelAdd`, `eventAdd` and finally `initialSyncCompleted`.
 *  4. The client can then request the additional server data (`getDiskSpace`, `getProfiles`, ...).
 *
 * Robolectric is required for the same reason as in the other connection tests: [HtspConnection]
 * resolves the host/port via `android.net.Uri` and stores its response handlers in an
 * `android.util.SparseArray`. The `FakeHtspServer` reuses the production [HtspMessage] framing,
 * so the payloads are exactly what a real server would send.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HtspConnectionDataFlowTest {

    private lateinit var server: FakeHtspServer
    private lateinit var connection: HtspConnection

    private val messages = CopyOnWriteArrayList<Pair<String, HtspMessage>>()
    private val stateListener = RecordingStateListener()
    private val syncCompleted = CountDownLatch(1)

    @Before
    fun setUp() {
        server = FakeHtspServer(username = USERNAME, password = PASSWORD).start()
    }

    @After
    fun tearDown() {
        if (::connection.isInitialized) {
            connection.closeConnection()
        }
        server.close()
    }

    @Test
    fun receivesTheInitialMetadataAfterAskingForIt() {
        connectAndAuthenticate()

        val request = HtspMessage()
        request.setMethod("enableAsyncMetadata")
        request.put("epg", 1)
        connection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                // The acknowledgement to enableAsyncMetadata carries nothing we need here.
            }
        })

        assertTrue(
            "Expected the initial sync to complete, received: ${messages.map { it.first }}",
            syncCompleted.await(10, TimeUnit.SECONDS),
        )

        assertEquals("tags", 2, methods("tagAdd").size)
        assertEquals("channels", 3, methods("channelAdd").size)
        assertEquals("events", 6, methods("eventAdd").size)
        assertEquals("recordings", 5, methods("dvrEntryAdd").size)
        assertEquals("series recordings", 1, methods("autorecEntryAdd").size)
        assertEquals("initialSyncCompleted", 1, methods("initialSyncCompleted").size)

        // The first channel matches the sample data.
        val firstChannel = methods("channelAdd").first { it.getInteger("channelId") == 1 }
        assertEquals("Das Erste HD", firstChannel.getString("channelName"))
        assertEquals(1, firstChannel.getInteger("channelNumber"))
        assertEquals(listOf(1), firstChannel.getIntegerList("tags"))

        // The server status sent with the hello reply is delivered to the message listeners.
        val serverStatus = messages.firstOrNull { it.first == "serverStatus" }?.second
        assertNotNull("Expected a serverStatus message", serverStatus)
        assertEquals("FakeTVHeadend", serverStatus!!.getString("servername"))
        assertEquals(2, serverStatus.getInteger("api_version"))

        // Sanity check that the fixture actually pushed the metadata.
        assertEquals(2, server.tagAddCount.get())
        assertEquals(3, server.channelAddCount.get())
        assertEquals(6, server.eventAddCount.get())
        assertEquals(5, server.dvrEntryAddCount.get())
        assertEquals(1, server.autorecEntryAddCount.get())
        assertEquals(1, server.initialSyncCompletedCount.get())
    }

    @Test
    fun answersTheFollowUpServerDataRequests() {
        connectAndAuthenticate()

        // enableAsyncMetadata starts the push; the follow-up requests are normally sent once
        // the client has processed initialSyncCompleted.
        val enable = HtspMessage()
        enable.setMethod("enableAsyncMetadata")
        connection.sendMessage(enable, null)
        assertTrue("Expected the initial sync to complete", syncCompleted.await(10, TimeUnit.SECONDS))

        val diskSpace = awaitResponse("getDiskSpace")
        assertTrue("free disk space", diskSpace.getLong("freediskspace") > 0)
        assertTrue("total disk space", diskSpace.getLong("totaldiskspace") > 0)

        val profiles = awaitResponse("getProfiles")
        assertEquals(2, profiles.getList("profiles").size)

        val dvrConfigs = awaitResponse("getDvrConfigs")
        assertEquals(1, dvrConfigs.getList("dvrconfigs").size)
    }

    private fun awaitResponse(method: String): HtspMessage {
        val latch = CountDownLatch(1)
        val holder = arrayOfNulls<HtspMessage>(1)
        val request = HtspMessage()
        request.setMethod(method)
        connection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                holder[0] = response
                latch.countDown()
            }
        })
        assertTrue("Expected a response to $method", latch.await(10, TimeUnit.SECONDS))
        return holder[0]!!
    }

    private fun connectAndAuthenticate() {
        val connectionData = HtspConnectionData(
            username = USERNAME,
            password = PASSWORD,
            serverUrl = server.url,
            versionName = "test",
            versionCode = 1,
            connectionTimeout = 5_000,
        )
        connection = HtspConnection(
            connectionData,
            stateListener,
            object : ServerMessageListener<HtspMessage> {
                override fun onMessage(response: HtspMessage, method: String) {
                    messages.add(method to response)
                    if (method == "initialSyncCompleted") {
                        syncCompleted.countDown()
                    }
                }
            },
        )
        connection.openConnection()
        connection.authenticate()
        assertTrue(
            "Expected the connection to be authenticated",
            stateListener.authenticated.await(10, TimeUnit.SECONDS),
        )
    }

    private fun methods(name: String): List<HtspMessage> =
        messages.filter { it.first == name }.map { it.second }

    /** Records the authentication result. */
    private class RecordingStateListener : ServerConnectionStateListener {

        val authenticated = CountDownLatch(1)

        override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
            if (result is AuthenticationStateResult.Authenticated) {
                authenticated.countDown()
            }
        }

        override fun onConnectionStateChange(result: ConnectionStateResult) = Unit
    }

    companion object {
        private const val USERNAME = "testuser"
        private const val PASSWORD = "testpassword"

        init {
            // Keep Timber from writing to Logcat while the tests run.
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
            })
        }
    }
}
