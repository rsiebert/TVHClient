package org.tvheadend.htsp;

import android.net.Uri;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import timber.log.Timber;

public class HtspConnection extends Thread implements HtspConnectionInterface {

    private final String username;
    private final String password;
    private final String url;

    private volatile boolean isRunning;
    private final Lock lock;
    private SocketChannel socketChannel;
    private final ByteBuffer inputByteBuffer;
    private int seq;

    private final HtspConnectionStateListener connectionListener;
    private final Set<HtspMessageListener> messageListeners = new HashSet<>();
    private final SparseArray<HtspResponseListener> responseHandlers;
    private final LinkedList<HtspMessage> messageQueue;
    private boolean isConnecting = false;
    private boolean isAuthenticated = false;
    private Selector selector;
    private final int connectionTimeout;

    @Override
    public void addMessageListener(@NonNull HtspMessageListener listener) {
        messageListeners.add(listener);
    }

    @Override
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

    public HtspConnection(@Nullable String username,
                          @Nullable String password,
                          @Nullable String url,
                          int connectionTimeout,
                          @NonNull HtspConnectionStateListener connectionListener,
                          @Nullable HtspMessageListener messageListener) {
        Timber.d("Initializing HTSP connection thread");

        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
        this.url = url != null ? url : "";
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
    @Override
    public void openConnection() {
        Timber.i("Opening HTSP Connection");
        connectionListener.onConnectionStateChange(ConnectionState.CONNECTING);

        if (isRunning) {
            return;
        }

        Timber.d("Connection to server is starting");
        isConnecting = true;

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

            Timber.d("Parsing url " + url + " to get required host and port information");
            Uri uri = Uri.parse(url);
            InetSocketAddress inetSocketAddress = new InetSocketAddress(uri.getHost(), uri.getPort());

            InetAddress inetAddress = inetSocketAddress.getAddress();
            if (inetAddress instanceof Inet4Address) {
                Timber.d("Connecting via socket to ipv4 address '" + inetAddress.getHostName() + "' and port '" + inetSocketAddress.getPort() + "'");

            } else if (inetAddress instanceof Inet6Address) {
                Inet6Address inet6Address = (Inet6Address) inetAddress;
                if (inet6Address.getScopeId() != 0) {
                    Timber.d("Connecting via socket to ipv6 address '" + inetAddress.getHostName() + "' and port '" + inetSocketAddress.getPort() + "'");
                    inetSocketAddress = new InetSocketAddress(InetAddress.getByAddress(inet6Address.getAddress()), inetSocketAddress.getPort());
                } else {
                    Timber.d("Scope id of ipv6 address '" + inetAddress.getHostName() + "', is 0");
                    inetSocketAddress = null;
                }
            }

            if (!socketChannel.connect(inetSocketAddress)) {
                Timber.d("Socket did not yet finish connecting, calling finishConnect()");
                socketChannel.finishConnect();
            }

            Timber.d("HTSP Connection thread can be started");
            isRunning = true;
            start();

        } catch (UnknownHostException e) {
            Timber.d(e, "Unknown host exception while opening HTSP connection");
            connectionListener.onConnectionStateChange(ConnectionState.FAILED_UNRESOLVED_ADDRESS);

        } catch (ClosedByInterruptException e) {
            Timber.d(e, "Failed to open HTSP connection, interrupted");
            connectionListener.onConnectionStateChange(ConnectionState.FAILED_INTERRUPTED);

        } catch (UnresolvedAddressException e) {
            Timber.d(e, "Failed to resolve HTSP server address");
            connectionListener.onConnectionStateChange(ConnectionState.FAILED_UNRESOLVED_ADDRESS);

        } catch (UnsupportedAddressTypeException e) {
            Timber.d(e, "Type of HTSP server address is not supported");
            connectionListener.onConnectionStateChange(ConnectionState.FAILED_UNRESOLVED_ADDRESS);

        } catch (IOException e) {
            Timber.d(e, "Caught IOException while opening SocketChannel");
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
                    Timber.d(e, "Waiting for pending connection was interrupted.");
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
        authMessage.put("username", username);

        final HtspResponseListener authHandler = response -> {
            isAuthenticated = response.getInteger("noaccess", 0) != 1;
            Timber.d("Authentication was successful: %s", isAuthenticated);
            if (!isAuthenticated) {
                connectionListener.onAuthenticationStateChange(AuthenticationState.FAILED_BAD_CREDENTIALS);
            } else {
                connectionListener.onAuthenticationStateChange(AuthenticationState.AUTHENTICATED);
            }
            synchronized (authMessage) {
                authMessage.notify();
            }
            Timber.d("Connection to server is complete");
            isConnecting = false;
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
                Timber.d(e, "Could not sent authentication message.");
            }
        });

