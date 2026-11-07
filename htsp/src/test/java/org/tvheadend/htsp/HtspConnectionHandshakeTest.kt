package org.tvheadend.htsp

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.api.AuthenticationRights
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * JVM (Robolectric) tests for the parts of the handshake that the client uses to learn what
 * the server supports: the protocol version that is advertised with `hello` and the rights
 * that are reported with the reply to `authenticate`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HtspConnectionHandshakeTest {

    private lateinit var server: FakeHtspServer

    @After
    fun tearDown() {
        if (::server.isInitialized) {
            server.close()
        }
    }

    @Test
    fun advertisesTheSupportedHtspVersion() {
        server = FakeHtspServer(username = USERNAME, password = PASSWORD).start()
        val listener = RecordingStateListener()
        val connection = newConnection(listener)
        try {
            connection.openConnection()
            connection.authenticate()
            assertTrue("Expected the connection to be authenticated", listener.authenticated.await(10, TimeUnit.SECONDS))

            val hello = server.lastHelloMessage
            assertTrue("The server should have received a hello message", hello != null)
            assertEquals(
                "The client should advertise the newest protocol version it can speak",
                HTSP_VERSION_FOR_TEST,
                hello!!.getLong("htspversion"),
            )
        } finally {
            connection.closeConnection()
        }
    }

    @Test
    fun parsesTheRightsOfTheGrantedAccess() {
        server = FakeHtspServer(username = USERNAME, password = PASSWORD, grantsAuthenticationRights = true).start()
        val listener = RecordingStateListener()
        val connection = newConnection(listener)
        try {
            connection.openConnection()
            connection.authenticate()
            assertTrue("Expected the connection to be authenticated", listener.authenticated.await(10, TimeUnit.SECONDS))

            val rights = listener.rights
            assertTrue("Expected rights to be reported, but got ${listener.results}", rights != null)
            assertTrue("admin", rights!!.isAdmin)
            assertTrue("streaming", rights.isStreaming)
            assertTrue("dvr", rights.isDvr)
            assertFalse("faileddvr", rights.isFailedDvr)
            assertTrue("anonymous", rights.isAnonymous)
            assertEquals("limitall", 4, rights.limitAll)
            assertEquals("limitdvr", 2, rights.limitDvr)
            assertEquals("limitstreaming", 1, rights.limitStreaming)
            assertEquals("uilevel", 3, rights.uiLevel)
            assertEquals("uilanguage", "de", rights.uiLanguage)
        } finally {
            connection.closeConnection()
        }
    }

    @Test
    fun grantsEverythingWhenTheServerDoesNotReportRights() {
        // Servers with HTSP version 25 and older do not send the rights at all.
        server = FakeHtspServer(username = USERNAME, password = PASSWORD, grantsAuthenticationRights = false).start()
        val listener = RecordingStateListener()
        val connection = newConnection(listener)
        try {
            connection.openConnection()
            connection.authenticate()
            assertTrue("Expected the connection to be authenticated", listener.authenticated.await(10, TimeUnit.SECONDS))

            val rights = listener.rights
            assertTrue("Expected rights to be reported, but got ${listener.results}", rights != null)
            assertTrue("streaming", rights!!.isStreaming)
            assertTrue("dvr", rights.isDvr)
            assertFalse("admin", rights.isAdmin)
            assertEquals("uilevel", 0, rights.uiLevel)
        } finally {
            connection.closeConnection()
        }
    }

    private fun newConnection(listener: ServerConnectionStateListener): HtspConnection {
        val connectionData = HtspConnectionData(
            username = USERNAME,
            password = PASSWORD,
            serverUrl = server.url,
            versionName = "test",
            versionCode = 1,
            connectionTimeout = 5_000,
        )
        return HtspConnection(connectionData, listener, null)
    }

    /** Records the authentication result including the rights that the server granted. */
    private class RecordingStateListener : ServerConnectionStateListener {

        val results = CopyOnWriteArrayList<AuthenticationStateResult>()
        val authenticated = CountDownLatch(1)

        val rights: AuthenticationRights?
            get() = results.filterIsInstance<AuthenticationStateResult.Authenticated>().firstOrNull()?.rights

        override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
            results.add(result)
            if (result is AuthenticationStateResult.Authenticated) {
                authenticated.countDown()
            }
        }

        override fun onConnectionStateChange(result: ConnectionStateResult) {
            // Not relevant for the handshake tests.
        }
    }

    companion object {
        private const val USERNAME = "testuser"
        private const val PASSWORD = "testpassword"

        // HtspMessage.HTSP_VERSION is package private, which this test package can read.
        private const val HTSP_VERSION_FOR_TEST = HtspMessage.HTSP_VERSION

        init {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
            })
        }
    }
}
