package org.tvheadend.tvhclient.data.service_old;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.SparseArray;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.HtspConnection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import timber.log.Timber;

public class HTSConnection extends Thread {

    private final AppRepository appRepository;
    private final Connection connection;
    private volatile boolean isRunning;
    private final Lock lock;
    private SocketChannel socketChannel;
    private final ByteBuffer inputByteBuffer;
    private int seq;

    private final HTSConnectionListener listener;
    private final SparseArray<HTSResponseHandler> responseHandlers;
    private final LinkedList<HTSMessage> messageQueue;
    private boolean isAuthenticated = false;
    private Selector selector;
    private int connectionTimeout;

    HTSConnection(Context context, AppRepository appRepository, Connection connection, HTSConnectionListener listener) {
        Timber.d("Initializing HTSP connection thread");

        // Disable the use of IPv6
        System.setProperty("java.net.preferIPv6Addresses", "false");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        connectionTimeout = Integer.valueOf(sharedPreferences.getString("connection_timeout", context.getResources().getString(R.string.pref_default_connection_timeout))) * 1000;

        this.appRepository = appRepository;
        this.connection = connection;
        this.isRunning = false;
        this.lock = new ReentrantLock();
        this.inputByteBuffer = ByteBuffer.allocateDirect(2048 * 2048);
        this.inputByteBuffer.limit(4);
        this.responseHandlers = new SparseArray<>();
        this.messageQueue = new LinkedList<>();
        this.listener = listener;
    }

    // synchronized, non blocking connect
    void openConnection(String hostname, int port) {
        Timber.i("Opening HTSP Connection");

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

            Timber.d("Connecting via socket to " + connection.getHostname() + ":" + connection.getPort());
            socketChannel.connect(new InetSocketAddress(hostname, port));
            isRunning = true;
            start();

        } catch (ClosedByInterruptException e) {
            Timber.e("Failed to open HTSP connection, interrupted");
            listener.onConnectionStateChange(HtspConnection.State.FAILED_INTERRUPTED);

        } catch (UnresolvedAddressException e) {
            Timber.e("Failed to resolve HTSP server address:", e);
            listener.onConnectionStateChange(HtspConnection.State.FAILED_UNRESOLVED_ADDRESS);

        } catch (IOException e) {
            Timber.e("Caught IOException while opening SocketChannel:", e);
            listener.onConnectionStateChange(HtspConnection.State.FAILED_EXCEPTION_OPENING_SOCKET);

        } finally {
            lock.unlock();
        }

