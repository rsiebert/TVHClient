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
package org.tvheadend.tvhclient.htsp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.interfaces.HTSConnectionListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.DvrCutpoint;
import org.tvheadend.tvhclient.model.HttpTicket;
import org.tvheadend.tvhclient.model.Packet;
import org.tvheadend.tvhclient.model.Profiles;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.SeriesInfo;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.model.SourceInfo;
import org.tvheadend.tvhclient.model.Stream;
import org.tvheadend.tvhclient.model.Subscription;
import org.tvheadend.tvhclient.model.TimerRecording;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 *
 * @author john-tornblom
 * @author Robert Siebert
 */
public class HTSService extends Service implements HTSConnectionListener {

    private static final String TAG = HTSService.class.getSimpleName();
    
    private ScheduledExecutorService execService;
    private HTSConnection connection;
    PackageInfo packInfo;
    private NotificationManager notificationManager = null;
    private SharedPreferences prefs;

    public class LocalBinder extends Binder {
        HTSService getService() {
            return HTSService.this;
        }
    }

    @Override
    public void onCreate() {
        execService = Executors.newScheduledThreadPool(5);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        try {
            packInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException ex) {
            Log.e(TAG, "Can't get package info", ex);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();

        if (action.equals(Constants.ACTION_CONNECT)) {
            boolean force = intent.getBooleanExtra("force", false);
            boolean async = intent.getBooleanExtra("async", true);
            final String hostname = intent.getStringExtra("hostname");
            final int port = intent.getIntExtra("port", 9982);
            final String username = intent.getStringExtra("username");
            final String password = intent.getStringExtra("password");

            if (connection != null && force) {
                connection.close();
            }
            if (connection == null || !connection.isConnected()) {
                final TVHClientApplication app = (TVHClientApplication) getApplication();
                app.clearAll();
                app.setLoading(true);
                connection = new HTSConnection(this, packInfo.packageName, packInfo.versionName, async);

                // Since this is blocking, spawn to a new thread
                execService.execute(new Runnable() {
                    public void run() {
                        connection.open(hostname, port);
                        connection.authenticate(username, password);
                    }
                });
            }

        } else if (connection == null || !connection.isConnected()) {
            Log.e(TAG, "No connection to perform " + action);

        } else if (action.equals(Constants.ACTION_DISCONNECT)) {
            connection.close();

        } else if (action.equals(Constants.ACTION_GET_EVENT)) {
            getEvent(intent.getLongExtra("eventId", 0));

        } else if (action.equals(Constants.ACTION_GET_EVENTS)) {
            TVHClientApplication app = (TVHClientApplication) getApplication();
            final Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            getEvents(ch, intent.getLongExtra("eventId", 0), intent.getIntExtra("count", 10));

        } else if (action.equals(Constants.ACTION_ADD_DVR_ENTRY)) {
            TVHClientApplication app = (TVHClientApplication) getApplication();
            Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            addDvrEntry(ch, intent.getLongExtra("eventId", 0));

        } else if (action.equals(Constants.ACTION_DELETE_DVR_ENTRY)) {
            deleteDvrEntry(Integer.valueOf(intent.getStringExtra("id")));

        } else if (action.equals(Constants.ACTION_CANCEL_DVR_ENTRY)) {
            cancelDvrEntry(intent.getLongExtra("id", 0));

        } else if (action.equals(Constants.ACTION_ADD_TIMER_REC_ENTRY)) {
            addTimerRecEntry(
                intent.getStringExtra("title"),
                intent.getLongExtra("start", 0), 
                intent.getLongExtra("stop", 0), 
                intent.getLongExtra("channelId", 0), 
                intent.getStringExtra("configName"),
                intent.getLongExtra("retention", 0), 
                intent.getLongExtra("daysOfWeek", 0), 
                intent.getLongExtra("priority", 0), 
                intent.getLongExtra("enabled", 0), 
                intent.getStringExtra("name"),
                intent.getStringExtra("directory"));

        } else if (action.equals(Constants.ACTION_DELETE_TIMER_REC_ENTRY)) {
            deleteTimerRecEntry(intent.getStringExtra("id"));

        } else if (action.equals(Constants.ACTION_EPG_QUERY)) {
            TVHClientApplication app = (TVHClientApplication) getApplication();
            Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            epgQuery(ch, intent.getStringExtra("query"), intent.getLongExtra("tagId", 0));

        } else if (action.equals(Constants.ACTION_SUBSCRIBE)) {
            subscribe(intent.getLongExtra("channelId", 0),
                    intent.getLongExtra("subscriptionId", 0),
                    intent.getIntExtra("maxWidth", 0),
                    intent.getIntExtra("maxHeight", 0),
                    intent.getStringExtra("audioCodec"),
                    intent.getStringExtra("videoCodec"));

        } else if (action.equals(Constants.ACTION_UNSUBSCRIBE)) {
            unsubscribe(intent.getLongExtra("subscriptionId", 0));

        } else if (action.equals(Constants.ACTION_FEEDBACK)) {
            feedback(intent.getLongExtra("subscriptionId", 0), intent.getIntExtra("speed", 0));

        } else if (action.equals(Constants.ACTION_GET_TICKET)) {
            TVHClientApplication app = (TVHClientApplication) getApplication();
            Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            Recording rec = app.getRecording(intent.getLongExtra("dvrId", 0));
            if (ch != null) {
                getTicket(ch);
            } else if (rec != null) {
                getTicket(rec);
            }

        } else if (action.equals(Constants.ACTION_GET_DISC_SPACE)) {
        	getDiscSpace();

        } else if (action.equals(Constants.ACTION_GET_DVR_CONFIG)) {
            getDvrConfigs();
            
        } else if (action.equals(Constants.ACTION_GET_PROFILES)) {
            getProfiles();
            
        } else if (action.equals(Constants.ACTION_GET_CHANNEL)) {
            TVHClientApplication app = (TVHClientApplication) getApplication();
            Channel ch = app.getChannel(intent.getLongExtra("channelId", 0));
            if (ch != null) {
                getChannel(ch);
            }
            
        } else if (action.equals(Constants.ACTION_SUBSCRIBE_FILTER_STREAM)) {
            subscriptionFilterStream();
            
        } else if (action.equals(Constants.ACTION_GET_DVR_CUTPOINTS)) {
            TVHClientApplication app = (TVHClientApplication) getApplication();
            Recording rec = app.getRecording(intent.getLongExtra("dvrId", 0));
            if (rec != null) {
                getDvrCutpoints(rec);
            }
            
        } else if (action.equals(Constants.ACTION_ADD_SERIES_DVR_ENTRY)) {
            addAutorecEntry(
                    intent.getStringExtra("title"), 
                    intent.getLongExtra("channelId", 0),
                    intent.getStringExtra("configName"),
                    intent.getLongExtra("maxDuration", 0),
                    intent.getLongExtra("minDuration", 0),
                    intent.getLongExtra("retention", 0), 
                    intent.getLongExtra("daysOfWeek", 127), 
                    intent.getLongExtra("priority", 0), 
                    intent.getLongExtra("enabled", 1), 
                    intent.getLongExtra("startExtra", 0),
                    intent.getLongExtra("stopExtra", 0),
                    intent.getStringExtra("name"),
                    intent.getStringExtra("directory"));

        } else if (action.equals(Constants.ACTION_DELETE_SERIES_DVR_ENTRY)) {
            String id = intent.getStringExtra("id");
            deleteAutorecEntry(id);

        } else if (action.equals(Constants.ACTION_GET_SYSTEM_TIME)) {
            getSystemTime();

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

    public void onError(final String error) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.setLoading(false);
        app.setConnectionState(error);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final IBinder mBinder = new LocalBinder();

    private void onTagAdd(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        ChannelTag tag = new ChannelTag();
        tag.id = msg.getLong("tagId");
        tag.name = msg.getString("tagName", "");
        tag.icon = msg.getString("tagIcon", "");
        app.addChannelTag(tag);
        if (tag.icon != null) {
            getChannelTagIcon(tag);
        }
    }

    private void onTagUpdate(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        ChannelTag tag = app.getChannelTag(msg.getLong("tagId"));
        if (tag == null) {
            return;
        }

        tag.name = msg.getString("tagName", tag.name);
        String icon = msg.getString("tagIcon", tag.icon);
        if (icon == null) {
            tag.icon = null;
            tag.iconBitmap = null;
        } else if (!icon.equals(tag.icon)) {
            tag.icon = icon;
            getChannelTagIcon(tag);
        }
    }

    private void onTagDelete(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeChannelTag(msg.getLong("tagId"));
    }

    private void onChannelAdd(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        final Channel ch = new Channel();
        ch.id = msg.getLong("channelId");
        ch.name = msg.getString("channelName", "");
        ch.number = msg.getInt("channelNumber", 0);

        // The default values will be set in case a server with a htsp API
        // version 12 or lower is used
        ch.numberMinor = msg.getInt("channelNumberMinor", 0);

        ch.icon = msg.getString("channelIcon", "");
        ch.tags = msg.getIntList("tags", ch.tags);

        if (ch.number == 0) {
            ch.number = (int) (ch.id + 25000);
        }

        app.addChannel(ch);
        if (ch.icon != null) {
            getChannelIcon(ch);
        }
        long currEventId = msg.getLong("eventId", 0);
        long nextEventId = msg.getLong("nextEventId", 0);

        ch.isTransmitting = (currEventId != 0);

        if (currEventId > 0) {
            getEvents(ch, currEventId, 5);
        } else if (nextEventId > 0) {
            getEvents(ch, nextEventId, 5);
        }
    }

    private void onChannelUpdate(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        final Channel ch = app.getChannel(msg.getLong("channelId"));
        if (ch == null) {
            return;
        }

        ch.name = msg.getString("channelName", ch.name);
        ch.number = msg.getInt("channelNumber", ch.number);

        // The default values will be set in case a server with a htsp API
        // version 12 or lower is used
        ch.numberMinor = msg.getInt("channelNumberMinor", 0);

        String icon = msg.getString("channelIcon", ch.icon);
        ch.tags = msg.getIntList("tags", ch.tags);

        if (icon == null) {
            ch.icon = null;
            ch.iconBitmap = null;
        } else if (!icon.equals(ch.icon)) {
            ch.icon = icon;
            getChannelIcon(ch);
        }
        // Remove programs that have ended
        long currEventId = msg.getLong("eventId", 0);
        long nextEventId = msg.getLong("nextEventId", 0);

        ch.isTransmitting = currEventId != 0;

        Iterator<Program> it = ch.epg.iterator();
        ArrayList<Program> tmp = new ArrayList<Program>();

        while (it.hasNext() && currEventId > 0) {
            Program p = it.next();
            if (p.id != currEventId) {
                tmp.add(p);
            } else {
                break;
            }
        }
        ch.epg.removeAll(tmp);

        for (Program p : tmp) {
            app.removeProgram(p);
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

    private void onChannelDelete(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeChannel(msg.getLong("channelId"));
    }

    private void onDvrEntryAdd(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        Recording rec = new Recording();
        rec.id = msg.getLong("id");

        rec.eventId = msg.getLong("eventId", 0);
        rec.autorecId = msg.getString("autorecId", "");
        rec.timerecId = msg.getString("timerecId", "");
        rec.start = msg.getDate("start");
        rec.stop = msg.getDate("stop");
        rec.startExtra = msg.getDate("startExtra");
        rec.stopExtra = msg.getDate("stopExtra");
        rec.retention = msg.getLong("retention");
        rec.priority = msg.getLong("priority");
        rec.contentType = msg.getLong("contentType");
        rec.title = msg.getString("title", "");
        rec.description = msg.getString("description", "");
        rec.owner = msg.getString("owner", "");
        rec.creator = msg.getString("creator", "");
        rec.path = msg.getString("path", "");
        rec.state = msg.getString("state", "");
        rec.error = msg.getString("error", null);

        rec.channel = app.getChannel(msg.getLong("channel", 0));
        if (rec.channel != null) {
            rec.channel.recordings.add(rec);
        }

        app.addRecording(rec);
    }

    private void onDvrEntryUpdate(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        Recording rec = app.getRecording(msg.getLong("id"));
        if (rec == null) {
            return;
        }

        // Get the current recording state to check if a notification shall be shown
        String currentRecState = rec.state;

        rec.eventId = msg.getLong("eventId", rec.eventId);
        rec.autorecId = msg.getString("autorecId", rec.autorecId);
        rec.timerecId = msg.getString("timerecId", rec.timerecId);
        rec.start = msg.getDate("start");
        rec.stop = msg.getDate("stop");
        rec.startExtra = msg.getDate("startExtra");
        rec.stopExtra = msg.getDate("stopExtra");
        rec.retention = msg.getLong("retention");
        rec.priority = msg.getLong("priority");
        rec.contentType = msg.getLong("contentType");
        rec.title = msg.getString("title", rec.title);
        rec.description = msg.getString("description", rec.description);
        rec.owner = msg.getString("owner", rec.owner);
        rec.creator = msg.getString("creator", rec.creator);
        rec.path = msg.getString("path", rec.path);
        rec.state = msg.getString("state", rec.state);
        rec.error = msg.getString("error", rec.error);

        app.updateRecording(rec);

        // Check that the notification shall be shown
        boolean showNotification = prefs.getBoolean("showNotificationsPref", false);

        // Show a notification if enabled that the recording has either started or completed
        if (showNotification && notificationManager != null && currentRecState != rec.state) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.notification_icon);

            if (rec.state.equals("recording")) {
                builder.setContentTitle(getString(R.string.recording_started));
                builder.setContentText(getString(R.string.recording_started_text, rec.title));
                notificationManager.notify(1, builder.build());

            } else if (rec.state.equals("completed")) {
                builder.setContentTitle(getString(R.string.recording_completed));
                builder.setContentText(getString(R.string.recording_completed_text, rec.title));
                notificationManager.notify(1, builder.build());
            }
        }
    }

    private void onDvrEntryDelete(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        Recording rec = app.getRecording(msg.getLong("id"));

        if (rec == null || rec.channel == null) {
            return;
        }

        rec.channel.recordings.remove(rec);
        for (Program p : rec.channel.epg) {
            if (p.recording == rec) {
                p.recording = null;
                app.updateProgram(p);
                break;
            }
        }
        app.removeRecording(rec);
    }

    private void onTimerRecEntryAdd(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        TimerRecording rec = new TimerRecording();
        rec.id = msg.getString("id", "");
        rec.enabled = (msg.getLong("enabled", 0) == 0) ? false : true;
        rec.daysOfWeek = msg.getLong("daysOfWeek", 0);
        rec.retention = msg.getLong("retention", 0);
        rec.priority = msg.getLong("priority", 0);
        rec.start = msg.getLong("start");
        rec.stop = msg.getLong("stop");
        rec.title = msg.getString("title", "");
        rec.name = msg.getString("name", "");
        rec.directory = msg.getString("directory", "");
        rec.owner = msg.getString("owner", "");
        rec.creator = msg.getString("creator", "");
        rec.channel = app.getChannel(msg.getLong("channel", 0));
        app.addTimerRecording(rec);
    }

    private void onTimerRecEntryUpdate(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        TimerRecording rec = app.getTimerRecording(msg.getString("id"));
        if (rec == null) {
            return;
        }

        rec.enabled = (msg.getLong("enabled", 0) == 0) ? false : true;
        rec.daysOfWeek = msg.getLong("daysOfWeek", rec.daysOfWeek);
        rec.retention = msg.getLong("retention", rec.retention);
        rec.priority = msg.getLong("priority", rec.priority);
        rec.start = msg.getLong("start", rec.start);
        rec.stop = msg.getLong("stop", rec.stop);
        rec.title = msg.getString("title", rec.title);
        rec.name = msg.getString("name", rec.name);
        rec.directory = msg.getString("directory", rec.directory);
        rec.owner = msg.getString("owner", rec.owner);
        rec.creator = msg.getString("creator", rec.creator);
        rec.channel = app.getChannel(msg.getLong("channel", 0));
        app.updateTimerRecording(rec);
    }

    private void onTimerRecEntryDelete(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        TimerRecording rec = app.getTimerRecording(msg.getString("id"));

        if (rec == null || rec.channel == null) {
            return;
        }

        rec.channel = null;
        app.removeTimerRecording(rec);
    }

    private void onInitialSyncCompleted(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.setLoading(false);
        app.setConnectionState(Constants.ACTION_CONNECTION_STATE_OK);
        app.setProtocolVersion(connection.getProtocolVersion());
    }

    private void onSubscriptionStart(HTSMessage msg) {
        Log.d(TAG, "onSubscriptionStart");

        TVHClientApplication app = (TVHClientApplication) getApplication();
        Subscription subscription = app.getSubscription(msg.getLong("subscriptionId"));
        if (subscription == null) {
            return;
        }

        for (Object obj : msg.getList("streams")) {
            Stream s = new Stream();
            HTSMessage sub = (HTSMessage) obj;
            s.index = sub.getInt("index");
            s.type = sub.getString("type");
            s.language = sub.getString("language", "");
            s.width = sub.getInt("width", 0);
            s.height = sub.getInt("height", 0);
            s.duration = sub.getInt("duration", 0);
            s.aspectNum = sub.getInt("aspect_num", 0);
            s.aspectDen = sub.getInt("aspect_den", 0);
            s.autioType = sub.getInt("autio_type", 0);
            s.channels = sub.getInt("channels", 0);
            s.rate = sub.getInt("rate", 0);
            subscription.streams.add(s);

            Log.d(TAG, "onSubscriptionStart, added stream " + s.index);
        }

        if (msg.containsField("sourceinfo")) {
            Object obj = msg.get("sourceinfo");
            HTSMessage sub = (HTSMessage) obj;
            SourceInfo si = new SourceInfo();
            si.adapter = sub.getString("adapter", "");
            si.mux = sub.getString("mux", "");
            si.network = sub.getString("network", "");
            si.provider = sub.getString("provider", "");
            si.service = sub.getString("service", "");
            subscription.sourceInfo = si;

            Log.d(TAG, "onSubscriptionStart, added sourceinfo " + si.adapter);
        }
    }

    private void onSubscriptionStatus(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        Subscription s = app.getSubscription(msg.getLong("subscriptionId"));
        if (s == null) {
            return;
        }
        String status = msg.getString("status", "");
        if (s.status == null ? status != null : !s.status.equals(status)) {
            s.status = status;
            app.updateSubscription(s);
        }
    }

    private void onSubscriptionStop(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        Subscription s = app.getSubscription(msg.getLong("subscriptionId"));
        if (s == null) {
            return;
        }
        String status = msg.getString("status", "");
        if (s.status == null ? status != null : !s.status.equals(status)) {
            s.status = status;
            app.updateSubscription(s);
        }
        app.removeSubscription(s);
    }

    private void onSubscriptionGrace(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        Subscription s = app.getSubscription(msg.getLong("subscriptionId"));
        if (s == null) {
            return;
        }
        long gt = msg.getLong("graceTimeout", 0);
        if (s.graceTimeout != gt) {
            s.graceTimeout = gt;
            app.updateSubscription(s);
        }
    }

    private void onSubscriptionSignalStatus(HTSMessage msg) {
        Log.d(TAG, "onSubscriptionSignalStatus");

        TVHClientApplication app = (TVHClientApplication) getApplication();
        Subscription s = app.getSubscription(msg.getLong("subscriptionId"));
        if (s == null) {
            return;
        }
        s.feStatus = msg.getString("feStatus");
        s.feSNR = msg.getInt("feSNR", 0);
        s.feSignal = msg.getInt("feSignal", 0);
        s.feBER = msg.getInt("feBER", 0);
        s.feUNC = msg.getInt("feUNC", 0);
    }

    private void onMuxPacket(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        Subscription sub = app.getSubscription(msg.getLong("subscriptionId"));
        if (sub == null) {
            return;
        }

        Packet packet = new Packet();
        packet.dts = msg.getLong("dts", 0);
        packet.pts = msg.getLong("pts", 0);
        packet.duration = msg.getLong("duration");
        packet.frametype = msg.getInt("frametype");
        packet.payload = msg.getByteArray("payload");

        for (Stream st : sub.streams) {
            if (st.index == msg.getInt("stream")) {
                packet.stream = st;
            }
        }
        packet.subscription = sub;
        app.broadcastPacket(packet);
    }

    private void onQueueStatus(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        Subscription sub = app.getSubscription(msg.getLong("subscriptionId"));
        if (sub == null) {
            return;
        }
        if (msg.containsField("delay")) {
            BigInteger delay = msg.getBigInteger("delay");
            delay = delay.divide(BigInteger.valueOf((1000)));
            sub.delay = delay.longValue();
        }
        sub.droppedBFrames = msg.getLong("Bdrops", sub.droppedBFrames);
        sub.droppedIFrames = msg.getLong("Idrops", sub.droppedIFrames);
        sub.droppedPFrames = msg.getLong("Pdrops", sub.droppedPFrames);
        sub.packetCount = msg.getLong("packets", sub.packetCount);
        sub.queSize = msg.getLong("bytes", sub.queSize);

        app.updateSubscription(sub);
    }

    private void onAutorecEntryDelete(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        String id = msg.getString("id");
        if (id == null) {
            return;
        }
        // Remove the series recording from the list and also update all
        // recordings by removing the series id
        app.removeSeriesRecording(id);
        for (Recording rec : app.getRecordings()) {
            if (rec.autorecId != null && rec.autorecId.equals(id)) {
                rec.autorecId = null;
            }
        }
    }

    private void onAutorecEntryUpdate(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        SeriesRecording rec = app.getSeriesRecording(msg.getString("id"));
        if (rec == null) {
            return;
        }

        rec.enabled = (msg.getLong("enabled", 0) == 0) ? false : true;
        rec.maxDuration = msg.getLong("maxDuration");
        rec.minDuration = msg.getLong("minDuration");
        rec.retention = msg.getLong("retention");
        rec.daysOfWeek = msg.getLong("daysOfWeek");
        rec.approxTime = msg.getLong("approxTime");
        rec.start = msg.getLong("start");
        rec.startWindow = msg.getLong("startWindow");
        rec.priority = msg.getLong("priority");
        rec.startExtra = msg.getLong("startExtra");
        rec.stopExtra = msg.getLong("stopExtra");
        rec.title = msg.getString("title", rec.title);
        rec.name = msg.getString("name", rec.name);
        rec.directory = msg.getString("directory", rec.directory);
        rec.owner = msg.getString("owner", rec.owner);
        rec.creator = msg.getString("creator", rec.creator);
        app.updateSeriesRecording(rec);
    }

    private void onAutorecEntryAdd(HTSMessage msg) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        SeriesRecording rec = new SeriesRecording();
        rec.id = msg.getString("id");
        rec.enabled = (msg.getLong("enabled", 0) == 0) ? false : true;
        rec.maxDuration = msg.getLong("maxDuration");
        rec.minDuration = msg.getLong("minDuration");
        rec.retention = msg.getLong("retention");
        rec.daysOfWeek = msg.getLong("daysOfWeek");
        rec.approxTime = msg.getLong("approxTime");
        rec.start = msg.getLong("start");
        rec.startWindow = msg.getLong("startWindow");
        rec.priority = msg.getLong("priority");
        rec.startExtra = msg.getLong("startExtra");
        rec.stopExtra = msg.getLong("stopExtra");
        rec.title = msg.getString("title", "");
        rec.name = msg.getString("name", "");
        rec.directory = msg.getString("directory", "");
        rec.owner = msg.getString("owner", "");
        rec.creator = msg.getString("creator", "");
        rec.channel = app.getChannel(msg.getLong("channel", 0));
        app.addSeriesRecording(rec);
    }

    public void onMessage(HTSMessage msg) {
        String method = msg.getMethod();
        if (method.equals("tagAdd")) {
            onTagAdd(msg);
        } else if (method.equals("tagUpdate")) {
            onTagUpdate(msg);
        } else if (method.equals("tagDelete")) {
            onTagDelete(msg);
        } else if (method.equals("channelAdd")) {
            onChannelAdd(msg);
        } else if (method.equals("channelUpdate")) {
            onChannelUpdate(msg);
        } else if (method.equals("channelDelete")) {
            onChannelDelete(msg);
        } else if (method.equals("initialSyncCompleted")) {
            onInitialSyncCompleted(msg);
        } else if (method.equals("dvrEntryAdd")) {
            onDvrEntryAdd(msg);
        } else if (method.equals("dvrEntryUpdate")) {
            onDvrEntryUpdate(msg);
        } else if (method.equals("dvrEntryDelete")) {
            onDvrEntryDelete(msg);
        } else if (method.equals("timerecEntryAdd")) {
            onTimerRecEntryAdd(msg);
        } else if (method.equals("timerecEntryUpdate")) {
            onTimerRecEntryUpdate(msg);
        } else if (method.equals("timerecEntryDelete")) {
            onTimerRecEntryDelete(msg);
        } else if (method.equals("subscriptionStart")) {
            onSubscriptionStart(msg);
        } else if (method.equals("subscriptionStatus")) {
            onSubscriptionStatus(msg);
        } else if (method.equals("subscriptionStop")) {
            onSubscriptionStop(msg);
        } else if (method.equals("subscriptionGrace")) {
            onSubscriptionGrace(msg);
        } else if (method.equals("muxpkt")) {
            onMuxPacket(msg);
        } else if (method.equals("queueStatus")) {
            onQueueStatus(msg);
        } else if (method.equals("autorecEntryAdd")) {
            onAutorecEntryAdd(msg);
        } else if (method.equals("autorecEntryUpdate")) {
            onAutorecEntryUpdate(msg);
        } else if (method.equals("autorecEntryDelete")) {
            onAutorecEntryDelete(msg);
        } else if (method.equals("signalStatus")) {
            onSubscriptionSignalStatus(msg);
        } else {
            Log.d(TAG, method.toString());
        }
    }

    public String hashString(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Can't create hash string", e);
        }

        return "";
    }

    public void cacheImage(String url, File f) throws MalformedURLException, IOException {
        InputStream is;
        
        if (url.startsWith("http")) {
        	is = new BufferedInputStream(new URL(url).openStream());
        } else if (connection.getProtocolVersion() > 9){
        	is = new HTSFileInputStream(connection, url);
        } else {
        	Log.d(TAG, "Unhandled url: " + url);
        	return;
        }
        
        OutputStream os = new FileOutputStream(f);

        float scale = getResources().getDisplayMetrics().density;
        int width = (int) (64 * scale);
        int height = (int) (64 * scale);

        // Set the options for a bitmap and decode an input stream into a bitmap
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, o);
        is.close();

        if (url.startsWith("http")) {
            is = new BufferedInputStream(new URL(url).openStream());
        } else if (connection.getProtocolVersion() > 9) {
            is = new HTSFileInputStream(connection, url);
        }

        // Set the sample size of the image. This is the number of pixels in
        // either dimension that correspond to a single pixel in the decoded
        // bitmap. For example, inSampleSize == 4 returns an image that is 1/4
        // the width/height of the original, and 1/16 the number of pixels.
        int ratio = Math.max(o.outWidth / width, o.outHeight / height);
        int sampleSize = Integer.highestOneBit((int) Math.floor(ratio));
        o = new BitmapFactory.Options();
        o.inSampleSize = sampleSize;

        // Now decode an input stream into a bitmap and compress it.
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, o);
        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        }
        os.close();
        is.close();
    }

    private Bitmap getIcon(final String url) throws MalformedURLException, IOException {

        // When no channel icon shall be shown return null instead of the icon.
        // The icon will not be shown anyway, so returning null will drastically
        // reduce memory consumption.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean showIcons = prefs.getBoolean("showIconPref", true);
        if (!showIcons) {
            return null; 
        }

        if (url == null || url.length() == 0) {
            return null;
        }

        File dir = getCacheDir();
        File f = new File(dir, hashString(url) + ".png");

        if (!f.exists()) {
            cacheImage(url, f);
        }

        return BitmapFactory.decodeFile(f.toString());
    }

    private void getChannelIcon(final Channel ch) {
        execService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ch.iconBitmap = getIcon(ch.icon);
                    TVHClientApplication app = (TVHClientApplication) getApplication();
                    app.updateChannel(ch);
                } catch (Throwable ex) {
//                    Log.e(TAG, "Can't load channel icon", ex);
                }
            }
        });
    }

    private void getChannelTagIcon(final ChannelTag tag) {
        execService.execute(new Runnable() {
            public void run() {
                try {
                    tag.iconBitmap = getIcon(tag.icon);
                    TVHClientApplication app = (TVHClientApplication) getApplication();
                    app.updateChannelTag(tag);
                } catch (Throwable ex) {
//                    Log.e(TAG, "Can't load tag icon", ex);
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

                TVHClientApplication app = (TVHClientApplication) getApplication();

                for (Object obj : response.getList("events")) {
                    Program p = new Program();
                    HTSMessage sub = (HTSMessage) obj;
                    p.id = sub.getLong("eventId", 0);
                    p.nextId = sub.getLong("nextEventId", 0);
                    p.description = sub.getString("description", "");
                    p.summary = sub.getString("summary", "");
                    p.recording = app.getRecording(sub.getLong("dvrId", 0));
                    p.contentType = sub.getInt("contentType", 0);
                    p.title = sub.getString("title");
                    p.start = sub.getDate("start");
                    p.stop = sub.getDate("stop");
                    p.seriesInfo = buildSeriesInfo(sub);
                    p.starRating = sub.getInt("starRating", -1);
                    p.channel = ch;

                    if (ch.epg.add(p)) {
                        app.addProgram(p);
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
                TVHClientApplication app = (TVHClientApplication) getApplication();
                Channel ch = app.getChannel(response.getLong("channelId"));
                Program p = new Program();
                p.id = response.getLong("eventId");
                p.nextId = response.getLong("nextEventId", 0);
                p.description = response.getString("description", "");
                p.summary = response.getString("summary", "");
                p.recording = app.getRecording(response.getLong("dvrId", 0));
                p.contentType = response.getInt("contentType", 0);
                p.title = response.getString("title");
                p.start = response.getDate("start");
                p.stop = response.getDate("stop");
                p.seriesInfo = buildSeriesInfo(response);
                p.starRating = response.getInt("starRating", -1);
                p.channel = ch;

                if (ch.epg.add(p)) {
                    app.addProgram(p);
                    app.updateChannel(ch);
                }
            }
        });
    }

    private SeriesInfo buildSeriesInfo(HTSMessage msg) {
        SeriesInfo info = new SeriesInfo();
        info.episodeCount = msg.getInt("episodeCount", 0);
        info.episodeNumber = msg.getInt("episodeNumber", 0);
        info.onScreen = msg.getString("episodeOnscreen", "");
        info.partCount = msg.getInt("partCount", 0);
        info.partNumber = msg.getInt("partNumber", 0);
        info.seasonCount = msg.getInt("seasonCount", 0);
        info.seasonNumber = msg.getInt("seasonNumber", 0);
        return info;
    }
	
    private void epgQuery(final Channel ch, String query, long tagId) {
        HTSMessage request = new HTSMessage();
        request.setMethod("epgQuery");
        request.putField("query", query);

        // The default values will be set in case a server with a htsp API
        // version 12 or lower is used
        request.putField("minduration", 0);
        request.putField("maxduration", Integer.MAX_VALUE);

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
                @SuppressWarnings("unused")
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
                @SuppressWarnings("unused")
                boolean success = response.getInt("success", 0) == 1;
            }
        });
    }

    private void addDvrEntry(final Channel ch, final long eventId) {
        HTSMessage request = new HTSMessage();
        request.setMethod("addDvrEntry");
        request.putField("eventId", eventId);
        request.putField("retention", 0);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                if (response.getInt("success", 0) == 1) {
                    for (Program p : ch.epg) {
                        if (p.id == eventId) {
                            TVHClientApplication app = (TVHClientApplication) getApplication();
                            p.recording = app.getRecording(response.getLong("id", 0));
                            app.updateProgram(p);
                            break;
                        }
                    }
                }
                @SuppressWarnings("unused")
                String error = response.getString("error", "");
            }
        });
    }

    private void deleteTimerRecEntry(String id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("deleteTimerecEntry");
        request.putField("id", id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                @SuppressWarnings("unused")
                boolean success = response.getInt("success", 0) == 1;
            }
        });
    }

    /**
     * 
     * @param title
     * @param start
     * @param stop
     * @param channelId
     * @param configName
     * @param retention
     * @param daysOfWeek
     * @param priority
     * @param enabled
     * @param name
     * @param directory
     */
    private void addTimerRecEntry(String title, long start, long stop,
                long channelId, String configName, long retention, long daysOfWeek,
                long priority, long enabled, String name, String directory) {

        HTSMessage request = new HTSMessage();
        request.setMethod("addTimerecEntry");
        request.putField("title", title);
        request.putField("start", start);
        request.putField("stop", stop);
        request.putField("channelId", channelId);
        request.putField("configName", configName);
        request.putField("retention", retention);
        request.putField("daysOfWeek", daysOfWeek);
        request.putField("priority", priority);
        request.putField("enabled", enabled);
        request.putField("name", name);
        request.putField("directory", directory);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                @SuppressWarnings("unused")
                boolean success = response.getInt("success", 0) == 1;
                @SuppressWarnings("unused")
                String error = response.getString("error", "");
            }
        });
    }

    private void subscribe(long channelId, long subscriptionId, int maxWidth, int maxHeight, String aCodec, String vCodec) {
        Subscription subscription = new Subscription();
        subscription.id = subscriptionId;
        subscription.status = "Subscribing";

        TVHClientApplication app = (TVHClientApplication) getApplication();
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
        TVHClientApplication app = (TVHClientApplication) getApplication();
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
                String path = response.getString("path", "");
                String ticket = response.getString("ticket", "");
                String webroot = connection.getWebRoot();

                if (path != null && ticket != null) {
                    TVHClientApplication app = (TVHClientApplication) getApplication();
                    app.addTicket(new HttpTicket(webroot + path, ticket));
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
                String path = response.getString("path", "");
                String ticket = response.getString("ticket", "");

                if (path != null && ticket != null) {
                    TVHClientApplication app = (TVHClientApplication) getApplication();
                    app.addTicket(new HttpTicket(path, ticket));
                }
            }
        });
    }
    
    private void getDiscSpace() {
        HTSMessage request = new HTSMessage();
        request.setMethod("getDiskSpace");
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                TVHClientApplication app = (TVHClientApplication) getApplication();
                app.updateStatus("freediskspace", response.getString("freediskspace", ""));
                app.updateStatus("totaldiskspace", response.getString("totaldiskspace", ""));
            }
        });
    }

    private void getSystemTime() {
        HTSMessage request = new HTSMessage();
        request.setMethod("getSysTime");
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                TVHClientApplication app = (TVHClientApplication) getApplication();
                app.updateStatus("time", response.getString("time", ""));
                app.updateStatus("timezone", response.getString("timezone", ""));
            }
        });
    }

    private void getDvrConfigs() {
        HTSMessage request = new HTSMessage();
        request.setMethod("getDvrConfigs");
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                if (!response.containsKey("dvrconfigs")) {
                    return;
                }
                List<Profiles> pList = new ArrayList<Profiles>();
                TVHClientApplication app = (TVHClientApplication) getApplication();
                for (Object obj : response.getList("dvrconfigs")) {
                    HTSMessage sub = (HTSMessage) obj;

                    Profiles p = new Profiles();
                    p.uuid = sub.getString("uuid");
                    p.name = sub.getString("name");
                    if (p.name.length() == 0) {
                        p.name = Constants.REC_PROFILE_DEFAULT;
                    }
                    p.comment = sub.getString("comment");
                    pList.add(p);
                }
                app.addDvrConfigs(pList);
            }
        });
    }

    private void deleteAutorecEntry(final String id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("deleteAutorecEntry");
        request.putField("id", id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                @SuppressWarnings("unused")
                boolean success = response.getInt("success", 0) == 1;
            }
        });
    }

    /**
     * 
     * @param title
     * @param channelId
     * @param configName
     * @param maxDuration
     * @param minDuration
     * @param retention
     * @param daysOfWeek
     * @param priority
     * @param enabled
     * @param startExtra
     * @param stopExtra
     * @param name
     * @param directory
     */
    private void addAutorecEntry(String title, long channelId, String configName, long maxDuration,
            long minDuration, long retention, long daysOfWeek, long priority, long enabled,
            long startExtra, long stopExtra, String name, String directory) {

        HTSMessage request = new HTSMessage();
        request.setMethod("addAutorecEntry");
        request.putField("title", title);
        request.putField("configName", configName);
        request.putField("channelId", channelId);
        request.putField("minDuration", minDuration);
        request.putField("maxDuration", maxDuration);
        request.putField("retention", retention);
        request.putField("daysOfWeek", daysOfWeek);
        request.putField("priority", priority);
        request.putField("enabled", enabled);
        request.putField("startExtra", startExtra);
        request.putField("stopExtra", stopExtra);
        request.putField("name", name);
        request.putField("directory", directory);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                @SuppressWarnings("unused")
                boolean success = response.getInt("success", 0) == 1;
            }
        });
    }

    private void getDvrCutpoints(final Recording rec) {
        Log.d(TAG, "getDvrCutpoints, rec " + rec.title);

        HTSMessage request = new HTSMessage();
        request.setMethod("getDvrCutpoints");
        request.putField("id", rec.id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                if (!response.containsKey("cutpoints")) {
                    return;
                }
                // Clear all saved cut points before adding new ones.
                rec.dvrCutPoints.clear();

                for (Object obj : response.getList("cutpoints")) {
                    DvrCutpoint dc = new DvrCutpoint();
                    HTSMessage sub = (HTSMessage) obj;
                    dc.start = sub.getInt("start");
                    dc.end = sub.getInt("end");
                    dc.type = sub.getInt("type");
                    rec.dvrCutPoints.add(dc);

                    Log.d(TAG, "getDvrCutpoints, added cut point for rec " + rec.title);
                }
            }
        });
    }

    private void subscriptionFilterStream() {
        // TODO Auto-generated method stub
        
    }

    private void getChannel(final Channel ch) {
        HTSMessage request = new HTSMessage();
        request.setMethod("getChannel");
        request.putField("channelId", ch.id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                // TODO
                
            }
        });
    }

    private void getProfiles() {
        HTSMessage request = new HTSMessage();
        request.setMethod("getProfiles");
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                if (!response.containsKey("profiles")) {
                    return;
                }
                List<Profiles> pList = new ArrayList<Profiles>();
                TVHClientApplication app = (TVHClientApplication) getApplication();
                for (Object obj : response.getList("profiles")) {
                    HTSMessage sub = (HTSMessage) obj;

                    Profiles p = new Profiles();
                    p.uuid = sub.getString("uuid");
                    p.name = sub.getString("name");
                    p.comment = sub.getString("comment");
                    pList.add(p);
                }
                app.addProfiles(pList);
            }
        });
    }
}