        synchronized (authMessage) {
            try {
                authMessage.wait(connectionTimeout);
                if (!isAuthenticated) {
                    Timber.d("Timeout while waiting for authentication response");
                    connectionListener.onAuthenticationStateChange(AuthenticationState.FAILED);
                }
            } catch (InterruptedException e) {
                Timber.d(e, "Waiting for authentication message was interrupted.");
            }
        }
    }

    @Override
    public void sendMessage(@NonNull HtspMessage message) {
        sendMessage(message, null);
    }

    @Override
    public void sendMessage(@NonNull HtspMessage message, @Nullable HtspResponseListener listener) {
        if (isNotConnected()) {
            Timber.d("Not sending message, not connected to server");

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
            Timber.d(e, "Could not send message.");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeConnection() {
        Timber.d("Closing HTSP connection");
        lock.lock();
        try {
            responseHandlers.clear();
            messageQueue.clear();
            isAuthenticated = false;
            isConnecting = false;
            isRunning = false;

            if (socketChannel.isOpen()) {
                socketChannel.register(selector, 0);
                socketChannel.close();
            }

        } catch (ClosedChannelException e) {
            Timber.d(e, "Failed to register selector with socket channel, closed channel exception");
        } catch (NullPointerException e) {
            Timber.d(e, "Failed to register selector with socket channel or closing socket channel, socket channel is null");
        } catch (IllegalSelectorException e) {
            Timber.d(e, "Failed to register selector with socket channel, illegal selector");
        } catch (CancelledKeyException e) {
            Timber.d(e, "Failed to register selector with socket channel, cancelled key");
        } catch (IOException e) {
            Timber.d(e, "Failed to close socket channel");
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
                Timber.d(e, "Failed to select from socket channel, I/O error occurred");
                connectionListener.onConnectionStateChange(ConnectionState.FAILED);
                isRunning = false;
            } catch (ClosedSelectorException e) {
                Timber.d(e, "Failed to select from socket channel, selector is already closed");
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
                if (socketChannel.isOpen()) {
                    socketChannel.register(selector, ops);
                }

            } catch (NullPointerException e) {
                Timber.d(e, "Failed to register selector with socket channel, socket channel is null");
                isRunning = false;
            } catch (IllegalSelectorException e) {
                Timber.d(e, "Failed to register selector with socket channel, illegal selector");
                isRunning = false;
            } catch (ClosedChannelException e) {
                Timber.d(e, "Failed to register selector with socket channel, channel is already closed");
                isRunning = false;
            } catch (ClosedSelectorException e) {
                Timber.d(e, "Failed to register selector with socket channel, selector is already closed");
                isRunning = false;
            } catch (CancelledKeyException e) {
                Timber.d(e, "Invalid selection key was used while processing tcp selection key");
                isRunning = false;
            } catch (NotYetConnectedException e) {
                Timber.d(e, "Not yet connected while while processing tcp selection key");
                isRunning = false;
            } catch (IOException e) {
                Timber.d(e, "Exception while processing tcp selection key");
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
                connectionListener.onConnectionStateChange(ConnectionState.FAILED);
                Timber.d("Could not read data from server");
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
