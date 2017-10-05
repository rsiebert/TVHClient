package org.tvheadend.tvhclient.htsp;

import android.content.Context;
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

    private final HTSConnectionListener mListener;
    private final SparseArray<HTSResponseHandler> mResponseHandlers;
    private final SparseArray<String> mResponseHandlerMethods;
    private final LinkedList<HTSMessage> mMessageQueue;
    private boolean mAuthenticated = false;
    private Selector mSelector;
    private int mConnectionTimeout = 5000;

    public HTSConnection(Context context, HTSConnectionListener listener) {
        mLogger = Logger.getInstance();

        // Disable the use of IPv6
        System.setProperty("java.net.preferIPv6Addresses", "false");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
    }

    // synchronized, non blocking connect
    public void open(String hostname, int port, HTSResponseHandler handler) {
        mLogger.log(TAG, "open() called with: hostname = [" + hostname + "], port = [" + port + "]");

        if (mRunning) {
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

                        HTSMessage msg = new HTSMessage();
                        msg.setMethod("open");
                        if (handler != null) {
                            // TODO synchronized vs post ?
                            synchronized (handler) {
                                handler.handleResponse(msg);
                            }
                        }
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
}
