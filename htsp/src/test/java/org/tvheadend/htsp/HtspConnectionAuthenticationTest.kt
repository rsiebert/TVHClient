package org.tvheadend.htsp

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.api.AuthenticationFailureReason
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * JVM (Robolectric) tests that drive the real [HtspConnection] against the reusable
 * [FakeHtspServer].
 *
 * Robolectric is required because [HtspConnection.openConnection] resolves host/port via
 * `android.net.Uri` and [HtspConnection] stores its response handlers in an
 * `android.util.SparseArray`. Those are `Stub!` classes with the default
 * "return default values" setup, so a real implementation has to be supplied, which is
 * exactly what Robolectric does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HtspConnectionAuthenticationTest {

    private lateinit var server: FakeHtspServer

    @Before
    fun setUp() {
        server = FakeHtspServer(username = USERNAME, password = PASSWORD).start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun authenticateWithCorrectPasswordSucceeds() {
        val listener = RecordingStateListener()
        val connection = newConnection(password = PASSWORD, listener = listener)
        try {
            connection.openConnection()
            connection.authenticate()

            assertTrue(
                "Expected the connection to be authenticated",
                listener.authenticated.await(10, TimeUnit.SECONDS),
            )
            assertTrue("HtspConnection.isAuthenticated should be true", connection.isAuthenticated)
            assertTrue(
                "Expected an Authenticated result but got ${listener.results}",
                listener.results.any { it is AuthenticationStateResult.Authenticated },
            )
            assertTrue(
                "On success no Failed result should be emitted but got ${listener.results}",
                listener.results.none { it is AuthenticationStateResult.Failed },
            )
            // Sanity check that the fake server actually took part in the handshake.
            assertTrue("The server should have received a hello message", server.helloCount.get() >= 1)
            assertTrue("The server should have received an authenticate message", server.authenticateCount.get() >= 1)
        } finally {
            connection.closeConnection()
        }
    }

    @Test
    fun authenticateWithWrongPasswordIsRejected() {
        val listener = RecordingStateListener()
        val connection = newConnection(password = "definitely-not-the-password", listener = listener)
        try {
            connection.openConnection()
            connection.authenticate()

            assertTrue(
                "Expected the authentication to be rejected",
                listener.failed.await(10, TimeUnit.SECONDS),
            )
            assertFalse("HtspConnection.isAuthenticated should be false", connection.isAuthenticated)

            // Note: HtspConnection.authenticate() first reports BadCredentials from its
            // response handler and then - because isAuthenticated stays false - the blocking
            // wait() path adds a generic "Other" failure on top. We therefore assert that
            // BadCredentials was among the reported failures rather than being the last one.
            assertTrue(
                "Expected a BadCredentials failure but got ${listener.results}",
                listener.results.any {
                    it is AuthenticationStateResult.Failed && it.reason is AuthenticationFailureReason.BadCredentials
                },
            )
        } finally {
            connection.closeConnection()
        }
    }

    private fun newConnection(password: String, listener: ServerConnectionStateListener): HtspConnection {
        val connectionData = HtspConnectionData(
            username = USERNAME,
            password = password,
            serverUrl = server.url,
            versionName = "test",
            versionCode = 1,
            connectionTimeout = 5_000,
        )
        return HtspConnection(connectionData, listener, null)
    }

    /** Records every authentication/connection state change emitted by [HtspConnection]. */
    private class RecordingStateListener : ServerConnectionStateListener {

        val results = CopyOnWriteArrayList<AuthenticationStateResult>()
        val authenticated = CountDownLatch(1)
        val failed = CountDownLatch(1)

        override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
            results.add(result)
            when (result) {
                is AuthenticationStateResult.Authenticated -> authenticated.countDown()
                is AuthenticationStateResult.Failed -> failed.countDown()
                else -> Unit
            }
        }

        override fun onConnectionStateChange(result: ConnectionStateResult) {
            // Not relevant for the authentication tests.
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
