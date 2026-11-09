package org.tvheadend.htsp;

import android.net.Uri;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.tvheadend.api.AuthenticationFailureReason;
import org.tvheadend.api.AuthenticationRights;
import org.tvheadend.api.AuthenticationStateResult;
import org.tvheadend.api.ConnectionFailureReason;
import org.tvheadend.api.ConnectionStateResult;
import org.tvheadend.api.ServerConnectionInterface;
import org.tvheadend.api.ServerConnectionMessageInterface;
import org.tvheadend.api.ServerConnectionStateListener;
import org.tvheadend.api.ServerMessageListener;
import org.tvheadend.api.ServerResponseListener;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import timber.log.Timber;

public class HtspConnection extends Thread implements ServerConnectionInterface<HtspMessage>, ServerConnectionMessageInterface<HtspMessage, HtspMessage> {

    private final HtspConnectionData htspConnectionData;

    private volatile boolean isRunning;
    private final Lock lock;
    private SocketChannel socketChannel;
    private final ByteBuffer inputByteBuffer;
    private int seq;

    private final ServerConnectionStateListener connectionListener;
    private final Set<ServerMessageListener<HtspMessage>> messageListeners = new CopyOnWriteArraySet<>();
    private final SparseArray<ServerResponseListener<HtspMessage>> responseHandlers;
    private final LinkedList<HtspMessage> messageQueue;
    private volatile boolean isConnecting = false;
    private volatile boolean isAuthenticated = false;
    /**
     * Set when the connection is closed on purpose. A close during the handshake must not be
     * reported as a failed authentication, otherwise the user sees a misleading "Authentication
     * failed" message while switching servers.
     */
    private volatile boolean isClosing = false;
    /**
     * Monitor on which {@link #authenticate()} waits for the response. It is a field so that
     * {@link #closeConnection()} can release a waiting authenticator instead of leaving it to run
     * into the connection timeout.
     */
    private final Object authMonitor = new Object();
    private Selector selector;

    @Override
    public void addMessageListener(@NonNull ServerMessageListener<HtspMessage> listener) {
        messageListeners.add(listener);
    }

    @Override
    public void removeMessageListener(@NonNull ServerMessageListener<HtspMessage> listener) {
        messageListeners.remove(listener);
    }

    public HtspConnection(HtspConnectionData htspConnectionData,
                          @NonNull ServerConnectionStateListener connectionListener,
                          @Nullable ServerMessageListener<HtspMessage> messageListener) {
        Timber.d("Initializing HTSP connection thread");

        this.htspConnectionData = htspConnectionData;

        this.isRunning = false;
        this.lock = new ReentrantLock();
        this.inputByteBuffer = ByteBuffer.allocateDirect(2048 * 2048);
        this.inputByteBuffer.limit(4);
        this.responseHandlers = new SparseArray<>();
        this.messageQueue = new LinkedList<>();
        this.connectionListener = connectionListener;

        if (messageListener != null) {
            this.messageListeners.add(messageListener);
        }
    }

