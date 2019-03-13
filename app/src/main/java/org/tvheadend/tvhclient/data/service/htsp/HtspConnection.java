package org.tvheadend.tvhclient.data.service.htsp;

import android.util.SparseArray;

import org.tvheadend.tvhclient.BuildConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import timber.log.Timber;

// TODO provide an interface to access the connection from outside this package

public class HtspConnection extends Thread {

    private final String username;
    private final String password;
    private final String hostname;
    private final int port;

    private volatile boolean isRunning;
    private final Lock lock;
    private SocketChannel socketChannel;
    private final ByteBuffer inputByteBuffer;
    private int seq;

    private HtspConnectionStateListener connectionListener;
    private Set<HtspMessageListener> messageListeners = new HashSet<>();
    private final SparseArray<HtspResponseListener> responseHandlers;
    private final LinkedList<HtspMessage> messageQueue;
    private boolean isAuthenticated = false;
    private Selector selector;
    private int connectionTimeout;

    public void addMessageListener(@NonNull HtspMessageListener listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(@NonNull HtspMessageListener listener) {
        messageListeners.remove(listener);
    }

    public enum AuthenticationState {
        IDLE,
        AUTHENTICATING,
        AUTHENTICATED,
        FAILED_BAD_CREDENTIALS,
        FAILED
    }

    public enum ConnectionState {
        IDLE,
        CLOSED,
        CONNECTING,
        CONNECTED,
        CLOSING,
        FAILED,
        FAILED_INTERRUPTED,
        FAILED_UNRESOLVED_ADDRESS,
        FAILED_CONNECTING_TO_SERVER,
        FAILED_EXCEPTION_OPENING_SOCKET
    }

    public HtspConnection(@Nullable String username, @Nullable String password,
                          @Nullable String hostname, int port,
                          int connectionTimeout,
                          @NonNull HtspConnectionStateListener connectionListener,
                          @Nullable HtspMessageListener messageListener) {
        Timber.d("Initializing HTSP connection thread");

        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
        this.hostname = hostname != null ? hostname : "";
        this.port = port;
        this.connectionTimeout = connectionTimeout;

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
    public void openConnection() {
        Timber.i("Opening HTSP Connection");
        connectionListener.onConnectionStateChange(ConnectionState.CONNECTING);

        if (isRunning) {
            return;
        }

        final Object signal = new Object();

        lock.lock();
        try {
            Timber.d("Opening socket to server");
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.socket().setKeepAlive(true);
            socketChannel.socket().setSoTimeout(connectionTimeout);
            socketChannel.register(selector, SelectionKey.OP_CONNECT, signal);

            Timber.d("Connecting via socket to " + hostname + ":" + port);
            socketChannel.connect(new InetSocketAddress(hostname, port));

            Timber.d("HTSP Connection thread can be started");
            isRunning = true;
            start();

        } catch (ClosedByInterruptException e) {
            Timber.e("Failed to open HTSP connection, interrupted");
            connectionListener.onConnectionStateChange(ConnectionState.FAILED_INTERRUPTED);

        } catch (UnresolvedAddressException e) {
            Timber.e("Failed to resolve HTSP server address:", e);
            connectionListener.onConnectionStateChange(ConnectionState.FAILED_UNRESOLVED_ADDRESS);

        } catch (IOException e) {
            Timber.e("Caught IOException while opening SocketChannel:", e);
            connectionListener.onConnectionStateChange(ConnectionState.FAILED_EXCEPTION_OPENING_SOCKET);

        } finally {
            lock.unlock();
        }

        if (isRunning) {
            synchronized (signal) {
                try {
                    signal.wait(connectionTimeout);
                    if (socketChannel.isConnectionPending()) {
                        Timber.d("Timeout while waiting to connect to server");
                        connectionListener.onConnectionStateChange(ConnectionState.FAILED);
                        closeConnection();
                    }
                } catch (InterruptedException e) {
                    Timber.d("Waiting for pending connection was interrupted. ", e);
                }
            }
        }
        Timber.d("Opened HTSP Connection");
    }

    public boolean isNotConnected() {
        return socketChannel == null
                || !socketChannel.isOpen()
                || !socketChannel.isConnected()
                || !isRunning;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    // synchronized, blocking auth
    public void authenticate() {
        Timber.d("Starting authentication");

        if (isAuthenticated || !isRunning) {
            return;
        }

        isAuthenticated = false;

        final HtspMessage authMessage = new HtspMessage();
        authMessage.setMethod("authenticate");
        authMessage.put("username", username);

        final HtspResponseListener authHandler = response -> {
            isAuthenticated = response.getInteger("noaccess", 0) != 1;
            Timber.d("Authentication was successful: " + isAuthenticated);
            if (!isAuthenticated) {
                connectionListener.onAuthenticationStateChange(AuthenticationState.FAILED_BAD_CREDENTIALS);
            } else {
                connectionListener.onAuthenticationStateChange(AuthenticationState.AUTHENTICATED);
            }
            synchronized (authMessage) {
                authMessage.notify();
            }
        };

        Timber.d("Sending initial message to server");
        HtspMessage helloMessage = new HtspMessage();
        helloMessage.setMethod("hello");
        helloMessage.put("clientname", "TVHClient");
        helloMessage.put("clientversion", (BuildConfig.VERSION_NAME + "-" + BuildConfig.VERSION_CODE));
        helloMessage.put("htspversion", HtspMessage.HTSP_VERSION);
        helloMessage.put("username", username);

        sendMessage(helloMessage, response -> {

            response.setMethod("serverStatus");
            for (HtspMessageListener listener : messageListeners) {
                listener.onMessage(response);
            }

            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA1");
                md.update(password.getBytes());
                md.update(response.getByteArray("challenge"));

                Timber.d("Sending authentication message");
                authMessage.put("digest", md.digest());
                sendMessage(authMessage, authHandler);
            } catch (NoSuchAlgorithmException e) {
                Timber.d("Could not sent authentication message. ", e);
            }
        });

        synchronized (authMessage) {
            try {
                authMessage.wait(5000);
                if (!isAuthenticated) {
                    Timber.d("Timeout while waiting for authentication response");
                    connectionListener.onAuthenticationStateChange(AuthenticationState.FAILED);
                }
            } catch (InterruptedException e) {
                Timber.d("Waiting for authentication message was interrupted. ", e);
            }
        }
    }

    public void sendMessage(@NonNull HtspMessage message) {
        sendMessage(message, null);
    }

    public void sendMessage(@NonNull HtspMessage message, @Nullable HtspResponseListener listener) {
        if (isNotConnected()) {
            Timber.d("Not sending message, not connected to server");
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
            Timber.d("Could not send message. ", e);
        } finally {
            lock.unlock();
        }
    }

    public void closeConnection() {
        Timber.d("Closing HTSP connection");
        lock.lock();
        try {
            responseHandlers.clear();
            messageQueue.clear();
            isAuthenticated = false;
            isRunning = false;
            socketChannel.register(selector, 0);
            socketChannel.close();

        } catch (ClosedChannelException e) {
            Timber.d("Failed to register selector with socket channel, closed channel exception", e);
        } catch (NullPointerException e) {
            Timber.d("Failed to register selector with socket channel or closing socket channel, socket channel is null", e);
        } catch (IllegalSelectorException e) {
            Timber.d("Failed to register selector with socket channel, illegal selector", e);
        } catch (CancelledKeyException e) {
            Timber.d("Failed to register selector with socket channel, cancelled key", e);
        } catch (IOException e) {
            Timber.d("Failed to close socket channel", e);
        } finally {
            lock.unlock();
        }
        Timber.d("HTSP connection closed");
    }

    @Override
    public void run() {
        Timber.d("Starting HTSP connection thread");
        connectionListener.onConnectionStateChange(ConnectionState.CONNECTED);

        while (isRunning) {
            try {
                selector.select(5000);
            } catch (IOException e) {
                Timber.e("Failed to select from socket channel, I/O error occurred", e);
                connectionListener.onConnectionStateChange(ConnectionState.FAILED);
                isRunning = false;
            } catch (ClosedSelectorException cse) {
                Timber.e("Failed to select from socket channel, selector is already closed", cse);
                connectionListener.onConnectionStateChange(ConnectionState.FAILED);
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
                socketChannel.register(selector, ops);

            } catch (NullPointerException npe) {
                Timber.d("Failed to register selector with socket channel, socket channel is null", npe);
                isRunning = false;
            } catch (IllegalSelectorException ise) {
                Timber.d("Failed to register selector with socket channel, illegal selector", ise);
                isRunning = false;
            } catch (ClosedChannelException e) {
                Timber.e("Failed to register selector with socket channel, channel is already closed", e);
                isRunning = false;
            } catch (ClosedSelectorException e) {
                Timber.e("Failed to register selector with socket channel, selector is already closed", e);
                isRunning = false;
            } catch (CancelledKeyException e) {
                Timber.e("Invalid selection key was used while processing tcp selection key");
                isRunning = false;
            } catch (IOException e) {
                Timber.e("Exception while processing tcp selection key");
                isRunning = false;
            } finally {
                lock.unlock();
            }
        }

        closeConnection();
        Timber.d("HTSP connection thread stopped");
    }

    private void processTcpSelectionKey(SelectionKey selKey)
            throws IOException, NullPointerException, IllegalSelectorException, CancelledKeyException {

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
                connectionListener.onConnectionStateChange(ConnectionState.FAILED);
                Timber.e("Could not read data from server");
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

    private void handleMessage(HtspMessage msg) {
        if (msg.containsKey("seq")) {
            int respSeq = msg.getInteger("seq");
            HtspResponseListener handler = responseHandlers.get(respSeq);
            responseHandlers.remove(respSeq);

            if (handler != null) {
                synchronized (handler) {
                    handler.handleResponse(msg);
                }
                return;
            }
        }

        for (HtspMessageListener listener : messageListeners) {
            listener.onMessage(msg);
        }
    }
}
