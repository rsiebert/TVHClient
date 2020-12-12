package org.tvheadend.http

import com.burgstaller.okhttp.AuthenticationCacheInterceptor
import com.burgstaller.okhttp.CachingAuthenticatorDecorator
import com.burgstaller.okhttp.digest.CachingAuthenticator
import com.burgstaller.okhttp.digest.Credentials
import com.burgstaller.okhttp.digest.DigestAuthenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.tvheadend.api.*
import org.tvheadend.api.ConnectionStateResult.Connected
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


class HttpConnection(private val httpConnectionData: HttpConnectionData,
                     private val connectionListener: ServerConnectionStateListener,
                     messageListener: ServerMessageListener<JSONObject>) : Thread(), ServerConnectionInterface<JSONObject>, ServerConnectionMessageInterface<JSONObject, Request> {

    private var client: OkHttpClient
    private val lock: Lock = ReentrantLock()
    var isConnecting = false
    private var isRunning = false
    private val messageListeners = HashSet<ServerMessageListener<JSONObject>>()
    private val responseHandlers = HashMap<String, ServerResponseListener<JSONObject>?>()
    private val messageQueue = LinkedList<Request>()

    init {
        messageListeners.add(messageListener)

        val authenticator = DigestAuthenticator(Credentials(httpConnectionData.username, httpConnectionData.password))
        val authCache: Map<String, CachingAuthenticator> = ConcurrentHashMap()
        client = OkHttpClient.Builder()
                .authenticator(CachingAuthenticatorDecorator(authenticator, authCache))
                .addInterceptor(AuthenticationCacheInterceptor(authCache))
                .connectTimeout(httpConnectionData.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(httpConnectionData.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(httpConnectionData.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)
                .build()
    }

    override fun addMessageListener(listener: ServerMessageListener<JSONObject>) {
        messageListeners.add(listener)
    }

    override fun removeMessageListener(listener: ServerMessageListener<JSONObject>) {
        messageListeners.remove(listener)
    }

    override fun openConnection() {
        Timber.i("Opening Connection")
        connectionListener.onConnectionStateChange(ConnectionStateResult.Connecting())

        if (isRunning) return

        Timber.d("Connection to server is starting")
        isConnecting = true

        try {
            Timber.d("Building request with url ${httpConnectionData.url}")
            val request = Request.Builder().url("${httpConnectionData.url}").build()

            client.newCall(request).execute().use { response ->

                for ((name, value) in response.headers) {
                    Timber.d("Header $name: $value")
                }

                Timber.d("Received response code ${response.code}")
                Timber.d("Received response body ${response.body.toString()}")

                if (!response.isSuccessful) {
                    Timber.d("Response was not successful")
                    when (response.code) {
                        HTTP_UNAVAILABLE, HTTP_NOT_FOUND -> {
                            connectionListener.onConnectionStateChange(ConnectionStateResult.Failed(ConnectionFailureReason.UnresolvedAddress()))
                        }
                        HTTP_UNAUTHORIZED -> {
                            connectionListener.onAuthenticationStateChange(AuthenticationStateResult.Failed(AuthenticationFailureReason.BadCredentials()))
                        }
                        else -> {
                            connectionListener.onConnectionStateChange(ConnectionStateResult.Failed(ConnectionFailureReason.Other()))
                        }
                    }
                } else {
                    Timber.d("Response was successful, connection thread can be started")
                    connectionListener.onAuthenticationStateChange(AuthenticationStateResult.Authenticated())
                    isRunning = true
                    isConnecting = false
                    isAuthenticated = true
                    start()
                }
            }
        } catch (e: IllegalStateException) {
            Timber.d(e, "Caught illegal state exception, call has already been executed")
            connectionListener.onConnectionStateChange(ConnectionStateResult.Failed(ConnectionFailureReason.SocketException()))
        } catch (e: IOException) {
            Timber.d(e, "Caught exception while connecting to server")
            connectionListener.onConnectionStateChange(ConnectionStateResult.Failed(ConnectionFailureReason.Other()))
        } finally {

        }
        Timber.d("Opened connection")
    }

    override val isNotConnected: Boolean
        get() = !isRunning

    override var isAuthenticated: Boolean = false

    override fun authenticate() {
        // NOP
    }

    override fun sendMessage(message: Request) {
        sendMessage(message, null)
    }

    override fun sendMessage(message: Request, listener: ServerResponseListener<JSONObject>?) {
        if (isNotConnected) {
            Timber.d("Not sending message, not connected to server")
            // TODO create json response not connected ...
        }

        responseHandlers[message.tag().toString()] = listener
        messageQueue.add(message)
    }

    override fun closeConnection() {
        Timber.d("Closing connection")
        responseHandlers.clear()
        messageQueue.clear()
        isAuthenticated = false
        isConnecting = false
        isRunning = false
        Timber.d("Connection closed")
    }

    override fun run() {
        Timber.d("Starting Connection thread")
        connectionListener.onConnectionStateChange(Connected())
        while (isRunning) {
            lock.lock()
            try {
                val request: Request? = messageQueue.poll()
                if (request != null) {
                    client.newCall(request).execute().use { response ->
                        Timber.d("Received response code ${response.code}")
                        Timber.d("Received response body ${response.body.toString()}")
                        if (response.isSuccessful) {
                            handleMessage(JSONObject(response.body.toString()), request.tag().toString())
                        }
                    }
                }
            } catch (e: IllegalStateException) {
                Timber.d(e, "Caught illegal state exception, call has already been executed")
                connectionListener.onConnectionStateChange(ConnectionStateResult.Failed(ConnectionFailureReason.SocketException()))
                isRunning = false
            } catch (e: IOException) {
                Timber.d(e, "Caught exception while connecting to server")
                connectionListener.onConnectionStateChange(ConnectionStateResult.Failed(ConnectionFailureReason.Other()))
                isRunning = false
            } finally {
                lock.unlock()
            }
        }
        closeConnection()
        Timber.d("Connection thread stopped")
    }

    private fun handleMessage(msg: JSONObject, tag: String) {
        Timber.d("Handling message with tag $tag")
        val handler: ServerResponseListener<JSONObject>? = responseHandlers[tag]
        responseHandlers.remove(tag)
        if (handler != null) {
            synchronized(handler) { handler.handleResponse(msg) }
            return
        }
        for (listener in messageListeners) {
            listener.onMessage(msg)
        }
    }
}