        if (isRunning) {
            synchronized (signal) {
                try {
                    signal.wait(connectionTimeout);
                    if (socketChannel.isConnectionPending()) {
                        Timber.d("Timeout while waiting to connect to server");
                        listener.onConnectionStateChange(HtspConnection.State.FAILED);
                        closeConnection();
                    }
                } catch (InterruptedException e) {
                    Timber.d("Waiting for pending connection was interrupted. ", e);
                }
            }
        }
        Timber.d("Opened HTSP Connection");
    }

    public boolean isConnected() {
        return socketChannel != null
                && socketChannel.isOpen()
                && socketChannel.isConnected()
                && isRunning;
    }

    // synchronized, blocking auth
    void authenticate(String username, final String password) {
        Timber.d("Starting authentication");

        if (isAuthenticated || !isRunning) {
            return;
        }

        isAuthenticated = false;

        final HTSMessage authMessage = new HTSMessage();
        authMessage.setMethod("authenticate");
        authMessage.putField("username", username);

        final HTSResponseHandler authHandler = response -> {
            isAuthenticated = response.getInteger("noaccess", 0) != 1;
            Timber.d("Authentication was successful: " + isAuthenticated);
            if (!isAuthenticated) {
                listener.onAuthenticationStateChange(Authenticator.State.FAILED_BAD_CREDENTIALS);
            } else {
                listener.onAuthenticationStateChange(Authenticator.State.AUTHENTICATED);
            }
            synchronized (authMessage) {
                authMessage.notify();
            }
        };

        Timber.d("Sending initial message to server");
        HTSMessage helloMessage = new HTSMessage();
        helloMessage.setMethod("hello");
        helloMessage.putField("clientname", "TVHClient");
        helloMessage.putField("clientversion", (BuildConfig.VERSION_NAME + "-" + BuildConfig.VERSION_CODE));
        helloMessage.putField("htspversion", HTSMessage.HTSP_VERSION);
        helloMessage.putField("username", username);

        sendMessage(helloMessage, response -> {

            ServerStatus serverStatus = appRepository.getServerStatusData().getActiveItem();
            ServerStatus updatedServerStatus = HTSUtils.convertMessageToServerStatusModel(serverStatus, response);
            updatedServerStatus.setConnectionId(connection.getId());
            updatedServerStatus.setConnectionName(connection.getName());
            Timber.d("Received initial response from server " + updatedServerStatus.getServerName() + ", api version: " + updatedServerStatus.getHtspVersion());

            appRepository.getServerStatusData().updateItem(updatedServerStatus);

            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA1");
                md.update(password.getBytes());
                md.update(response.getByteArray("challenge"));

                Timber.d("Sending authentication message");
                authMessage.putField("digest", md.digest());
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
                    listener.onAuthenticationStateChange(Authenticator.State.FAILED);
                }
            } catch (InterruptedException e) {
                Timber.d("Waiting for authentication message was interrupted. ", e);
            }
        }
    }

    public void sendMessage(HTSMessage message, HTSResponseHandler listener) {
        if (!isConnected()) {
            return;
        }
        lock.lock();
        try {
            seq++;
            message.putField("seq", seq);
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

    void closeConnection() {
        Timber.d("Closing HTSP connection");
        lock.lock();
        try {
            responseHandlers.clear();
            messageQueue.clear();
            isAuthenticated = false;
            isRunning = false;
            socketChannel.register(selector, 0);
            socketChannel.close();
        } catch (IOException e) {
            Timber.w("Failed to close socket channel: ", e);
        } finally {
            lock.unlock();
        }
        Timber.d("HTSP connection closed");
    }

    @Override
    public void run() {
        Timber.d("Starting HTSP connection thread");

        while (isRunning) {
            try {
                selector.select(5000);
            } catch (IOException e) {
                Timber.e("Failed to select from socket channel, I/O error occurred", e);
                listener.onConnectionStateChange(HtspConnection.State.FAILED);
                isRunning = false;
            } catch (ClosedSelectorException cse) {
                Timber.e("Failed to select from socket channel, selector is already closed", cse);
                listener.onConnectionStateChange(HtspConnection.State.FAILED);
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

            } catch (Exception ex) {
                isRunning = false;

            } finally {
                lock.unlock();
            }
        }
        closeConnection();
        Timber.d("HTSP connection thread stopped");
    }

    private void processTcpSelectionKey(SelectionKey selKey) throws IOException {

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
                listener.onConnectionStateChange(HtspConnection.State.FAILED);

                Timber.d("processTcpSelectionKey: Could not read data from server");
                throw new IOException();
            }

            HTSMessage msg = HTSMessage.parse(inputByteBuffer);
            if (msg != null) {
                handleMessage(msg);
            }
        }
        if (selKey.isWritable() && selKey.isValid()) {
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            HTSMessage msg = messageQueue.poll();
            if (msg != null) {
                msg.transmit(sChannel);
            }
        }
    }

    private void handleMessage(HTSMessage msg) {
        if (msg.containsField("seq")) {
            int respSeq = msg.getInteger("seq");
            HTSResponseHandler handler = responseHandlers.get(respSeq);
            responseHandlers.remove(respSeq);

            if (handler != null) {
                synchronized (handler) {
                    handler.handleResponse(msg);
                }
                return;
            }
        }
        listener.onMessage(msg);
    }
}
