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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.me.tvhguide.R;
import org.me.tvhguide.TVHGuideApplication;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.ChannelTag;
import org.me.tvhguide.model.HttpTicket;
import org.me.tvhguide.model.Packet;
import org.me.tvhguide.model.Programme;
import org.me.tvhguide.model.Recording;
import org.me.tvhguide.model.Stream;
import org.me.tvhguide.model.Subscription;

/**
 *
 * @author john-tornblom
 */
public class HTSService extends Service implements HTSConnectionListener {

    public static final String ACTION_CONNECT = "org.me.tvhguide.htsp.CONNECT";
    public static final String ACTION_DISCONNECT = "org.me.tvhguide.htsp.DISCONNECT";
    public static final String ACTION_EPG_QUERY = "org.me.tvhguide.htsp.EPG_QUERY";
    public static final String ACTION_GET_EVENT = "org.me.tvhguide.htsp.GET_EVENT";
    public static final String ACTION_GET_EVENTS = "org.me.tvhguide.htsp.GET_EVENTS";
    public static final String ACTION_DVR_ADD = "org.me.tvhguide.htsp.DVR_ADD";
    public static final String ACTION_DVR_DELETE = "org.me.tvhguide.htsp.DVR_DELETE";
    public static final String ACTION_DVR_CANCEL = "org.me.tvhguide.htsp.DVR_CANCEL";
    public static final String ACTION_SUBSCRIBE = "org.me.tvhguide.htsp.SUBSCRIBE";
    public static final String ACTION_UNSUBSCRIBE = "org.me.tvhguide.htsp.UNSUBSCRIBE";
    public static final String ACTION_FEEDBACK = "org.me.tvhguide.htsp.FEEDBACK";
    public static final String ACTION_GET_TICKET = "org.me.tvhguide.htsp.GET_TICKET";
    private static final String TAG = "HTSService";
    private ScheduledExecutorService execService;
    private HTSConnection connection;
    PackageInfo packInfo;

    public class LocalBinder extends Binder {

        HTSService getService() {
            return HTSService.this;
        }
    }

    @Override
    public void onCreate() {
        execService = Executors.newScheduledThreadPool(5);
        try {
            packInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException ex) {
            Log.e(TAG, "Can't get package info", ex);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_CONNECT.equals(intent.getAction())) {
            boolean force = intent.getBooleanExtra("force", false);
            final String hostname = intent.getStringExtra("hostname");
            final int port = intent.getIntExtra("port", 9982);
            final String username = intent.getStringExtra("username");
            final String password = intent.getStringExtra("password");

            if (connection != null && force) {
                connection.close();
            }

            if (connection == null || !connection.isConnected()) {
                final TVHGuideApplication app = (TVHGuideApplication) getApplication();
                app.clearAll();
                app.setLoading(true);
                connection = new HTSConnection(this, packInfo.packageName, packInfo.versionName);

                //Since this is blocking, spawn to a new thread
                execService.execute(new Runnable() {

                    public void run() {
                        connection.open(hostname, port);
                        connection.authenticate(username, password);
                    }
                });
            }
        } else if (connection == null || !connection.isConnected()) {
            Log.e(TAG, "No connection to perform " + intent.getAction());
        } else if (ACTION_DISCONNECT.equals(intent.getAction())) {
            connection.close();
        } else if (ACTION_GET_EVENT.equals(intent.getAction())) {
            getEvent(intent.getLongExtra("eventId", 0));
        } else if (ACTION_GET_EVENTS.equals(intent.getAction())) {
            TVHGuideApplication app = (TVHGuideApplication) getApplication();
            Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            getEvents(ch,
                    intent.getLongExtra("eventId", 0),
                    intent.getIntExtra("count", 10));
        } else if (ACTION_DVR_ADD.equals(intent.getAction())) {
            TVHGuideApplication app = (TVHGuideApplication) getApplication();
            Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            addDvrEntry(ch, intent.getLongExtra("eventId", 0));
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
        } else if (ACTION_SUBSCRIBE.equals(intent.getAction())) {
            subscribe(intent.getLongExtra("channelId", 0),
                    intent.getLongExtra("subscriptionId", 0),
                    intent.getIntExtra("maxWidth", 0),
                    intent.getIntExtra("maxHeight", 0),
                    intent.getStringExtra("audioCodec"),
                    intent.getStringExtra("videoCodec"));
        } else if (ACTION_UNSUBSCRIBE.equals(intent.getAction())) {
            unsubscribe(intent.getLongExtra("subscriptionId", 0));
        } else if (ACTION_FEEDBACK.equals(intent.getAction())) {
            feedback(intent.getLongExtra("subscriptionId", 0),
                    intent.getIntExtra("speed", 0));
        } else if (ACTION_GET_TICKET.equals(intent.getAction())) {
            TVHGuideApplication app = (TVHGuideApplication) getApplication();
            Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            Recording rec = app.getRecording(intent.getLongExtra("dvrId", 0));
            if (ch != null) {
                getTicket(ch);
            } else if (rec != null) {
                getTicket(rec);
            }
        }


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        execService.shutdown();
        if (connection != null) {
            connection.close();
        }
    }

