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
package org.me.tvhguide.htsp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import java.util.Map;
import org.me.tvhguide.R;
import org.me.tvhguide.TVHGuideApplication;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.ChannelTag;
import org.me.tvhguide.model.Programme;

/**
 *
 * @author john
 */
public class HTSService extends Service {

    public static final String ACTION_CONNECT = "org.me.tvhguide.htsp.CONNECT";
    public static final String ACTION_REFRESH = "org.me.tvhguide.htsp.REFRESH";
    private static final String TAG = "HTSService";
    private SelectionThread t;
    private ByteBuffer inBuf;
    private LinkedList<HTSMessage> requestQue = new LinkedList<HTSMessage>();
    private Map<Integer, HTSResponseListener> responseHandelers = new HashMap<Integer, HTSResponseListener>();
    private String username;
    private String password;
    private InetSocketAddress addr;
    private int seq = 0;

    public class LocalBinder extends Binder {

        HTSService getService() {
            return HTSService.this;
        }
    }

    @Override
    public void onCreate() {
        inBuf = ByteBuffer.allocateDirect(1024 * 1024);
        inBuf.limit(4);

        t = new SelectionThread() {

            @Override
            public void onEvent(int selectionKey, SocketChannel ch) throws Exception {
                onNetworkEvent(selectionKey, ch);
            }
        };
        t.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);



