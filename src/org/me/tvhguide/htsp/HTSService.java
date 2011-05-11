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
import java.util.Iterator;
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
    public static final String ACTION_EPG_QUERY = "org.me.tvhguide.htsp.EPG_QUERY";
    public static final String ACTION_GET_EVENT = "org.me.tvhguide.htsp.GET_EVENT";
    public static final String ACTION_DVR_ADD = "org.me.tvhguide.htsp.DVR_ADD";
    public static final String ACTION_DVR_DEL = "org.me.tvhguide.htsp.DVR_DEL";
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

        if (ACTION_CONNECT.equals(intent.getAction())) {
            try {
                connect(intent, false);
            } catch (Throwable ex) {
                Log.e(TAG, "Can't connect to server", ex);
            }
        } else if (ACTION_REFRESH.equals(intent.getAction())) {
            try {
                TVHGuideApplication app = (TVHGuideApplication) getApplication();
                app.getChannelTags().clear();
                app.getChannels().clear();
                connect(intent, true);
            } catch (Throwable ex) {
                Log.e(TAG, "Can't connect to server", ex);
            }
        } else if (ACTION_GET_EVENT.equals(intent.getAction())) {
            HTSMessage request = new HTSMessage();
            request.setMethod("getEvent");
            request.putField("eventId", intent.getStringExtra("eventId"));
            request.putField("seq", seq);
            requestQue.add(request);
            responseHandelers.put(seq, new HTSResponseListener() {

                public void handleResonse(HTSMessage response) throws Exception {
                    Programme p = new Programme();
                    p.id = response.getLong("eventId");
                    if (response.containsFiled("ext_desc")) {
                        p.description = response.getString("ext_desc");
                    } else if (response.containsFiled("description")) {
                        p.description = response.getString("description");
                    }
                    p.title = response.getString("title");
                    p.start = response.getDate("start");
                    p.stop = response.getDate("stop");

                    TVHGuideApplication app = (TVHGuideApplication) getApplication();
                    for (Channel ch : app.getChannels()) {
                        if (ch.id == response.getLong("channelId")) {
                            ch.epg.add(p);
                            break;
                        }
                    }
                }
            });
            seq++;
        } else if (ACTION_DVR_ADD.equals(intent.getAction())) {
        } else if (ACTION_DVR_DEL.equals(intent.getAction())) {
        } else if (ACTION_EPG_QUERY.equals(intent.getAction())) {
        }

        return START_NOT_STICKY;
    }

    private boolean connect(Intent intent, boolean force) throws IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String host = prefs.getString("serverHostPref", "localhost");
        int port = Integer.parseInt(prefs.getString("serverPortPref", "9982"));
        InetSocketAddress ad = new InetSocketAddress(host, port);
        String user = prefs.getString("usernamePref", "");
        String pw = prefs.getString("passwordPref", "");

        if (!force && ad.equals(addr) && user.equals(username) && pw.equals(password)) {
            return false;
        }

        addr = ad;
        username = user;
        password = pw;

        SocketChannel sChannel = SocketChannel.open();
        sChannel.configureBlocking(false);
        sChannel.connect(addr);
        t.register(sChannel, SelectionKey.OP_CONNECT, true);

        return true;
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
            tag.name = response.getString("tagName", null);
            tag.icon = response.getString("tagIcon", null);
            //tag.members = (ArrayList) msg.get("members");
            app.addChannelTag(tag);
        } else if (method.equals("tagUpdate")) {
            for (ChannelTag tag : app.getChannelTags()) {
                if (tag.id == response.getLong("tagId")) {
                    tag.name = response.getString("tagName", tag.name);
                    tag.icon = response.getString("tagIcon", tag.icon);
                    break;
                }
            }
        } else if (method.equals("tagDelete")) {
            app.removeChannelTag(response.getLong("tagId"));
        } else if (method.equals("channelAdd")) {
            final Channel ch = new Channel();
            ch.id = response.getLong("channelId");
            ch.name = response.getString("channelName", null);
            ch.number = response.getInt("channelNumber", 0);
            ch.icon = response.getString("channelIcon", null);
            ch.tags = (List<Integer>) response.getList("tags", Integer.class);

            if (ch.number == 0) {
                ch.number = (int) (ch.id + 25000);
            }

            app.addChannel(ch);

            if (ch.icon != null) {
                loadChannelIcon(ch);
            }

            long eventId = response.getLong("eventId", 0);
            if (eventId > 0) {
                loadProgrammes(ch, eventId, 5);
            }

        } else if (method.equals("channelUpdate")) {
            for (Channel ch : app.getChannels()) {
                if (ch.id == response.getLong("channelId")) {
                    ch.name = response.getString("channelName", ch.name);
                    ch.number = response.getInt("channelNumber", ch.number);
                    ch.icon = response.getString("channelIcon", ch.icon);

                    //Remove programmes that have ended
                    long eventId = response.getLong("eventId", 0);
                    Iterator<Programme> it = ch.epg.iterator();
                    while (it.hasNext()) {
                        Programme p = it.next();
                        if (p.id != eventId) {
                            ch.epg.remove(p);
                        }
                    }

                    if (eventId > 0 && ch.epg.size() < 2) {
                        loadProgrammes(ch, eventId, 5);
                    }
                    //ch.tags = (ArrayList) msg.get("tags");
                    break;
                }
            }
        } else if (method.equals("channelDelete")) {
            app.removeChannel(response.getLong("channelId"));
        } else if (method.equals("initialSyncCompleted")) {
            app.setLoading(false);
        } else if (method.equals("dvrEntryAdd")) {
            Recording rec = new Recording();
            rec.id = response.getLong("id");
            rec.description = response.getString("description", null);
            rec.error = response.getString("error", null);
            rec.start = response.getDate("start");
            rec.state = response.getString("state", null);
            rec.stop = response.getDate("stop");
            rec.title = response.getString("title", null);
            app.addRecording(rec);
        } else if (method.equals("dvrEntryUpdate")) {
            for (Recording rec : app.getRecordings()) {
                if (rec.id == response.getLong("id")) {
                    rec.description = response.getString("description", rec.description);
                    rec.error = response.getString("error", rec.error);
                    rec.start = response.getDate("start");
                    rec.state = response.getString("state", rec.state);
                    rec.stop = response.getDate("stop");
                    rec.title = response.getString("title", rec.title);
                    break;
                }
            }
        } else if (method.equals("dvrEntryDelete")) {
            app.removeRecording(response.getLong("id"));
        } else {
            Log.d(TAG, method.toString());
        }
    }

    private void loadChannelIcon(final Channel ch) {
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

    private void loadProgrammes(final Channel ch, final long eventId, int cnt) {
        HTSMessage request = new HTSMessage();
        request.setMethod("getEvents");
        request.putField("eventId", eventId);
        request.putField("numFollowing", cnt);
        request.putField("seq", seq);

        responseHandelers.put(seq, new HTSResponseListener() {

            public void handleResonse(HTSMessage response) throws Exception {

                if (!response.containsKey("events")) {
                    return;
                }

                for (HTSMessage sub : (List<HTSMessage>)response.getList("events", HTSMessage.class)) {
                    Programme p = new Programme();
                    p.id = eventId;
                    if (sub.containsFiled("description")) {
                        p.description = sub.getString("description");
                    } else if (sub.containsFiled("ext_desc")) {
                        p.description = sub.getString("ext_desc");
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
    }
}
