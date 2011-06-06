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
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.me.tvhguide.R;
import org.me.tvhguide.TVHGuideApplication;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.ChannelTag;
import org.me.tvhguide.model.Programme;
import org.me.tvhguide.model.Recording;

/**
 *
 * @author john
 */
public class HTSService extends Service {

    public static final String ACTION_CONNECT = "org.me.tvhguide.htsp.CONNECT";
    public static final String ACTION_REFRESH = "org.me.tvhguide.htsp.REFRESH";
    public static final String ACTION_EPG_QUERY = "org.me.tvhguide.htsp.EPG_QUERY";
    public static final String ACTION_GET_EVENT = "org.me.tvhguide.htsp.GET_EVENT";
    public static final String ACTION_GET_EVENTS = "org.me.tvhguide.htsp.GET_EVENTS";
    public static final String ACTION_DVR_ADD = "org.me.tvhguide.htsp.DVR_ADD";
    public static final String ACTION_DVR_DELETE = "org.me.tvhguide.htsp.DVR_DELETE";
    public static final String ACTION_DVR_CANCEL = "org.me.tvhguide.htsp.DVR_CANCEL";
    private static final String TAG = "HTSService";
    private SelectionThread t;
    private ByteBuffer inBuf;
    private LinkedList<HTSMessage> requestQue = new LinkedList<HTSMessage>();
    private Map<Integer, HTSResponseListener> responseHandelers = new HashMap<Integer, HTSResponseListener>();
    private String username;
    private String password;
    private InetSocketAddress addr;
    private int seq = 0;
    private SocketChannel socketChannel;
    private ExecutorService execService;

    public class LocalBinder extends Binder {

        HTSService getService() {
            return HTSService.this;
        }
    }

