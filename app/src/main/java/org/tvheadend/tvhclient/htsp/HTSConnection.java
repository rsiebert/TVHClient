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
    private final Logger mLogger;
    private volatile boolean mRunning;
    private final Lock mLock;
    private SocketChannel mSocketChannel;
    private final ByteBuffer inBuf;
    private int seq;
    private final String mClientName;
    private final String mClientVersion;
    private int mProtocolVersion;
    private String mServerName;
    private String mServerVersion;
    private String mWebRoot;

    private final HTSConnectionListener mListener;
    private final SparseArray<HTSResponseHandler> mResponseHandlers;
    private final SparseArray<String> mResponseHandlerMethods;
    private final LinkedList<HTSMessage> mMessageQueue;
    private boolean mAuthenticated = false;
    private Selector mSelector;
    private final TVHClientApplication app;
    private int mConnectionTimeout = 5000;

    public HTSConnection(TVHClientApplication app, HTSConnectionListener listener, String clientName, String clientVersion) {
        this.app = app;
        mLogger = Logger.getInstance();

        // Disable the use of IPv6
        System.setProperty("java.net.preferIPv6Addresses", "false");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        mConnectionTimeout = Integer.parseInt(prefs.getString("connectionTimeout", "5")) * 1000;
        int bufferSize = Integer.parseInt(prefs.getString("bufferSize", "0"));

        mRunning = false;
        mLock = new ReentrantLock();
        inBuf = ByteBuffer.allocateDirect(2048 * 2048 * (bufferSize + 1));
        inBuf.limit(4);
        mResponseHandlers = new SparseArray<>();
        mResponseHandlerMethods = new SparseArray<>();
        mMessageQueue = new LinkedList<>();

        this.mListener = listener;
        this.mClientName = clientName;
        this.mClientVersion = clientVersion;
    }

    public void setRunning(boolean b) {
        try {
            mLock.lock();
            mRunning = false;
        } finally {
            mLock.unlock();
        }
    }

    // synchronized, non blocking connect
    public void open(String hostname, int port, boolean connected) {
        mLogger.log(TAG, "open() called with: hostname = [" + hostname + "], port = [" + port + "], connected = [" + connected + "]");

        if (mRunning) {
            return;
        }
        if (!connected) {
            mListener.onError(Constants.ACTION_CONNECTION_STATE_NO_NETWORK);
            return;
        }
        if (hostname == null) {
            mListener.onError(Constants.ACTION_CONNECTION_STATE_NO_CONNECTION);
            return;
        }

        final Object signal = new Object();

        mLock.lock();
        try {
            mSelector = Selector.open();
            mSocketChannel = SocketChannel.open();
            mSocketChannel.configureBlocking(false);
            mSocketChannel.socket().setKeepAlive(true);
            mSocketChannel.socket().setSoTimeout(mConnectionTimeout);
            mSocketChannel.register(mSelector, SelectionKey.OP_CONNECT, signal);
            mSocketChannel.connect(new InetSocketAddress(hostname, port));

            mRunning = true;
            start();
        } catch (Exception e) {
            mLogger.log(TAG, "open: Could not open connection. " + e.getLocalizedMessage());
            mListener.onError(Constants.ACTION_CONNECTION_STATE_REFUSED);
        } finally {
            mLock.unlock();
        }

        if (mRunning) {
            synchronized (signal) {
                try {
                    signal.wait(mConnectionTimeout);
                    if (mSocketChannel.isConnectionPending()) {
                        mLogger.log(TAG, "open: Timeout waiting for pending connection to server");
                        mListener.onError(Constants.ACTION_CONNECTION_STATE_TIMEOUT);
                        close();
                    } else {
                        // TODO change this
                        HTSMessage msg = new HTSMessage();
                        msg.setMethod("open");
                        mListener.onMessage(msg);
                    }
                } catch (InterruptedException ex) {
                    mLogger.log(TAG, "open: Waiting for pending connection was interrupted. " + ex.getLocalizedMessage());
                }
            }
        }
    }

    public boolean isConnected() {
        return mSocketChannel != null
                && mSocketChannel.isOpen()
                && mSocketChannel.isConnected()
                && mRunning;
    }

    // synchronized, blocking mAuthenticated
    public void authenticate(String username, final String password) {
        mLogger.log(TAG, "authenticate() called with: username = [" + username + "], password = [secret]");

        if (mAuthenticated || !mRunning) {
            return;
        }

        mAuthenticated = false;

        final HTSMessage authMessage = new HTSMessage();
        authMessage.setMethod("enableAsyncMetadata");
        authMessage.putField("username", username);

        final HTSResponseHandler authHandler = new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                mLogger.log(TAG, "handleResponse: Response to 'authenticate' message");
                
                mAuthenticated = response.getInt("noaccess", 0) != 1;
                mLogger.log(TAG, "handleResponse: Authentication successful: " + mAuthenticated);
                if (!mAuthenticated) {
                    mListener.onError(Constants.ACTION_CONNECTION_STATE_AUTH);
                }
                synchronized (authMessage) {
                    authMessage.notify();
                }
            }
        };

        mLogger.log(TAG, "authenticate: Sending 'hello' message");
        HTSMessage helloMessage = new HTSMessage();
        helloMessage.setMethod("hello");
        helloMessage.putField("clientname", this.mClientName);
        helloMessage.putField("clientversion", this.mClientVersion);
        helloMessage.putField("htspversion", HTSMessage.HTSP_VERSION);
        helloMessage.putField("username", username);

        sendMessage(helloMessage, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                mLogger.log(TAG, "handleResponse: Response to 'hello' message");

                mProtocolVersion = response.getInt("htspversion");
                mServerName = response.getString("servername");
                mServerVersion = response.getString("serverversion");
                mWebRoot = response.getString("webroot", "");

                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("SHA1");
                    md.update(password.getBytes());
                    md.update(response.getByteArray("challenge"));

                    mLogger.log(TAG, "authenticate: Sending 'authenticate' message");
                    authMessage.putField("digest", md.digest());
                    sendMessage(authMessage, authHandler);
                } catch (NoSuchAlgorithmException ex) {
                    mLogger.log(TAG, "handleResponse: Could not sent 'authenticate' message. " + ex.getLocalizedMessage());
                }
            }
        });

        synchronized (authMessage) {
            try {
                authMessage.wait(5000);
                if (!mAuthenticated) {
                    mLogger.log(TAG, "authenticate: Timeout waiting for authentication response");
                    mListener.onError(Constants.ACTION_CONNECTION_STATE_AUTH);
                }
            } catch (InterruptedException ex) {
                mLogger.log(TAG, "authenticate: Waiting for authentication message was interrupted. " + ex.getLocalizedMessage());
            }
        }
    }

    public boolean isAuthenticated() {
        return mAuthenticated;
    }

    public void sendMessage(HTSMessage message, HTSResponseHandler listener) {
        if (!isConnected()) {
            return;
        }
        mLock.lock();
        try {
            seq++;
            message.putField("seq", seq);
            mResponseHandlers.put(seq, listener);
            mResponseHandlerMethods.put(seq, message.getMethod());
            mSocketChannel.register(mSelector, SelectionKey.OP_WRITE | SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
            mMessageQueue.add(message);
            mSelector.wakeup();
        } catch (Exception e) {
            mLogger.log(TAG, "sendMessage: Could not send message. " + e.getLocalizedMessage());
        } finally {
            mLock.unlock();
        }
    }

    public void close() {
        mLogger.log(TAG, "close() called");
        mLock.lock();
        try {
            mResponseHandlers.clear();
            mResponseHandlerMethods.clear();
            mMessageQueue.clear();
            mAuthenticated = false;
            mRunning = false;
            mSocketChannel.register(mSelector, 0);
            mSocketChannel.close();
        } catch (Exception e) {
            mLogger.log(TAG, "close: Could not close connection. " + e.getLocalizedMessage());
        } finally {
            mLock.unlock();
        }
        mLogger.log(TAG, "close() returned: ");
    }

    @Override
    public void run() {
        mLogger.log(TAG, "run() called");

        while (mRunning) {
            try {
                mSelector.select(5000);
            } catch (IOException ex) {
                mListener.onError(Constants.ACTION_CONNECTION_STATE_LOST);
                mRunning = false;
                continue;
            }

            mLock.lock();
            try {
                Iterator<SelectionKey> it = mSelector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey selKey = it.next();
                    it.remove();
                    processTcpSelectionKey(selKey);
                }
                int ops = SelectionKey.OP_READ;
                if (!mMessageQueue.isEmpty()) {
                    ops |= SelectionKey.OP_WRITE;
                }
                mSocketChannel.register(mSelector, ops);

            } catch (Exception ex) {
                mRunning = false;

            } finally {
                mLock.unlock();
            }
        }
        close();
        mLogger.log(TAG, "run() returned:");
    }

    private void processTcpSelectionKey(SelectionKey selKey) throws IOException {

        if (selKey.isConnectable() && selKey.isValid()) {
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            sChannel.finishConnect();
            final Object signal = selKey.attachment();
            synchronized (signal) {
                signal.notify();
            }
            sChannel.register(mSelector, SelectionKey.OP_READ);
        }
        if (selKey.isReadable() && selKey.isValid()) {
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            int len = sChannel.read(inBuf);
            if (len < 0) {
                mListener.onError(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN);
                mLogger.log(TAG, "processTcpSelectionKey: Could not read data from server");
                throw new IOException();
            }

            HTSMessage msg = HTSMessage.parse(inBuf);
            if (msg != null) {
                handleMessage(msg);
            }
        }
        if (selKey.isWritable() && selKey.isValid()) {
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            HTSMessage msg = mMessageQueue.poll();
            if (msg != null) {
                msg.transmit(sChannel);
            }
        }
    }

    private void handleMessage(HTSMessage msg) {
        if (msg.containsField("seq")) {
            int respSeq = msg.getInt("seq");
            HTSResponseHandler handler = mResponseHandlers.get(respSeq);
            mResponseHandlers.remove(respSeq);
            String method = mResponseHandlerMethods.get(respSeq);
            msg.setMethod(method);

            if (handler != null) {
            	synchronized (handler) {
                    handler.handleResponse(msg);
            	}
                return;
            }
        }
        mListener.onMessage(msg);
    }
    
    public int getProtocolVersion() {
    	return this.mProtocolVersion;
    }

    public String getServerName() {
        return this.mServerName;
    }

    public String getServerVersion() {
        return this.mServerVersion;
    }

    public String getWebRoot() {
    	return this.mWebRoot;
    }
}