    private void showError(final String error) {
        if (error == null || error.length() < 0) {
            return;
        }

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.setLoading(false);
        app.broadcastError(error);
    }

    private void showError(int recourceId) {
        showError(getString(recourceId));
    }

    public void onError(int errorCode) {
        switch (errorCode) {
            case HTSConnection.CONNECTION_LOST_ERROR:
                showError(R.string.err_con_lost);
                break;
            case HTSConnection.TIMEOUT_ERROR:
                showError("Connection timeout");
                break;
            case HTSConnection.CONNECTION_REFUSED_ERROR:
                showError(R.string.err_connect);
                break;
            case HTSConnection.HTS_AUTH_ERROR:
                showError(R.string.err_auth);
                break;
        }
    }

    public void onError(Exception ex) {
        showError(ex.getLocalizedMessage());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final IBinder mBinder = new LocalBinder();

    public void onAsyncResponse(HTSMessage response) {
        TVHGuideApplication app = (TVHGuideApplication) getApplication();

        String method = response.getMethod();
        if (method.equals("tagAdd")) {
            ChannelTag tag = new ChannelTag();
            tag.id = response.getLong("tagId");
            tag.name = response.getString("tagName", null);
            tag.icon = response.getString("tagIcon", null);
            //tag.members = response.getIntList("members");
            app.addChannelTag(tag);
            if (tag.icon != null) {
                getChannelTagIcon(tag);
            }
        } else if (method.equals("tagUpdate")) {
            ChannelTag tag = app.getChannelTag(response.getLong("tagId"));
            if (tag != null) {
                tag.name = response.getString("tagName", tag.name);
                String icon = response.getString("tagIcon", tag.icon);
                if (icon == null) {
                    tag.icon = null;
                    tag.iconBitmap = null;
                } else if (!icon.equals(tag.icon)) {
                    tag.icon = icon;
                    getChannelTagIcon(tag);
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
            if (ch.icon != null) {
                getChannelIcon(ch);
            }
            long currEventId = response.getLong("eventId", 0);
            long nextEventId = response.getLong("nextEventId", 0);

            ch.isTransmitting = currEventId != 0;

            if (currEventId > 0) {
                getEvents(ch, currEventId, 5);
            } else if (nextEventId > 0) {
                getEvents(ch, nextEventId, 5);
            }
        } else if (method.equals("channelUpdate")) {
            final Channel ch = app.getChannel(response.getLong("channelId"));
            if (ch != null) {
                ch.name = response.getString("channelName", ch.name);
                ch.number = response.getInt("channelNumber", ch.number);
                String icon = response.getString("channelIcon", ch.icon);
                ch.tags = response.getIntList("tags", ch.tags);

                if (icon == null) {
                    ch.icon = null;
                    ch.iconBitmap = null;
                } else if (!icon.equals(ch.icon)) {
                    ch.icon = icon;
                    getChannelIcon(ch);
                }
                //Remove programmes that have ended
                long currEventId = response.getLong("eventId", 0);
                long nextEventId = response.getLong("nextEventId", 0);

                ch.isTransmitting = currEventId != 0;

                Iterator<Programme> it = ch.epg.iterator();
                ArrayList<Programme> tmp = new ArrayList<Programme>();

                while (it.hasNext() && currEventId > 0) {
                    Programme p = it.next();
                    if (p.id != currEventId) {
                        tmp.add(p);
                    } else {
                        break;
                    }
                }
                ch.epg.removeAll(tmp);

                for (Programme p : tmp) {
                    app.removeProgramme(p);
                }

                final long eventId = currEventId != 0 ? currEventId : nextEventId;
                if (eventId > 0 && ch.epg.size() < 2) {
                    execService.schedule(new Runnable() {

                        public void run() {
                            getEvents(ch, eventId, 5);
                        }
                    }, 30, TimeUnit.SECONDS);
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
                app.updateRecording(rec);
            }
        } else if (method.equals("dvrEntryDelete")) {
            Recording rec = app.getRecording(response.getLong("id"));
            if (rec != null && rec.channel != null) {
                rec.channel.recordings.remove(rec);
                for (Programme p : rec.channel.epg) {
                    if (p.recording == rec) {
                        p.recording = null;
                        app.updateProgramme(p);
                        break;
                    }
                }
                app.removeRecording(rec);
            }
        } else if (method.equals("subscriptionStart")) {
            Subscription subscription = app.getSubscription(response.getLong("subscriptionId"));
            if (subscription == null) {
                return;
            }

            for (Object obj : response.getList("streams")) {
                Stream s = new Stream();
                HTSMessage sub = (HTSMessage) obj;

                s.index = sub.getInt("index");
                s.type = sub.getString("type");
                s.language = sub.getString("language", "");
                s.width = sub.getInt("width", 0);
                s.height = sub.getInt("height", 0);

                subscription.streams.add(s);
            }
        } else if (method.equals("subscriptionStatus")) {
            Subscription s = app.getSubscription(response.getLong("subscriptionId"));
            if (s == null) {
                return;
            }
            String status = response.getString("status", null);
            if (s.status == null ? status != null : !s.status.equals(status)) {
                s.status = status;
                app.updateSubscription(s);
            }
        } else if (method.equals("subscriptionStop")) {
            Subscription s = app.getSubscription(response.getLong("subscriptionId"));
            if (s == null) {
                return;
            }
            String status = response.getString("status", null);
            if (s.status == null ? status != null : !s.status.equals(status)) {
                s.status = status;
                app.updateSubscription(s);
            }
            app.removeSubscription(s);
        } else if (method.equals("muxpkt")) {
            Subscription sub = app.getSubscription(response.getLong("subscriptionId"));
            if (sub == null) {
                return;
            }

            Packet packet = new Packet();
            packet.dts = response.getLong("dts", 0);
            packet.pts = response.getLong("pts", 0);
            packet.duration = response.getLong("duration");
            packet.frametype = response.getInt("frametype");
            packet.payload = response.getByteArray("payload");

            for (Stream st : sub.streams) {
                if (st.index == response.getInt("stream")) {
                    packet.stream = st;
                }
            }
            packet.subscription = sub;
            app.broadcastPacket(packet);
        } else if (method.equals("queueStatus")) {
            Subscription sub = app.getSubscription(response.getLong("subscriptionId"));
            if (sub == null) {
                return;
            }
            if (response.containsFiled("delay")) {
                BigInteger delay = response.getBigInteger("delay");
                delay = delay.divide(BigInteger.valueOf((1000)));
                sub.delay = delay.longValue();
            }
            sub.droppedBFrames = response.getLong("Bdrops", sub.droppedBFrames);
            sub.droppedIFrames = response.getLong("Idrops", sub.droppedIFrames);
            sub.droppedPFrames = response.getLong("Pdrops", sub.droppedPFrames);
            sub.packetCount = response.getLong("packets", sub.packetCount);
            sub.queSize = response.getLong("bytes", sub.queSize);

            app.updateSubscription(sub);
        } else {
            Log.d(TAG, method.toString());
        }
    }

    private void getChannelIcon(final Channel ch) {
        execService.execute(new Runnable() {

            public void run() {

                try {
                    InputStream inputStream = new URL(ch.icon).openStream();
                    ch.iconBitmap = BitmapFactory.decodeStream(inputStream);
                    TVHGuideApplication app = (TVHGuideApplication) getApplication();
                    app.updateChannel(ch);
                } catch (Throwable ex) {
                }
            }
        });
    }

    private void getChannelTagIcon(final ChannelTag tag) {
        execService.execute(new Runnable() {

            public void run() {

                try {
                    InputStream inputStream = new URL(tag.icon).openStream();
                    tag.iconBitmap = BitmapFactory.decodeStream(inputStream);
                    TVHGuideApplication app = (TVHGuideApplication) getApplication();
                    app.updateChannelTag(tag);
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
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {

                if (!response.containsKey("events")) {
                    return;
                }

                TVHGuideApplication app = (TVHGuideApplication) getApplication();

                for (Object obj : response.getList("events")) {
                    Programme p = new Programme();
                    HTSMessage sub = (HTSMessage) obj;
                    p.id = sub.getLong("eventId", 0);
                    p.nextId = sub.getLong("nextEventId", 0);
                    p.description = sub.getString("description", null);
                    p.ext_desc = sub.getString("ext_text", p.description);
                    p.recording = app.getRecording(sub.getLong("dvrId", 0));
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
    }

    private void getEvent(long eventId) {
        HTSMessage request = new HTSMessage();
        request.setMethod("getEvent");
        request.putField("eventId", eventId);

        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {
                TVHGuideApplication app = (TVHGuideApplication) getApplication();
                Channel ch = app.getChannel(response.getLong("channelId"));
                Programme p = new Programme();
                p.id = response.getLong("eventId");
                p.nextId = response.getLong("nextEventId", 0);
                p.description = response.getString("description", null);
                p.ext_desc = response.getString("ext_text", p.description);
                p.recording = app.getRecording(response.getLong("dvrId", 0));
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
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {

                if (!response.containsKey("eventIds")) {
                    return;
                }

                for (Long id : response.getLongList("eventIds")) {
                    getEvent(id);
                }
            }
        });
    }

    private void cancelDvrEntry(long id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("cancelDvrEntry");
        request.putField("id", id);
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {

                boolean success = response.getInt("success", 0) == 1;
            }
        });
    }

    private void deleteDvrEntry(long id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("deleteDvrEntry");
        request.putField("id", id);
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {

                boolean success = response.getInt("success", 0) == 1;
            }
        });
    }

    private void addDvrEntry(final Channel ch, final long eventId) {
        HTSMessage request = new HTSMessage();
        request.setMethod("addDvrEntry");
        request.putField("eventId", eventId);
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {
                if (response.getInt("success", 0) == 1) {
                    for (Programme p : ch.epg) {
                        if (p.id == eventId) {
                            TVHGuideApplication app = (TVHGuideApplication) getApplication();
                            p.recording = app.getRecording(response.getLong("id", 0));
                            app.updateProgramme(p);
                            break;
                        }
                    }
                }
                String error = response.getString("error", null);
            }
        });
    }

    private void subscribe(long channelId, long subscriptionId, int maxWidth, int maxHeight, String aCodec, String vCodec) {
        Subscription subscription = new Subscription();
        subscription.id = subscriptionId;
        subscription.status = "Subscribing";

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addSubscription(subscription);

        HTSMessage request = new HTSMessage();
        request.setMethod("subscribe");
        request.putField("channelId", channelId);
        request.putField("maxWidth", maxWidth);
        request.putField("maxHeight", maxHeight);
        request.putField("audioCodec", aCodec);
        request.putField("videoCodec", vCodec);
        request.putField("subscriptionId", subscriptionId);
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {
                //NOP
            }
        });
    }

    private void unsubscribe(long subscriptionId) {
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.removeSubscription(subscriptionId);

        HTSMessage request = new HTSMessage();
        request.setMethod("unsubscribe");
        request.putField("subscriptionId", subscriptionId);
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {
                //NOP
            }
        });
    }

    private void feedback(long subscriptionId, int speed) {
        HTSMessage request = new HTSMessage();
        request.setMethod("feedback");
        request.putField("subscriptionId", subscriptionId);
        request.putField("speed", speed);
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {
                //NOP
            }
        });
    }

    private void getTicket(Channel ch) {
        HTSMessage request = new HTSMessage();
        request.setMethod("getTicket");
        request.putField("channelId", ch.id);
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {
                String path = response.getString("path", null);
                String ticket = response.getString("ticket", null);

                if (path != null && ticket != null) {
                    TVHGuideApplication app = (TVHGuideApplication) getApplication();
                    app.addTicket(new HttpTicket(path, ticket));
                }
            }
        });
    }

    private void getTicket(Recording rec) {
        HTSMessage request = new HTSMessage();
        request.setMethod("getTicket");
        request.putField("dvrId", rec.id);
        connection.sendMessage(request, new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {
                String path = response.getString("path", null);
                String ticket = response.getString("ticket", null);

                if (path != null && ticket != null) {
                    TVHGuideApplication app = (TVHGuideApplication) getApplication();
                    app.addTicket(new HttpTicket(path, ticket));
                }
            }
        });
    }
}