        try {
            String host = prefs.getString("serverHostPref", "localhost");
            int port = Integer.parseInt(prefs.getString("serverPortPref", "9982"));
            InetSocketAddress ad = new InetSocketAddress(host, port);
            String user = prefs.getString("usernamePref", "");
            String pw = prefs.getString("passwordPref", "");

            if (ad.equals(addr) && user.equals(username) && pw.equals(password)) {
                return START_NOT_STICKY;
            }

            addr = ad;
            username = user;
            password = pw;

            SocketChannel sChannel = SocketChannel.open();
            sChannel.configureBlocking(false);
            sChannel.connect(addr);
            t.register(sChannel, SelectionKey.OP_CONNECT, true);
        } catch (Throwable ex) {
            Log.e(TAG, "Can't connect to server", ex);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        t.setRunning(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final IBinder mBinder = new LocalBinder();

    private void onNetworkEvent(int selectionKey, SocketChannel ch) throws Exception {
        switch (selectionKey) {
            case SelectionKey.OP_CONNECT: {
                t.register(ch, SelectionKey.OP_CONNECT, false);

                HTSMessage request = new HTSMessage();
                request.setMethod("hello");
                request.putField("clientname", getString(R.string.app_name));
                request.putField("clientversion", getString(R.string.app_version));
                request.putField("htspversion", HTSMessage.HTSP_VERSION);
                request.putField("username", username);
                request.putField("seq", seq);
                requestQue.add(request);

                responseHandelers.put(seq, new HTSResponseListener() {

                    public void handleResonse(HTSMessage response) throws Exception {
                        MessageDigest md = MessageDigest.getInstance("SHA1");
                        md.update(password.getBytes());
                        md.update(response.getByteArray("challenge"));

                        HTSMessage request = new HTSMessage();
                        request.setMethod("enableAsyncMetadata");
                        request.putField("username", "john");
                        request.putField("digest", md.digest());
                        requestQue.add(request);
                    }
                });

                seq++;

                TVHGuideApplication app = (TVHGuideApplication) getApplication();
                app.setLoading(true);

                t.register(ch, SelectionKey.OP_READ, true);
                t.register(ch, SelectionKey.OP_WRITE, true);
                break;
            }
            case SelectionKey.OP_READ: {
                int len = ch.read(inBuf);
                if (len < 0) {
                    throw new IOException("Server went down");
                }

                HTSMessage response = HTSMessage.parse(inBuf);
                if (response == null) {
                    break;
                }

                handleResponse(response);
                if (!requestQue.isEmpty()) {
                    t.register(ch, SelectionKey.OP_WRITE, true);
                }
                break;
            }
            case SelectionKey.OP_WRITE: {
                HTSMessage request = requestQue.peek();
                if (request != null && request.transmit(ch)) {
                    requestQue.removeFirst();
                }
                if (requestQue.isEmpty()) {
                    t.register(ch, SelectionKey.OP_WRITE, false);
                }
                break;
            }
            default:
                return;
        }
    }

    private void handleResponse(HTSMessage response) throws Exception {
        if (!response.containsKey("method") && !response.containsKey("seq")) {
            return;
        }

        if (response.containsKey("seq")) {
            int respSeq = response.getInt("seq");
            responseHandelers.get(respSeq).handleResonse(response);
            responseHandelers.remove(respSeq);
            return;
        }

        TVHGuideApplication app = (TVHGuideApplication) getApplication();

        String method = response.getMethod();
        if (method.equals("tagAdd")) {
            ChannelTag tag = new ChannelTag();
            tag.id = response.getLong("tagId");
            tag.name = response.getString("tagName");
            tag.icon = response.getString("tagIcon");
            //tag.members = (ArrayList) msg.get("members");
            app.addChannelTag(tag);
        } else if (method.equals("tagUpdate")) {
            //ChannelTag tag = msg.get((Long) msg.get("tagId"));
            //tag.name = (String) msg.get("tagName");
            //tag.icon = (String) msg.get("tagIcon");
            //tag.members = (ArrayList) msg.get("members");
        } else if (method.equals("tagDelete")) {
            app.removeChannelTag(response.getLong("tagId"));
        } else if (method.equals("channelAdd")) {
            final Channel ch = new Channel();
            ch.id = response.getLong("channelId");
            ch.name = response.getString("channelName");
            ch.number = response.getInt("channelNumber");
            ch.icon = response.getString("channelIcon");
            //ch.tags = (ArrayList) msg.get("tags");

            if (ch.number == 0) {
                ch.number = (int) (ch.id + 25000);
            }

            app.addChannel(ch);

            //Get the icon. Could use some optimization ;)
            if (ch.icon != null) {
                new Thread(new Runnable() {

                    public void run() {

                        try {
                            URL url = new URL(ch.icon);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setDoInput(true);
                            conn.connect();
                            ch.iconBitmap = BitmapFactory.decodeStream(conn.getInputStream());
                        } catch (Throwable ex) {
                        }

                    }
                }).start();

            }

            final long eventId = response.getLong("eventId");
            if (eventId < 1) {
                return;
            }

            HTSMessage request = new HTSMessage();
            request.setMethod("getEvents");
            request.putField("eventId", eventId);
            request.putField("numFollowing", 24);
            request.putField("seq", seq);

            responseHandelers.put(seq, new HTSResponseListener() {

                public void handleResonse(HTSMessage response) throws Exception {

                    if (!response.containsKey("events")) {
                        return;
                    }

                    for (HTSMessage sub : (List<HTSMessage>) response.get("events")) {
                        Programme p = new Programme();
                        p.id = eventId;
                        if (sub.containsFiled("ext_desc")) {
                            p.description = sub.getString("ext_desc");
                        } else if (sub.containsFiled("description")) {
                            p.description = sub.getString("description");
                        }
                        p.title = sub.getString("title");
                        p.start = sub.getDate("start");
                        p.stop = sub.getDate("stop");
                        ch.epg.add(p);
                    }
                }
            });
            requestQue.add(request);
            seq++;

        } else if (method.equals("channelUpdate")) {
            //Channel ch = channelMap.get((Long) msg.get("channelId"));
            //ch.name = (String) msg.get("channelName");
            //ch.number = (Long) msg.get("channelNumber");
            //ch.icon = (String) msg.get("channelIcon");
            //ch.eventId = (Long) msg.get("eventId");
            //ch.tags = (ArrayList) msg.get("tags");
        } else if (method.equals("channelDelete")) {
            app.removeChannel(response.getLong("channelId"));
        } else if (method.equals("initialSyncCompleted")) {
            app.setLoading(false);
        } else {
            Log.d(TAG, method.toString());
        }
    }
}
