package org.tvheadend.htsp

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.api.AuthenticationFailureReason
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import timber.log.Timber
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * JVM (Robolectric) tests for the failure paths of [HtspConnection] that the happy-path
 * connection tests ([HtspConnectionHandshakeTest], [HtspConnectionAuthenticationTest],
 * [HtspConnectionDataFlowTest]) do not touch:
 *
 *  - opening a socket to a port where nothing is listening,
 *  - a server that rejects the supplied credentials,
 *  - closing a connection that was never authenticated (or never opened at all).
 *
 * The connection timeout is kept short so a failure is reported quickly instead of blocking the
 * test thread. Robolectric is required for the same reason as in the other connection tests:
 * [HtspConnection] resolves host/port via `android.net.Uri` and stores its response handlers in
 * an `android.util.SparseArray`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HtspConnectionErrorTest {

    private lateinit var server: FakeHtspServer

    @After
    fun tearDown() {
        if (::server.isInitialized) {
            server.close()
        }
    }

    @Test
    fun connectingToAClosedPortFails() {
        val listener = RecordingStateListener()
        val connection = newConnection(closedPortUrl(), listener)
        try {
            connection.openConnection()

            assertTrue(
                "Expected a failed connection result, but got ${listener.connectionResults}",
                listener.connectionFailed.await(10, TimeUnit.SECONDS),
            )
            assertFalse("The connection must not be reported as connected", listener.connected())
        } finally {
            connection.closeConnection()
        }
    }

    @Test
    fun authenticationIsRejectedForAnUnknownUsername() {
        server = FakeHtspServer(username = "expected-user", password = "secret").start()
        val listener = RecordingStateListener()
        val connection = newConnection(server.url, listener, username = "somebody-else")
        try {
            connection.openConnection()
            connection.authenticate()

            assertTrue(
                "Expected the authentication to be rejected, but got ${listener.authenticationResults}",
                listener.authenticationFailed.await(10, TimeUnit.SECONDS),
            )
            assertFalse("HtspConnection.isAuthenticated should be false", connection.isAuthenticated)
            assertTrue(
                "Expected a BadCredentials failure but got ${listener.authenticationResults}",
                listener.authenticationResults.any {
                    it is AuthenticationStateResult.Failed && it.reason is AuthenticationFailureReason.BadCredentials
                },
            )
        } finally {
            connection.closeConnection()
        }
    }

    @Test
    fun closingAnOpenButUnauthenticatedConnectionDoesNotThrow() {
        server = FakeHtspServer(username = USERNAME, password = PASSWORD).start()
        val listener = RecordingStateListener()
        val connection = newConnection(server.url, listener)
        try {
            connection.openConnection()
            // Never authenticate.

            connection.closeConnection()

            assertFalse("HtspConnection.isAuthenticated should be false", connection.isAuthenticated)
            assertTrue("The connection should report itself as not connected", connection.isNotConnected)
        } finally {
            // Closing twice must be just as harmless.
            connection.closeConnection()
        }
    }

    @Test
    fun closingANeverOpenedConnectionDoesNotThrow() {
        val listener = RecordingStateListener()
        val connection = newConnection(serverUrl = null, listener = listener)

        // socketChannel is null here, which the implementation has to tolerate.
        connection.closeConnection()

        assertTrue("The connection should report itself as not connected", connection.isNotConnected)
        assertFalse(connection.isAuthenticated)
    }

    private fun closedPortUrl(): String {
        // Ask the OS for a free port and then release it again, so nothing is listening there.
        val socket = ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return "http://127.0.0.1:$port"
    }

    private fun newConnection(
        serverUrl: String?,
        listener: ServerConnectionStateListener,
        username: String = USERNAME,
    ): HtspConnection {
        val connectionData = HtspConnectionData(
            username = username,
            password = PASSWORD,
            serverUrl = serverUrl,
            versionName = "test",
            versionCode = 1,
            // Short timeout so a hanging connect/reply fails fast instead of blocking the test.
            connectionTimeout = 1_500,
        )
        return HtspConnection(connectionData, listener, null)
    }

    /** Records every connection/authentication state change emitted by [HtspConnection]. */
    private class RecordingStateListener : ServerConnectionStateListener {

        val connectionResults = CopyOnWriteArrayList<ConnectionStateResult>()
        val authenticationResults = CopyOnWriteArrayList<AuthenticationStateResult>()
        val connectionFailed = CountDownLatch(1)
        val authenticationFailed = CountDownLatch(1)

        fun connected(): Boolean = connectionResults.any { it is ConnectionStateResult.Connected }

        override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
            authenticationResults.add(result)
            if (result is AuthenticationStateResult.Failed) {
                authenticationFailed.countDown()
            }
        }

        override fun onConnectionStateChange(result: ConnectionStateResult) {
            connectionResults.add(result)
            if (result is ConnectionStateResult.Failed) {
                connectionFailed.countDown()
            }
        }
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
