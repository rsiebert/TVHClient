package org.tvheadend.htsp

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * JVM (Robolectric) tests for [FakeTvheadendServer], the debug-only in-process HTSP server that
 * lets a fresh debug install show demo channels, EPG data and recordings without a real server.
 *
 * The tests drive the server with the real [HtspConnection] over a loopback socket, the same way
 * the other connection tests drive [FakeHtspServer]: the client performs the `hello`/`authenticate`
 * handshake, asks for `enableAsyncMetadata` and then exercises every request/response path the fake
 * implements, plus its "ignore everything else" fallback.
 *
 * Robolectric is required because [HtspConnection] resolves the host/port through `android.net.Uri`
 * and keeps its response handlers in an `android.util.SparseArray`.
 *
 * [FakeTvheadendServer] lives in the `debug` source set, so this test has to live in `src/testDebug`
 * rather than `src/test` (the shared test source set cannot see debug-only classes).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FakeTvheadendServerTest {

    private lateinit var server: FakeTvheadendServer
    private lateinit var connection: HtspConnection

    private val messages = CopyOnWriteArrayList<Pair<String, HtspMessage>>()
    private val syncCompleted = CountDownLatch(1)

    @After
    fun tearDown() {
        if (::connection.isInitialized) {
            connection.closeConnection()
        }
        if (::server.isInitialized) {
            server.close()
        }
    }

    // ---- lifecycle -------------------------------------------------------------------

    @Test
    fun startsAndExposesTheBoundPortAndUrl() {
        server = FakeTvheadendServer(port = 0).start()

        assertThat(server.boundPort).isGreaterThan(0)
        assertThat(server.url).isEqualTo("http://${FakeTvheadendServer.DEFAULT_HOST}:${server.boundPort}")
    }

    @Test
    fun startIsIdempotentAndKeepsTheSamePort() {
        server = FakeTvheadendServer(port = 0)

        val started = server.start()
        val port = server.boundPort

        assertThat(server.start()).isSameInstanceAs(started)
        assertThat(server.boundPort).isEqualTo(port)
    }

    @Test
    fun closingIsIdempotentAndSafeBeforeStart() {
        server = FakeTvheadendServer(port = 0)
        // Never started: close() must tolerate the null server channel and accept thread.
        server.close()

        server.start()
        server.close()
        server.close()
    }

    @Test
    fun closingTheServerStopsAcceptingConnections() {
        server = FakeTvheadendServer(port = 0).start()
        val port = server.boundPort

        server.close()

        val socket = SocketChannel.open()
        try {
            assertThrows(IOException::class.java) {
                socket.connect(InetSocketAddress(FakeTvheadendServer.DEFAULT_HOST, port))
            }
        } finally {
            socket.close()
        }
    }

    // ---- handshake -------------------------------------------------------------------

    @Test
    fun repliesToHelloWithTheServerStatus() {
        server = FakeTvheadendServer(port = 0).start()
        connectAndAuthenticate()

        val serverStatus = messages.firstOrNull { it.first == "serverStatus" }?.second
        assertThat(serverStatus).isNotNull()
        assertThat(serverStatus!!.getString("servername")).isEqualTo("Fake TVHeadend")
        assertThat(serverStatus.getString("serverversion")).isEqualTo("4.3-demo")
        assertThat(serverStatus.getLong("htspversion")).isEqualTo(HtspMessage.HTSP_VERSION)
        assertThat(serverStatus.getInteger("api_version")).isEqualTo(2)
        assertThat(serverStatus.getString("webroot")).isEqualTo("/")
        assertThat(serverStatus.getByteArray("challenge")).hasLength(16)
        assertThat(serverStatus.getList("servercapability")).containsExactly("timeshift", "tvh-http-api")
    }

    @Test
    fun acceptsAnyCredentialsIncludingAWrongPassword() {
        // The fake never verifies the password; any credentials have to authenticate so the demo
        // UI is reachable on a fresh debug install.
        server = FakeTvheadendServer(port = 0).start()

        val (conn, listener) = newConnection(password = "definitely-not-the-password")
        try {
            assertTrue("Expected the connection to be authenticated", listener.authenticated.await(10, TimeUnit.SECONDS))
            assertThat(conn.isAuthenticated).isTrue()
            assertThat(listener.results.filterIsInstance<AuthenticationStateResult.Authenticated>()).hasSize(1)
            assertThat(listener.results.filterIsInstance<AuthenticationStateResult.Failed>()).isEmpty()
        } finally {
            conn.closeConnection()
        }
    }

    // ---- initial sync ----------------------------------------------------------------

    @Test
    fun pushesTheDemoMetadataAfterEnableAsyncMetadata() {
        server = FakeTvheadendServer(port = 0, hoursOfEpg = 2).start()
        connectAndAuthenticate()

        sendEnableAsyncMetadata()
        assertTrue("Expected the initial sync to complete", syncCompleted.await(10, TimeUnit.SECONDS))

        assertThat(methods("tagAdd")).hasSize(1)
        assertThat(methods("channelAdd")).hasSize(6)
        assertThat(methods("eventAdd")).hasSize(12) // 6 channels * 2 hours
        assertThat(methods("dvrEntryAdd")).hasSize(5)
        assertThat(methods("autorecEntryAdd")).hasSize(1)
        assertThat(methods("initialSyncCompleted")).hasSize(1)

        val tag = methods("tagAdd").single()
        assertThat(tag.getInteger("tagId")).isEqualTo(1)
        assertThat(tag.getString("tagName")).isEqualTo("Demo")
        assertThat(tag.getInteger("tagIndex")).isEqualTo(1)
        assertThat(tag.getIntegerList("members")).containsExactly(1, 2, 3, 4, 5, 6)

        val channel = methods("channelAdd").first { it.getInteger("channelId") == 1 }
        assertThat(channel.getInteger("channelNumber")).isEqualTo(1)
        assertThat(channel.getInteger("channelNumberMinor")).isEqualTo(0)
        assertThat(channel.getString("channelName")).isEqualTo("Demo One HD")
        assertThat(channel.getString("channelIcon")).isEqualTo("imagecache/demo1.png")
        assertThat(channel.getInteger("eventId")).isEqualTo(1000)
        assertThat(channel.getInteger("nextEventId")).isEqualTo(1001)
        assertThat(channel.getIntegerList("tags")).containsExactly(1)

        val events = methods("eventAdd")
        val firstEvent = events.first { it.getInteger("eventId") == 1000 }
        assertThat(firstEvent.getInteger("channelId")).isEqualTo(1)
        assertThat(firstEvent.getString("title")).isEqualTo("Demo One HD: Show 1")
        assertThat(firstEvent.getString("subtitle")).isEqualTo("Now")
        // Events are aligned to the top of the hour and last one hour.
        assertThat(firstEvent.getLong("start") % 3600).isEqualTo(0L)
        assertThat(firstEvent.getLong("stop") - firstEvent.getLong("start")).isEqualTo(3600L)
        assertThat(events.first { it.getInteger("eventId") == 1001 }.getString("subtitle")).isEqualTo("Upcoming")

        val recordings = methods("dvrEntryAdd").associateBy { it.getInteger("id") }
        assertThat(recordings.keys).containsExactly(1, 2, 3, 4, 5)
        assertThat(recordings.getValue(1).getString("state")).isEqualTo("scheduled")
        assertThat(recordings.getValue(2).getString("state")).isEqualTo("recording")
        assertThat(recordings.getValue(3).getString("state")).isEqualTo("completed")
        assertThat(recordings.getValue(1).getInteger("channel")).isEqualTo(1)
        assertThat(recordings.getValue(1).getString("title")).isEqualTo("Scheduled Demo Recording")

        // Two entries land in the DVR "failed" tab: a missed one that carries an error and one
        // that completed but was aborted by the user.
        assertThat(recordings.getValue(4).getString("state")).isEqualTo("missed")
        assertThat(recordings.getValue(4).getString("error")).isEqualTo("Subscription failed")
        assertThat(recordings.getValue(5).getString("state")).isEqualTo("completed")
        assertThat(recordings.getValue(5).getString("error")).isEqualTo("Aborted by user")

        val seriesRecording = methods("autorecEntryAdd").single()
        assertThat(seriesRecording.getString("id")).isEqualTo("demo-series-1")
        assertThat(seriesRecording.getInteger("enabled")).isEqualTo(1)
        assertThat(seriesRecording.getString("name")).isEqualTo("Demo Series Recording")
        assertThat(seriesRecording.getInteger("channel")).isEqualTo(1)
    }

    // ---- follow-up requests ----------------------------------------------------------

    @Test
    fun answersTheFollowUpServerDataRequests() {
        server = FakeTvheadendServer(port = 0, hoursOfEpg = 2).start()
        connectAndAuthenticate()

        val diskSpace = request("getDiskSpace")
        assertThat(diskSpace).isNotNull()
        assertThat(diskSpace!!.getLong("freediskspace")).isEqualTo(50L * 1024 * 1024 * 1024)
        assertThat(diskSpace.getLong("totaldiskspace")).isEqualTo(100L * 1024 * 1024 * 1024)

        val sysTime = request("getSysTime")
        assertThat(sysTime).isNotNull()
        assertThat(sysTime!!.getLong("time")).isGreaterThan(0L)
        assertThat(sysTime.getInteger("gmtoffset")).isEqualTo(0)

        val profiles = request("getProfiles")
        assertThat(profiles).isNotNull()
        val profileList = profiles!!.getList("profiles")
        assertThat(profileList).hasSize(2)
        assertThat(profileList.map { (it as HtspMessage).getString("name") }).containsExactly("htsp", "matroska")

        val dvrConfigs = request("getDvrConfigs")
        assertThat(dvrConfigs).isNotNull()
        val configList = dvrConfigs!!.getList("dvrconfigs")
        assertThat(configList).hasSize(1)
        assertThat((configList[0] as HtspMessage).getString("name")).isEqualTo("Default Profile")

        val events = request("getEvents")
        assertThat(events).isNotNull()
        assertThat(events!!.getList("events")).hasSize(12)

        val channel = request("getChannel") { it.put("channelId", 1) }
        assertThat(channel).isNotNull()
        assertThat(channel!!.getMethod()).isEqualTo("getChannel")
        assertThat(channel.getString("channelName")).isEqualTo("Demo One HD")

        val event = request("getEvent") { it.put("eventId", 1000) }
        assertThat(event).isNotNull()
        assertThat(event!!.getMethod()).isEqualTo("getEvent")
        assertThat(event.getString("title")).isEqualTo("Demo One HD: Show 1")
    }

    @Test
    fun answersTheProfileListApiRequest() {
        server = FakeTvheadendServer(port = 0).start()
        connectAndAuthenticate()

        val response = request("api") { it.put("path", "profile/list") }

        assertThat(response).isNotNull()
        assertThat(response!!.getString("response")).contains("matroska")
    }

    @Test
    fun acknowledgesOtherApiRequestsWithoutADataPayload() {
        server = FakeTvheadendServer(port = 0).start()
        connectAndAuthenticate()

        val response = request("api") { it.put("path", "some/other/endpoint") }

        assertThat(response).isNotNull()
        assertThat(response!!.getMethod()).isEqualTo("api")
        assertThat(response.containsKey("response")).isFalse()
    }

    @Test
    fun servesChannelIconsThroughTheHtspFileApi() {
        server = FakeTvheadendServer(port = 0).start()
        connectAndAuthenticate()

        val open = request("fileOpen") { it.put("file", "imagecache/demo1.png") }
        assertThat(open).isNotNull()
        val fileId = open!!.getInteger("id")
        val size = open.getLong("size")
        assertThat(fileId).isGreaterThan(0)
        assertThat(size).isGreaterThan(0L)

        val read = request("fileRead") {
            it.put("id", fileId)
            it.put("offset", 0L)
            it.put("size", size)
        }
        assertThat(read).isNotNull()
        val data = read!!.getByteArray("data")
        assertThat(data).hasLength(size.toInt())
        // A valid PNG starts with the 8-byte signature 89 50 4E 47 0D 0A 1A 0A.
        assertThat(data.take(8).joinToString("") { "%02X".format(it) }).isEqualTo("89504E470D0A1A0A")

        val close = request("fileClose") { it.put("id", fileId) }
        assertThat(close).isNotNull()
        assertThat(close!!.getMethod()).isEqualTo("fileClose")
    }

    // ---- edge cases ------------------------------------------------------------------

    @Test
    fun doesNotReplyToUnknownChannelOrEventRequests() {
        server = FakeTvheadendServer(port = 0).start()
        connectAndAuthenticate()

        // Unknown ids fall through the `firstOrNull { ... }?.let { ... }` and produce no reply.
        assertThat(request("getChannel", NEGATIVE_TIMEOUT_MS) { it.put("channelId", 999) }).isNull()
        assertThat(request("getEvent", NEGATIVE_TIMEOUT_MS) { it.put("eventId", 999_999) }).isNull()
    }

    @Test
    fun ignoresUnknownMethods() {
        server = FakeTvheadendServer(port = 0).start()
        connectAndAuthenticate()

        // Anything that is not part of the handshake/metadata flow is silently dropped.
        assertThat(request("subscribe", NEGATIVE_TIMEOUT_MS)).isNull()
    }

    @Test
    fun servesMultipleClientsConcurrently() {
        server = FakeTvheadendServer(port = 0).start()

        val (first, firstListener) = newConnection()
        val (second, secondListener) = newConnection()
        try {
            assertTrue("First client should authenticate", firstListener.authenticated.await(10, TimeUnit.SECONDS))
            assertTrue("Second client should authenticate", secondListener.authenticated.await(10, TimeUnit.SECONDS))
            assertThat(first.isAuthenticated).isTrue()
            assertThat(second.isAuthenticated).isTrue()
        } finally {
            first.closeConnection()
            second.closeConnection()
        }
    }

    // ---- helpers ---------------------------------------------------------------------

    private fun connectAndAuthenticate() {
        val (conn, listener) = newConnection(messageListener = object : ServerMessageListener<HtspMessage> {
            override fun onMessage(response: HtspMessage, method: String) {
                messages.add(method to response)
                if (method == "initialSyncCompleted") {
                    syncCompleted.countDown()
                }
            }
        })
        connection = conn
        assertTrue("Expected the connection to be authenticated", listener.authenticated.await(10, TimeUnit.SECONDS))
    }

    private fun newConnection(
        password: String = PASSWORD,
        messageListener: ServerMessageListener<HtspMessage>? = null,
    ): Pair<HtspConnection, RecordingStateListener> {
        val listener = RecordingStateListener()
        val connectionData = HtspConnectionData(
            username = USERNAME,
            password = password,
            serverUrl = server.url,
            versionName = "test",
            versionCode = 1,
            connectionTimeout = CONNECTION_TIMEOUT_MS,
        )
        val conn = HtspConnection(connectionData, listener, messageListener)
        conn.openConnection()
        conn.authenticate()
        return conn to listener
    }

    private fun sendEnableAsyncMetadata() {
        val request = HtspMessage()
        request.setMethod("enableAsyncMetadata")
        request.put("epg", 1)
        connection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                // The acknowledgement carries nothing we need here.
            }
        })
    }

    /**
     * Sends [method] and returns the correlated reply, or `null` if the server never answered
     * within [timeoutMillis]. No polling: a single bounded wait on a latch.
     */
    private fun request(
        method: String,
        timeoutMillis: Long = RESPONSE_TIMEOUT_MS,
        configure: (HtspMessage) -> Unit = {},
    ): HtspMessage? {
        val latch = CountDownLatch(1)
        val holder = arrayOfNulls<HtspMessage>(1)
        val message = HtspMessage()
        message.setMethod(method)
        configure(message)
        connection.sendMessage(message, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                holder[0] = response
                latch.countDown()
            }
        })
        return if (latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) holder[0] else null
    }

    private fun methods(name: String): List<HtspMessage> =
        messages.filter { it.first == name }.map { it.second }

    /** Records the authentication result emitted by [HtspConnection]. */
    private class RecordingStateListener : ServerConnectionStateListener {

        val results = CopyOnWriteArrayList<AuthenticationStateResult>()
        val authenticated = CountDownLatch(1)

        override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
            results.add(result)
            if (result is AuthenticationStateResult.Authenticated) {
                authenticated.countDown()
            }
        }

        override fun onConnectionStateChange(result: ConnectionStateResult) = Unit
    }

    companion object {
        private const val USERNAME = "demo"
        private const val PASSWORD = "demo"
        private const val CONNECTION_TIMEOUT_MS = 5_000
        private const val RESPONSE_TIMEOUT_MS = 5_000L
        private const val NEGATIVE_TIMEOUT_MS = 400L

        init {
            // Keep Timber from writing to Logcat while the tests run.
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
            })
        }
    }
}