    @Override
    public void onCreate() {
        inBuf = ByteBuffer.allocateDirect(1024 * 1024);
        inBuf.limit(4);

        execService = Executors.newFixedThreadPool(5);

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
                connect(false);
            } catch (Throwable ex) {
                showError(R.string.err_connect);
                Log.e(TAG, "Can't connect to server", ex);
            }
        } else if (ACTION_REFRESH.equals(intent.getAction())) {
            try {
                connect(true);
            } catch (Throwable ex) {
                showError(R.string.err_connect);
                Log.e(TAG, "Can't connect to server", ex);
            }
        } else if (ACTION_GET_EVENT.equals(intent.getAction())) {
            getEvent(intent.getLongExtra("eventId", 0));
        } else if (ACTION_GET_EVENTS.equals(intent.getAction())) {
            TVHGuideApplication app = (TVHGuideApplication) getApplication();
            Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            getEvents(ch,
                    intent.getLongExtra("eventId", 0),
                    intent.getIntExtra("count", 10));
        } else if (ACTION_DVR_ADD.equals(intent.getAction())) {
            addDvrEntry(intent.getLongExtra("eventId", 0));
        } else if (ACTION_DVR_DELETE.equals(intent.getAction())) {
            deleteDvrEntry(intent.getLongExtra("id", 0));
        } else if (ACTION_DVR_CANCEL.equals(intent.getAction())) {
            cancelDvrEntry(intent.getLongExtra("id", 0));
        } else if (ACTION_EPG_QUERY.equals(intent.getAction())) {
            TVHGuideApplication app = (TVHGuideApplication) getApplication();
            Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            epgQuery(ch,
                    intent.getStringExtra("query"),
                    intent.getLongExtra("tagId", 0));
        }

        return START_NOT_STICKY;
    }

    private void connect(boolean force) throws IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String host = prefs.getString("serverHostPref", "localhost");
        int port = Integer.parseInt(prefs.getString("serverPortPref", "9982"));
        InetSocketAddress ad = new InetSocketAddress(host, port);
        String user = prefs.getString("usernamePref", "");
        String pw = prefs.getString("passwordPref", "");

        if (!ad.equals(addr)
                || !user.equals(username)
                || !pw.equals(password)
                || (socketChannel != null && !socketChannel.isConnected())
                || force) {

            addr = ad;
            username = user;
            password = pw;

            if (socketChannel != null) {
                t.close(socketChannel);
            }

            TVHGuideApplication app = (TVHGuideApplication) getApplication();
            app.clearAll();

            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(addr);
            t.register(socketChannel, SelectionKey.OP_CONNECT, true);
        }
    }

    @Override
    public void onDestroy() {
        t.setRunning(false);
        execService.shutdown();
    }

    private void showError(final String error) {
        Log.d(TAG, error);
    }

    private void showError(int recourceId) {
        showError(getString(recourceId));
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
                PackageInfo packInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                request.setMethod("hello");
                request.putField("clientname", getString(R.string.app_name));
                request.putField("clientversion", packInfo.versionName);
                request.putField("htspversion", HTSMessage.HTSP_VERSION);
                request.putField("username", username);
                request.putField("seq", seq);
                requestQue.add(request);

                responseHandelers.put(seq, new HTSResponseListener() {

                    public void handleResponse(HTSMessage response) throws Exception {
                        MessageDigest md = MessageDigest.getInstance("SHA1");
                        md.update(password.getBytes());
                        md.update(response.getByteArray("challenge"));

                        HTSMessage request = new HTSMessage();
                        request.setMethod("enableAsyncMetadata");
                        request.putField("username", username);
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
            case -1: {
                showError(R.string.err_con_lost);
                socketChannel = null;
                break;
            }
            default:
                return;
        }
    }

    private void handleResponse(HTSMessage response) throws Exception {
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean loadIcons = prefs.getBoolean("loadIcons", false);

        if (response.containsFiled("error")) {
            showError(response.getString("error"));
            app.setLoading(false);
        }

        if (1 == response.getInt("noaccess", 0)) {
            showError(R.string.err_auth);
            app.setLoading(false);
        }

        if (!response.containsFiled("method") && !response.containsFiled("seq")) {
            return;
        }

        if (response.containsFiled("seq")) {
            int respSeq = response.getInt("seq");
            responseHandelers.get(respSeq).handleResponse(response);
            responseHandelers.remove(respSeq);
            return;
        }

        String method = response.getMethod();
        if (method.equals("tagAdd")) {
            ChannelTag tag = new ChannelTag();
            tag.id = response.getLong("tagId");
            tag.name = response.getString("tagName", null);
            tag.icon = response.getString("tagIcon", null);
            //tag.members = response.getIntList("members");
            app.addChannelTag(tag);
        } else if (method.equals("tagUpdate")) {
            for (ChannelTag tag : app.getChannelTags()) {
                if (tag.id == response.getLong("tagId")) {
                    tag.name = response.getString("tagName", tag.name);
                    tag.icon = response.getString("tagIcon", tag.icon);
                    //tag.members = response.getIntList("members");
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
            ch.tags = response.getIntList("tags", ch.tags);

            if (ch.number == 0) {
                ch.number = (int) (ch.id + 25000);
            }

            app.addChannel(ch);

            if (loadIcons && ch.icon != null) {
                getChannelIcon(ch);
            }

            long eventId = response.getLong("eventId", 0);
            if (eventId > 0) {
                getEvents(ch, eventId, 5);
            }

        } else if (method.equals("channelUpdate")) {
            Channel ch = app.getChannel(response.getLong("channelId"));
            if (ch != null) {
                ch.name = response.getString("channelName", ch.name);
                ch.number = response.getInt("channelNumber", ch.number);
                ch.icon = response.getString("channelIcon", ch.icon);
                ch.tags = response.getIntList("tags", ch.tags);

                //Remove programmes that have ended
                long eventId = response.getLong("eventId", 0);
                Iterator<Programme> it = ch.epg.iterator();
                ArrayList<Programme> tmp = new ArrayList<Programme>();

                while (it.hasNext()) {
                    Programme p = it.next();
                    if (p.id != eventId) {
                        tmp.add(p);
                    } else {
                        break;
                    }
                }
                ch.epg.removeAll(tmp);

                for (Programme p : tmp) {
                    app.removeProgramme(p);
                }

                if (eventId > 0 && ch.epg.size() < 2) {
                    getEvents(ch, eventId, 5);
                } else {
                    app.updateChannel(ch);
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
            rec.channel = app.getChannel(response.getLong("channel"));
            if (rec.channel != null) {
                rec.channel.recordings.add(rec);
            }
            app.addRecording(rec);
        } else if (method.equals("dvrEntryUpdate")) {
            Recording rec = app.getRecording(response.getLong("id"));
            if (rec != null) {
                rec.description = response.getString("description", rec.description);
                rec.error = response.getString("error", rec.error);
                rec.start = response.getDate("start");
                rec.state = response.getString("state", rec.state);
                rec.stop = response.getDate("stop");
                rec.title = response.getString("title", rec.title);
            }
            app.updateRecording(rec);
        } else if (method.equals("dvrEntryDelete")) {
            Recording rec = app.getRecording(response.getLong("id"));
            if (rec.channel != null) {
                rec.channel.recordings.remove(rec);
            }
            app.removeRecording(rec);
        } else {
            Log.d(TAG, method.toString());
        }
    }

    private void getChannelIcon(final Channel ch) {
        execService.execute(new Runnable() {

            public void run() {

                try {
                    InputStream inputStream = new URL(ch.icon).openStream();
                    ch.iconDrawable = Drawable.createFromStream(inputStream, ch.icon);
                    TVHGuideApplication app = (TVHGuideApplication) getApplication();
                    app.updateChannel(ch);
                } catch (Throwable ex) {
                }
            }
        });
    }

    private void getEvents(final Channel ch, final long eventId, int cnt) {
        if (ch == null) {
            return;
        }

        HTSMessage request = new HTSMessage();
        request.setMethod("getEvents");
        request.putField("eventId", eventId);
        request.putField("numFollowing", cnt);
        request.putField("seq", seq);

        responseHandelers.put(seq, new HTSResponseListener() {

            public void handleResponse(HTSMessage response) throws Exception {

                if (!response.containsKey("events")) {
                    return;
                }

                TVHGuideApplication app = (TVHGuideApplication) getApplication();

                for (Object obj : response.getList("events")) {
                    Programme p = new Programme();
                    HTSMessage sub = (HTSMessage) obj;
                    p.id = sub.getLong("eventId");
                    p.nextId = sub.getLong("nextEventId", 0);
                    p.description = sub.getString("description", null);
                    p.ext_desc = sub.getString("ext_text", p.description);
                    p.type = sub.getInt("contentType", 0);
                    p.title = sub.getString("title");
                    p.start = sub.getDate("start");
                    p.stop = sub.getDate("stop");
                    p.channel = ch;
                    if (ch.epg.add(p)) {
                        app.addProgramme(p);
                    }
                }
                app.updateChannel(ch);
            }
        });
        requestQue.add(request);
        seq++;
        t.register(socketChannel, SelectionKey.OP_WRITE, true);
    }

    private void getEvent(long eventId) {
        HTSMessage request = new HTSMessage();
        request.setMethod("getEvent");
        request.putField("eventId", eventId);
        request.putField("seq", seq);
        requestQue.add(request);
        responseHandelers.put(seq, new HTSResponseListener() {

            public void handleResponse(HTSMessage response) throws Exception {
                TVHGuideApplication app = (TVHGuideApplication) getApplication();
                Channel ch = app.getChannel(response.getLong("channelId"));
                Programme p = new Programme();
                p.id = response.getLong("eventId");
                p.nextId = response.getLong("nextEventId", 0);
                p.description = response.getString("description", null);
                p.ext_desc = response.getString("ext_text", p.description);
                p.type = response.getInt("contentType", 0);
                p.title = response.getString("title");
                p.start = response.getDate("start");
                p.stop = response.getDate("stop");
                p.channel = ch;

                if (ch.epg.add(p)) {
                    app.addProgramme(p);
                    app.updateChannel(ch);
                }
            }
        });
        seq++;
        t.register(socketChannel, SelectionKey.OP_WRITE, true);
    }

    private void epgQuery(final Channel ch, String query, long tagId) {
        HTSMessage request = new HTSMessage();
        request.setMethod("epgQuery");
        request.putField("query", query);
        if (ch != null) {
            request.putField("channelId", ch.id);
        }
        if (tagId > 0) {
            request.putField("tagId", tagId);
        }
        request.putField("seq", seq);
        requestQue.add(request);
        responseHandelers.put(seq, new HTSResponseListener() {

            public void handleResponse(HTSMessage response) throws Exception {

                if (!response.containsKey("eventIds")) {
                    return;
                }

                for (Long id : response.getLongList("eventIds")) {
                    getEvent(id);
                }
            }
        });
        seq++;
        t.register(socketChannel, SelectionKey.OP_WRITE, true);
    }

    private void cancelDvrEntry(long id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("cancelDvrEntry");
        request.putField("id", id);
        request.putField("seq", seq);
        requestQue.add(request);
        responseHandelers.put(seq, new HTSResponseListener() {

            public void handleResponse(HTSMessage response) throws Exception {

                boolean success = response.getInt("success", 0) == 1;
            }
        });
        seq++;
        t.register(socketChannel, SelectionKey.OP_WRITE, true);
    }

    private void deleteDvrEntry(long id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("deleteDvrEntry");
        request.putField("id", id);
        request.putField("seq", seq);
        requestQue.add(request);
        responseHandelers.put(seq, new HTSResponseListener() {

            public void handleResponse(HTSMessage response) throws Exception {

                boolean success = response.getInt("success", 0) == 1;
            }
        });
        seq++;
        t.register(socketChannel, SelectionKey.OP_WRITE, true);
    }

    private void addDvrEntry(long eventId) {
        HTSMessage request = new HTSMessage();
        request.setMethod("addDvrEntry");
        request.putField("eventId", eventId);
        request.putField("seq", seq);
        requestQue.add(request);
        responseHandelers.put(seq, new HTSResponseListener() {

            public void handleResponse(HTSMessage response) throws Exception {

                boolean success = response.getInt("success", 0) == 1;
                String error = response.getString("error", null);
            }
        });
        seq++;
        t.register(socketChannel, SelectionKey.OP_WRITE, true);
    }
}
