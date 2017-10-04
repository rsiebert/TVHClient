package org.tvheadend.tvhclient.htsp;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.SparseArray;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.interfaces.HTSConnectionListener;
import org.tvheadend.tvhclient.interfaces.HTSResponseHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HTSConnection extends Thread {

    private static final String TAG = HTSConnection.class.getSimpleName();
    private final Logger logger;
    private volatile boolean running;
    private final Lock lock;
    private SocketChannel socketChannel;
    private final ByteBuffer inBuf;
    private int seq;
    private final String clientName;
    private final String clientVersion;
    private int protocolVersion;
    private String serverName;
    private String serverVersion;
    private String webRoot;

    private final HTSConnectionListener listener;
    private final SparseArray<HTSResponseHandler> responseHandlers;
    private final SparseArray<String> responseHandlerMethods;
    private final LinkedList<HTSMessage> messageQueue;
    private boolean auth = false;
    private Selector selector;
    private final TVHClientApplication app;
    private int connectionTimeout = 5000;

    public HTSConnection(TVHClientApplication app, HTSConnectionListener listener, String clientName, String clientVersion) {
        this.app = app;
        logger = Logger.getInstance();

        // Disable the use of IPv6
        System.setProperty("java.net.preferIPv6Addresses", "false");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        connectionTimeout = Integer.parseInt(prefs.getString("connectionTimeout", "5")) * 1000;
        int bufferSize = Integer.parseInt(prefs.getString("bufferSize", "0"));

        running = false;
        lock = new ReentrantLock();
        inBuf = ByteBuffer.allocateDirect(2048 * 2048 * (bufferSize + 1));
        inBuf.limit(4);
        responseHandlers = new SparseArray<>();
        responseHandlerMethods = new SparseArray<>();
        messageQueue = new LinkedList<>();

        this.listener = listener;
        this.clientName = clientName;
        this.clientVersion = clientVersion;
    }

    public void setRunning(boolean b) {
        try {
            lock.lock();
            running = false;
        } finally {
            lock.unlock();
        }
    }

    // synchronized, non blocking connect
    public void open(String hostname, int port, boolean connected) {
        logger.log(TAG, "open() called with: hostname = [" + hostname + "], port = [" + port + "], connected = [" + connected + "]");

        if (running) {
            return;
        }
        if (!connected) {
            listener.onError(Constants.ACTION_CONNECTION_STATE_NO_NETWORK);
            return;
        }
        if (hostname == null) {
            listener.onError(Constants.ACTION_CONNECTION_STATE_NO_CONNECTION);
            return;
        }

        final Object signal = new Object();

        lock.lock();
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.socket().setKeepAlive(true);
            socketChannel.socket().setSoTimeout(connectionTimeout);
            socketChannel.register(selector, SelectionKey.OP_CONNECT, signal);
            socketChannel.connect(new InetSocketAddress(hostname, port));
            running = true;
            start();
        } catch (Exception e) {
            logger.log(TAG, "open: Could not open connection. " + e.getLocalizedMessage());
            listener.onError(Constants.ACTION_CONNECTION_STATE_REFUSED);
        } finally {
            lock.unlock();
        }

        if (running) {
            synchronized (signal) {
                try {
                    signal.wait(connectionTimeout);
                    if (socketChannel.isConnectionPending()) {
                        logger.log(TAG, "open: Timeout waiting for pending connection to server");
                        listener.onError(Constants.ACTION_CONNECTION_STATE_TIMEOUT);
                        close();
                    }
                } catch (InterruptedException ex) {
                    logger.log(TAG, "open: Waiting for pending connection was interrupted. " + ex.getLocalizedMessage());
                }
            }
        }
    }

    public boolean isConnected() {
        return socketChannel != null
                && socketChannel.isOpen()
                && socketChannel.isConnected()
                && running;
    }

    // synchronized, blocking auth
    public void authenticate(String username, final String password) {
        logger.log(TAG, "authenticate() called with: username = [" + username + "], password = [secret]");

        if (auth || !running) {
            return;
        }

        auth = false;

        final HTSMessage authMessage = new HTSMessage();
        authMessage.setMethod("enableAsyncMetadata");
        authMessage.putField("username", username);

        final HTSResponseHandler authHandler = new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                logger.log(TAG, "handleResponse: Response to 'authenticate' message");
                
                auth = response.getInt("noaccess", 0) != 1;
                logger.log(TAG, "handleResponse: Authentication successful: " + auth);
                if (!auth) {
                    listener.onError(Constants.ACTION_CONNECTION_STATE_AUTH);
                }
                synchronized (authMessage) {
                    authMessage.notify();
                }
            }
        };

        logger.log(TAG, "authenticate: Sending 'hello' message");
        HTSMessage helloMessage = new HTSMessage();
        helloMessage.setMethod("hello");
        helloMessage.putField("clientname", this.clientName);
        helloMessage.putField("clientversion", this.clientVersion);
        helloMessage.putField("htspversion", HTSMessage.HTSP_VERSION);
        helloMessage.putField("username", username);

        sendMessage(helloMessage, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                logger.log(TAG, "handleResponse: Response to 'hello' message");

                protocolVersion = response.getInt("htspversion");
                serverName = response.getString("servername");
                serverVersion = response.getString("serverversion");
                webRoot = response.getString("webroot", "");

                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("SHA1");
                    md.update(password.getBytes());
                    md.update(response.getByteArray("challenge"));

                    logger.log(TAG, "authenticate: Sending 'authenticate' message");
                    authMessage.putField("digest", md.digest());
                    sendMessage(authMessage, authHandler);
                } catch (NoSuchAlgorithmException ex) {
                    logger.log(TAG, "handleResponse: Could not sent 'authenticate' message. " + ex.getLocalizedMessage());
                }
            }
        });

        synchronized (authMessage) {
            try {
                authMessage.wait(5000);
                if (!auth) {
                    logger.log(TAG, "authenticate: Timeout waiting for authentication response");
                    listener.onError(Constants.ACTION_CONNECTION_STATE_AUTH);
                }
            } catch (InterruptedException ex) {
                logger.log(TAG, "authenticate: Waiting for authentication message was interrupted. " + ex.getLocalizedMessage());
            }
        }
    }

    public boolean isAuthenticated() {
        return auth;
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
            responseHandlerMethods.put(seq, message.getMethod());
            socketChannel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
            messageQueue.add(message);
            selector.wakeup();
        } catch (Exception e) {
            logger.log(TAG, "sendMessage: Could not send message. " + e.getLocalizedMessage());
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        logger.log(TAG, "close() called");
        lock.lock();
        try {
            responseHandlers.clear();
            responseHandlerMethods.clear();
            messageQueue.clear();
            auth = false;
            running = false;
            socketChannel.register(selector, 0);
            socketChannel.close();
        } catch (Exception e) {
            logger.log(TAG, "close: Could not close connection. " + e.getLocalizedMessage());
        } finally {
            lock.unlock();
        }
        logger.log(TAG, "close() returned: ");
    }

    @Override
    public void run() {
        logger.log(TAG, "run() called");

        while (running) {
            try {
                selector.select(5000);
            } catch (IOException ex) {
                listener.onError(Constants.ACTION_CONNECTION_STATE_LOST);
                running = false;
                continue;
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
                running = false;

            } finally {
                lock.unlock();
            }
        }
        close();
        logger.log(TAG, "run() returned:");
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
            int len = sChannel.read(inBuf);
            if (len < 0) {
                listener.onError(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN);
                logger.log(TAG, "processTcpSelectionKey: Could not read data from server");
                throw new IOException();
            }

            HTSMessage msg = HTSMessage.parse(inBuf);
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
            int respSeq = msg.getInt("seq");
            HTSResponseHandler handler = responseHandlers.get(respSeq);
            responseHandlers.remove(respSeq);
            String method = responseHandlerMethods.get(respSeq);
            msg.setMethod(method);

            if (handler != null) {
            	synchronized (handler) {
                    handler.handleResponse(msg);
            	}
                return;
            }
        }
        listener.onMessage(msg);
    }
    
    public int getProtocolVersion() {
    	return this.protocolVersion;
    }

    public String getServerName() {
        return this.serverName;
    }

    public String getServerVersion() {
        return this.serverVersion;
    }

    public String getWebRoot() {
    	return this.webRoot;
    }
}