    // synchronized, non blocking connect
    @Override
    public void openConnection() {
        Timber.i("Opening HTSP Connection");
        connectionListener.onConnectionStateChange(new ConnectionStateResult.Connecting());

        if (isRunning) {
            return;
        }

        Timber.d("Connection to server is starting");
        isConnecting = true;
        isClosing = false;

        final Object signal = new Object();

        lock.lock();
        try {
            Timber.d("Opening socket to server");
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.socket().setKeepAlive(true);
            socketChannel.socket().setSoTimeout(htspConnectionData.getConnectionTimeout());
            socketChannel.register(selector, SelectionKey.OP_CONNECT, signal);

            Timber.d("Parsing url " + htspConnectionData.getServerUrl() + " to get required host and port information");
            Uri uri = Uri.parse(htspConnectionData.getServerUrl());

            Timber.d("Getting inet address from hostname");
            InetAddress address = InetAddress.getByName(uri.getHost());
            if (address instanceof Inet6Address) {
                Timber.d("Found ipv6 address, looking for ipv4 %s", address);
                InetAddress[] inetAddressArray = InetAddress.getAllByName(uri.getHost());
                for (InetAddress inetAddress : inetAddressArray) {
                    if (inetAddress instanceof Inet4Address) {
                        address = inetAddress;
                        Timber.d("Found ipv4 %s", address);
                        break;
                    }
                }
            } else {
                Timber.d("Found ipv4 address");
            }

            InetSocketAddress inetSocketAddress  = new InetSocketAddress(address, uri.getPort());
            if (!socketChannel.connect(inetSocketAddress)) {
                Timber.d("Socket did not yet finish connecting, calling finishConnect()");
                socketChannel.finishConnect();
            }

            Timber.d("HTSP Connection thread can be started");
            isRunning = true;
            start();

        } catch (UnknownHostException e) {
            Timber.w(e, "Could not open the HTSP connection, the host is unknown");
            connectionListener.onConnectionStateChange(new ConnectionStateResult.Failed(new ConnectionFailureReason.UnresolvedAddress()));

        } catch (ClosedByInterruptException e) {
            Timber.w(e, "Could not open the HTSP connection, the call was interrupted");
            connectionListener.onConnectionStateChange(new ConnectionStateResult.Failed(new ConnectionFailureReason.Interrupted()));

        } catch (UnresolvedAddressException e) {
            Timber.w(e, "Could not resolve the address of the HTSP server");
            connectionListener.onConnectionStateChange(new ConnectionStateResult.Failed(new ConnectionFailureReason.UnresolvedAddress()));

        } catch (UnsupportedAddressTypeException e) {
            Timber.w(e, "The address type of the HTSP server is not supported");
            connectionListener.onConnectionStateChange(new ConnectionStateResult.Failed(new ConnectionFailureReason.UnresolvedAddress()));

        } catch (IOException e) {
            Timber.w(e, "Could not open the socket channel to the HTSP server");
            connectionListener.onConnectionStateChange(new ConnectionStateResult.Failed(new ConnectionFailureReason.SocketException()));

        } finally {
            lock.unlock();
        }

        if (isRunning) {
            // A server that answers immediately completes the connect inside connect()/
            // finishConnect(), and the selector then never reports OP_CONNECT, so the monitor below
            // would never be notified and every connect would block for the whole connection timeout.
            // There is nothing to wait for when the socket is already connected.
            if (socketChannel.isConnected()) {
                Timber.d("Connection to server completed immediately");
            } else {
                synchronized (signal) {
                    try {
                        signal.wait(htspConnectionData.getConnectionTimeout());
                        if (socketChannel.isConnectionPending()) {
                            Timber.w("Timed out while waiting for the connection to the server");
                            connectionListener.onConnectionStateChange(new ConnectionStateResult.Failed(new ConnectionFailureReason.Other()));
                            closeConnection();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Timber.w(e, "Waiting for the pending connection was interrupted");
                    }
                }
            }
        }
        Timber.d("Opened HTSP Connection");
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    @Override
    public boolean isNotConnected() {
        return socketChannel == null
                || !socketChannel.isOpen()
                || !socketChannel.isConnected()
                || !isRunning;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    // synchronized, blocking auth
    @Override
    public void authenticate() {
        Timber.d("Starting authentication");

        if (isAuthenticated || !isRunning) {
            return;
        }

        isAuthenticated = false;

        final HtspMessage authMessage = new HtspMessage();
        authMessage.setMethod("authenticate");
        authMessage.put("username", htspConnectionData.getUsername());

        final ServerResponseListener<HtspMessage> authHandler = response -> {
            isAuthenticated = response.getInteger("noaccess", 0) != 1;
            Timber.d("Authentication was successful: %s", isAuthenticated);
            if (!isAuthenticated) {
                connectionListener.onAuthenticationStateChange(new AuthenticationStateResult.Failed(new AuthenticationFailureReason.BadCredentials()));
            } else {
                AuthenticationRights rights = parseAuthenticationRights(response);
                Timber.d("Granted rights: admin %s, streaming %s, dvr %s", rights.isAdmin(), rights.isStreaming(), rights.isDvr());
                connectionListener.onAuthenticationStateChange(new AuthenticationStateResult.Authenticated("", rights));
            }
            synchronized (authMonitor) {
                authMonitor.notify();
            }
            Timber.d("Connection to server is complete");
            isConnecting = false;
        };

        Timber.d("Sending initial message to server");
        HtspMessage helloMessage = new HtspMessage();
        helloMessage.setMethod("hello");
        helloMessage.put("clientname", "TVHClient");
        helloMessage.put("clientversion", htspConnectionData.getVersionName() + "-" + htspConnectionData.getVersionCode());
        helloMessage.put("htspversion", HtspMessage.HTSP_VERSION);
        helloMessage.put("username", htspConnectionData.getUsername());

        sendMessage(helloMessage, response -> {

            response.setMethod("serverStatus");
            for (ServerMessageListener<HtspMessage> listener : messageListeners) {
                listener.onMessage(response, response.getMethod());
            }

            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA1");
                md.update(htspConnectionData.getPassword().getBytes());
                md.update(response.getByteArray("challenge"));

                Timber.d("Sending authentication message");
                authMessage.put("digest", md.digest());
                sendMessage(authMessage, authHandler);
            } catch (NoSuchAlgorithmException e) {
                Timber.e(e, "Could not send the authentication message, no SHA1 algorithm available");
            }
        });

        synchronized (authMonitor) {
            try {
                authMonitor.wait(htspConnectionData.getConnectionTimeout());
                // A close that was requested on purpose is not a failed authentication: the socket
                // was closed by the application, so the caller must not be told the handshake timed
                // out. Without this check a rebind or a service teardown reports a bogus failure.
                if (!isAuthenticated && !isClosing) {
                    Timber.w("Timed out while waiting for the authentication response");
                    connectionListener.onAuthenticationStateChange(new AuthenticationStateResult.Failed(new AuthenticationFailureReason.Other()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Timber.w(e, "Waiting for the authentication response was interrupted");
            }
        }
    }

    @Override
    public void sendMessage(@NonNull HtspMessage message) {
        sendMessage(message, null);
    }

    @Override
    public void sendMessage(@NonNull HtspMessage message, @Nullable ServerResponseListener<HtspMessage> listener) {
        if (isNotConnected()) {
            Timber.w("Not sending the message %s, not connected to the server", message.getMethod());

            HtspMessage msg = new HtspMessage();
            msg.put("success", 0);
            msg.put("error", "Could not send message, not connected to server");
            if (listener != null) {
                listener.handleResponse(msg);
            }

            return;
        }
        lock.lock();
        try {
            seq++;
            message.put("seq", seq);
            responseHandlers.put(seq, listener);
            socketChannel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
            messageQueue.add(message);
            selector.wakeup();

        } catch (Exception e) {
            Timber.w(e, "Could not send the message %s", message.getMethod());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeConnection() {
        Timber.d("Closing HTSP connection");
        isClosing = true;
        lock.lock();
        try {
            responseHandlers.clear();
            messageQueue.clear();
            isAuthenticated = false;
            isConnecting = false;
            isRunning = false;
            socketChannel.close();

        } catch (ClosedChannelException e) {
            Timber.w(e, "Could not close the HTSP connection, the channel is already closed");
        } catch (NullPointerException e) {
            Timber.w(e, "Could not close the HTSP connection, the socket channel is null");
        } catch (IllegalSelectorException e) {
            Timber.w(e, "Could not close the HTSP connection, the selector is illegal");
        } catch (CancelledKeyException e) {
            Timber.w(e, "Could not close the HTSP connection, the selection key was cancelled");
        } catch (IOException e) {
            Timber.w(e, "Could not close the socket channel of the HTSP connection");
        } finally {
            lock.unlock();
        }
        // Release a thread that is waiting for the authenticate response. Without this the waiter
        // would block until the connection timeout and then report a failed authentication even
        // though the connection was closed on purpose.
        synchronized (authMonitor) {
            authMonitor.notify();
        }
        Timber.d("HTSP connection closed");
    }

    @Override
    public void run() {
        Timber.d("Starting HTSP connection thread");
        connectionListener.onConnectionStateChange(new ConnectionStateResult.Connected());

        while (isRunning) {
            try {
                selector.select(5000);
            } catch (IOException e) {
                Timber.w(e, "Could not select from the socket channel, an I/O error occurred");
                connectionListener.onConnectionStateChange(new ConnectionStateResult.Failed(new ConnectionFailureReason.Other()));
                isRunning = false;
            } catch (ClosedSelectorException e) {
                Timber.w(e, "Could not select from the socket channel, the selector is already closed");
                connectionListener.onConnectionStateChange(new ConnectionStateResult.Failed(new ConnectionFailureReason.Other()));
                isRunning = false;
            }

            lock.lock();
            try {
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey selKey = it.next();
                    it.remove();
                    processTcpSelectionKey(selKey);
                }
                int ops = SelectionKey.OP_READ;
                if (!messageQueue.isEmpty()) {
                    ops |= SelectionKey.OP_WRITE;
                }
                if (socketChannel.isOpen()) {
                    socketChannel.register(selector, ops);
                }

            } catch (NullPointerException e) {
                Timber.w(e, "Could not register the selector, the socket channel is null");
                isRunning = false;
            } catch (IllegalSelectorException e) {
                Timber.w(e, "Could not register the selector, the selector is illegal");
                isRunning = false;
            } catch (ClosedChannelException e) {
                Timber.w(e, "Could not register the selector, the channel is already closed");
                isRunning = false;
            } catch (ClosedSelectorException e) {
                Timber.w(e, "Could not register the selector, the selector is already closed");
                isRunning = false;
            } catch (CancelledKeyException e) {
                Timber.w(e, "Could not register the selector, the selection key was cancelled");
                isRunning = false;
            } catch (NotYetConnectedException e) {
                Timber.w(e, "Could not register the selector, the channel is not connected yet");
                isRunning = false;
            } catch (IOException e) {
                Timber.w(e, "Could not process the tcp selection key");
                isRunning = false;
            } finally {
                lock.unlock();
            }
        }

        closeConnection();
        Timber.d("HTSP connection thread stopped");
    }

    private void processTcpSelectionKey(SelectionKey selKey)
            throws IOException, NullPointerException, IllegalSelectorException, CancelledKeyException, NotYetConnectedException {

        if (selKey.isConnectable() && selKey.isValid()) {
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            sChannel.finishConnect();
            final Object signal = selKey.attachment();
            synchronized (signal) {
                signal.notify();
            }
            sChannel.register(selector, SelectionKey.OP_READ);
        }
        if (selKey.isReadable() && selKey.isValid()) {
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            int len = sChannel.read(inputByteBuffer);
            if (len < 0) {
                connectionListener.onConnectionStateChange(new ConnectionStateResult.Failed(new ConnectionFailureReason.Other()));
                Timber.w("Could not read data from the server, the server closed the connection");
                throw new IOException();
            }

            HtspMessage msg = HtspMessage.parse(inputByteBuffer);
            if (msg != null) {
                handleMessage(msg);
            }
        }
        if (selKey.isWritable() && selKey.isValid()) {
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            HtspMessage msg = messageQueue.poll();
            if (msg != null) {
                msg.transmit(sChannel);
            }
        }
    }

    /**
     * Reads the rights of the authenticated account from the reply to the authenticate
     * request. The server only sends them when the negotiated HTSP version is newer than 25,
     * so older servers are treated as if everything was granted.
     */
    private static AuthenticationRights parseAuthenticationRights(HtspMessage response) {
        if (!response.containsKey("streaming")) {
            return new AuthenticationRights(false, true, true, false, false, 0, 0, 0, 0, "");
        }
        return new AuthenticationRights(
                response.getInteger("admin", 0) == 1,
                response.getInteger("streaming", 0) == 1,
                response.getInteger("dvr", 0) == 1,
                response.getInteger("faileddvr", 0) == 1,
                response.getInteger("anonymous", 0) == 1,
                response.getInteger("limitall", 0),
                response.getInteger("limitdvr", 0),
                response.getInteger("limitstreaming", 0),
                response.getInteger("uilevel", 0),
                response.getString("uilanguage", ""));
    }

    private void handleMessage(HtspMessage msg) {
        if (msg.containsKey("seq")) {
            int respSeq = msg.getInteger("seq");
            ServerResponseListener<HtspMessage> handler = responseHandlers.get(respSeq);
            responseHandlers.remove(respSeq);

            if (handler != null) {
                synchronized (handler) {
                    handler.handleResponse(msg);
                }
                return;
            }
        }

        for (ServerMessageListener<HtspMessage> listener : messageListeners) {
            listener.onMessage(msg, msg.getMethod());
        }
    }
}
