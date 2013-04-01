/*
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhguide.htsp;

import android.util.Log;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author john-tornblom
 */
public abstract class SelectionThread extends Thread {

    private static final String TAG = "SelectionThread";
    private Selector selector;
    private volatile boolean running;
    private HashMap<AbstractSelectableChannel, Integer> regBuf;
    private Lock lock;

    public SelectionThread() {
        running = false;
        lock = new ReentrantLock();
        regBuf = new HashMap<AbstractSelectableChannel, Integer>();
    }

    public void setRunning(boolean b) {
        try {
            lock.lock();
            running = false;
        } finally {
            lock.unlock();
        }
    }

    void close(AbstractSelectableChannel channel) throws IOException {
        lock.lock();
        try {
            regBuf.remove(channel);
            channel.close();
        } finally {
            lock.unlock();
        }
    }

    public void register(AbstractSelectableChannel channel, int ops, boolean b) {
        lock.lock();
        try {
            int oldOps = 0;
            if (regBuf.containsKey(channel)) {
                oldOps = regBuf.get(channel);
            }
            if (b) {
                ops |= oldOps;
            } else {
                ops = oldOps & ~ops;
            }
            regBuf.put(channel, ops);
            if (selector != null) {
                selector.wakeup();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        try {
            lock.lock();
            selector = Selector.open();
            running = true;
        } catch (IOException ex) {
            running = false;
            Log.e(TAG, "Can't open a selector", ex);
        } finally {
            lock.unlock();
        }

        while (running) {
            select(5000);
        }

        try {
            lock.lock();
            //Clean up
            for (SelectionKey key : selector.keys()) {
                try {
                    key.channel().close();
                } catch (IOException ex) {
                    Log.e(TAG, "Can't close channel", ex);
                    key.cancel();
                }

            }
            try {
                selector.close();
            } catch (IOException ex) {
                Log.e(TAG, "Can't close selector", ex);
            }
        } finally {
            lock.unlock();
        }
    }

    private void select(int timeout) {
        try {
            selector.select(timeout);
        } catch (IOException ex) {
            Log.e(TAG, "Can't select socket", ex);
            return;
        }

        Iterator it = selector.selectedKeys().iterator();

        //Process the selected keys
        while (it.hasNext()) {
            SelectionKey selKey = (SelectionKey) it.next();
            it.remove();
            processTcpSelectionKey(selKey);
        }

        try {
            lock.lock();
            ArrayList<AbstractSelectableChannel> tmp = new ArrayList<AbstractSelectableChannel>();
            for (AbstractSelectableChannel ch : regBuf.keySet()) {
                try {
                    int ops = regBuf.get(ch);
                    ch.register(selector, ops);
                } catch (Throwable t) {
                    tmp.add(ch);
                    Log.e(TAG, "Can't register channel", t);
                    if (ch instanceof SocketChannel) {
                        onError((SocketChannel) ch);
                    }
                }
            }
            for (AbstractSelectableChannel ch : tmp) {
                regBuf.remove(ch);
            }
        } finally {
            lock.unlock();
        }
    }

    private void processTcpSelectionKey(SelectionKey selKey) {
        //Incomming connection established
        if (selKey.isValid() && selKey.isAcceptable()) {
            try {
                ServerSocketChannel ssChannel = (ServerSocketChannel) selKey.channel();
                SocketChannel sChannel = ssChannel.accept();
                if (sChannel != null) {
                    sChannel.configureBlocking(false);
                    try {
                        onAccept(sChannel);
                    } catch (Throwable t) {
                        Log.e(TAG, "Can't establish connection", t);
                        onError(sChannel);
                        return;
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Can't establish connection", t);
                return;
            }
        }

        //Outgoing connection established
        if (selKey.isValid() && selKey.isConnectable()) {
            try {
                SocketChannel sChannel = (SocketChannel) selKey.channel();
                if (!sChannel.finishConnect()) {
                    onError(sChannel);
                    selKey.cancel();
                    return;
                }
                try {
                    onConnect(sChannel);
                } catch (Throwable t) {
                    Log.e(TAG, "Can't establish connection", t);
                    onError(sChannel);
                    selKey.cancel();
                    return;
                }
            } catch (Throwable t) {
                Log.e(TAG, "Can't establish connection", t);
                selKey.cancel();
                return;
            }
        }

        //Incomming data
        if (selKey.isValid() && selKey.isReadable()) {
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            try {
                onReadable(sChannel);
            } catch (Throwable t) {
                Log.e(TAG, "Can't read message", t);
                onError(sChannel);
                selKey.cancel();
                return;
            }
        }

        //Clear to send
        if (selKey.isValid() && selKey.isWritable()) {
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            try {
                onWrtiable(sChannel);
            } catch (Throwable t) {
                Log.e(TAG, "Can't send message", t);
                onError(sChannel);
                selKey.cancel();
                return;
            }
        }
    }

    private void onAccept(SocketChannel ch) throws Exception {
        onEvent(SelectionKey.OP_ACCEPT, ch);
    }

    private void onConnect(SocketChannel ch) throws Exception {
        onEvent(SelectionKey.OP_CONNECT, ch);
    }

    private void onReadable(SocketChannel ch) throws Exception {
        onEvent(SelectionKey.OP_READ, ch);
    }

    private void onError(SocketChannel ch) {
        try {
            lock.lock();
            ch.close();
            regBuf.remove(ch);
        } catch (Exception ex) {
            Log.e(TAG, null, ex);
        } finally {
            lock.unlock();
        }
        try {
            onEvent(-1, ch);
        } catch (Exception ex) {
        }
    }

    private void onWrtiable(SocketChannel ch) throws Exception {
        onEvent(SelectionKey.OP_WRITE, ch);
    }

    public abstract void onEvent(int selectionKey, SocketChannel ch) throws Exception;
}
