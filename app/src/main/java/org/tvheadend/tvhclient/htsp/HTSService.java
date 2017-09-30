package org.tvheadend.tvhclient.htsp;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.MiscUtils;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.data.DataContract;
import org.tvheadend.tvhclient.interfaces.HTSConnectionListener;
import org.tvheadend.tvhclient.interfaces.HTSResponseHandler;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.DiscSpace;
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
import org.tvheadend.tvhclient.model.SystemTime;
import org.tvheadend.tvhclient.model.TimerRecording;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HTSService extends Service implements HTSConnectionListener {

    private static final String TAG = HTSService.class.getSimpleName();

    private ScheduledExecutorService execService;
    private HTSConnection connection;
    private PackageInfo packInfo;
    private DataStorage ds;
    private Logger logger;
    private Connection mAccount;


    private class LocalBinder extends Binder {
        HTSService getService() {
            return HTSService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");

        // TODO move this into a account or connection mananger
        Cursor c = getApplicationContext().getContentResolver().query(
                DataContract.Connections.CONTENT_URI,
                DataContract.Connections.PROJECTION_ALL,
                DataContract.Connections.SELECTED + "=?", new String[] {"1"}, null);

        Log.d(TAG, "onCreate: queried connection data");

        if (c != null && c.getCount() > 0) {
            Log.d(TAG, "onCreate: Reading connection data");
            c.moveToFirst();
            mAccount = new Connection();
            mAccount.address = c.getString(c.getColumnIndex(DataContract.Connections.ADDRESS));
            mAccount.port = c.getInt(c.getColumnIndex(DataContract.Connections.PORT));
            mAccount.username = c.getString(c.getColumnIndex(DataContract.Connections.USERNAME));
            mAccount.password = c.getString(c.getColumnIndex(DataContract.Connections.PASSWORD));
            c.close();
        }

        execService = Executors.newScheduledThreadPool(10);
        ds = DataStorage.getInstance();
        logger = Logger.getInstance();

        try {
            packInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException ex) {
            // NOP
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");

        final String action = intent.getAction();

        if (action.equals(Constants.ACTION_CONNECT)) {
            logger.log(TAG, "onStartCommand: Connection to server requested");

            boolean force = intent.getBooleanExtra("force", false);
            if (connection != null && force) {
                logger.log(TAG, "onStartCommand: Closing existing connection");
                connection.close();
                ds.clearAll();
            }
            if (mAccount != null && connection == null || !connection.isConnected()) {
                logger.log(TAG, "onStartCommand: Connecting to server");
                ds.setLoading(true);
                connection = new HTSConnection(TVHClientApplication.getInstance(), this, packInfo.packageName, packInfo.versionName);

                // Since this is blocking, spawn to a new thread
                execService.execute(new Runnable() {
                    public void run() {
                        connection.open(mAccount.address, mAccount.port, MiscUtils.isNetworkAvailable(getApplicationContext()));
                        connection.authenticate(mAccount.username, mAccount.password);
                    }
                });
            }

        } else if (connection == null || !connection.isConnected()) {
            logger.log(TAG, "onStartCommand: No connection to perform " + action);

        } else if (action.equals(Constants.ACTION_DISCONNECT)) {
            logger.log(TAG, "onStartCommand: Closing connection to server");
            connection.close();

        } else if (action.equals(Constants.ACTION_GET_EVENT)) {
            getEvent(intent.getLongExtra("eventId", 0));

        } else if (action.equals(Constants.ACTION_GET_EVENTS)) {
            final Channel ch = ds.getChannel(intent.getLongExtra("channelId", 0));
            getEvents(ch, intent.getLongExtra("eventId", 0), intent.getIntExtra("count", 10));

        } else if (action.equals(Constants.ACTION_ADD_DVR_ENTRY)) {
            addDvrEntry(intent);

        } else if (action.equals(Constants.ACTION_UPDATE_DVR_ENTRY)) {
            updateDvrEntry(intent);

        } else if (action.equals(Constants.ACTION_DELETE_DVR_ENTRY)) {
            try {
                deleteDvrEntry(Long.valueOf(intent.getStringExtra("id")));
            } catch (NumberFormatException ex) {
                // NOP
            }

        } else if (action.equals(Constants.ACTION_CANCEL_DVR_ENTRY)) {
            cancelDvrEntry(intent.getLongExtra("id", 0));

        } else if (action.equals(Constants.ACTION_STOP_DVR_ENTRY)) {
            stopDvrEntry(intent.getLongExtra("id", 0));

        } else if (action.equals(Constants.ACTION_ADD_TIMER_REC_ENTRY)) {
            addTimerRecEntry(intent);

        } else if (action.equals(Constants.ACTION_UPDATE_TIMER_REC_ENTRY)) {
            updateTimerRecEntry(intent);

        } else if (action.equals(Constants.ACTION_DELETE_TIMER_REC_ENTRY)) {
            deleteTimerRecEntry(intent.getStringExtra("id"));

        } else if (action.equals(Constants.ACTION_EPG_QUERY)) {
            epgQuery(intent);

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
            Channel ch = ds.getChannel(intent.getLongExtra("channelId", 0));
            Recording rec = ds.getRecording(intent.getLongExtra("dvrId", 0));
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
            Channel ch = ds.getChannel(intent.getLongExtra("channelId", 0));
            if (ch != null) {
                getChannel(ch);
            }

        } else if (action.equals(Constants.ACTION_SUBSCRIBE_FILTER_STREAM)) {
            subscriptionFilterStream();

        } else if (action.equals(Constants.ACTION_GET_DVR_CUTPOINTS)) {
            Recording rec = ds.getRecording(intent.getLongExtra("dvrId", 0));
            if (rec != null) {
                getDvrCutpoints(rec);
            }

        } else if (action.equals(Constants.ACTION_ADD_SERIES_DVR_ENTRY)) {
            addAutorecEntry(intent);

        } else if (action.equals(Constants.ACTION_UPDATE_SERIES_DVR_ENTRY)) {
            updateAutorecEntry(intent);

        } else if (action.equals(Constants.ACTION_DELETE_SERIES_DVR_ENTRY)) {
            String id = intent.getStringExtra("id");
            deleteAutorecEntry(id);

        } else if (action.equals(Constants.ACTION_GET_SYSTEM_TIME)) {
            getSystemTime();

        }
        logger.log(TAG, "onStartCommand() returned: " + START_NOT_STICKY);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");

        execService.shutdown();
        if (connection != null) {
            connection.close();
        }
    }

    public void onError(final String error) {
        ds.setLoading(false);
        ds.setConnectionState(error);
    }

    private void onInitialSyncCompleted() {
        Log.d(TAG, "onInitialSyncCompleted() called");
        ds.setLoading(false);
        ds.setConnectionState(Constants.ACTION_CONNECTION_STATE_OK);
        ds.setProtocolVersion(connection.getProtocolVersion());
        ds.setServerName(connection.getServerName());
        ds.setServerVersion(connection.getServerVersion());
        ds.setWebRoot(connection.getWebRoot());

        // Get some additional information after the initial loading has been finished
        getDiscSpace();
        getSystemTime();
    }

    private void onSubscriptionStart(HTSMessage msg) {
        Subscription subscription = ds.getSubscription(msg.getLong("subscriptionId"));
        if (subscription == null) {
            return;
        }

        for (Object obj : msg.getList("streams")) {
            Stream s = new Stream();
            HTSMessage sub = (HTSMessage) obj;
            s.index = sub.getInt("index");
            s.type = sub.getString("type");
            s.language = sub.getString("language");
            s.width = sub.getInt("width", 0);
            s.height = sub.getInt("height", 0);
            s.duration = sub.getInt("duration", 0);
            s.aspectNum = sub.getInt("aspect_num", 0);
            s.aspectDen = sub.getInt("aspect_den", 0);
            s.autioType = sub.getInt("autio_type", 0);
            s.channels = sub.getInt("channels", 0);
            s.rate = sub.getInt("rate", 0);
            subscription.streams.add(s);
        }

        if (msg.containsField("sourceinfo")) {
            Object obj = msg.get("sourceinfo");
            HTSMessage sub = (HTSMessage) obj;
            SourceInfo si = new SourceInfo();
            si.adapter = sub.getString("adapter");
            si.mux = sub.getString("mux");
            si.network = sub.getString("network");
            si.provider = sub.getString("provider");
            si.service = sub.getString("service");
            subscription.sourceInfo = si;
        }
    }

    private void onSubscriptionStatus(HTSMessage msg) {
        Subscription s = ds.getSubscription(msg.getLong("subscriptionId"));
        if (s == null) {
            return;
        }
        String status = msg.getString("status", null);
        if (s.status == null ? status != null : !s.status.equals(status)) {
            s.status = status;
            ds.updateSubscription(s);
        }
    }

    private void onSubscriptionStop(HTSMessage msg) {
        Subscription s = ds.getSubscription(msg.getLong("subscriptionId"));
        if (s == null) {
            return;
        }
        String status = msg.getString("status", null);
        if (s.status == null ? status != null : !s.status.equals(status)) {
            s.status = status;
            ds.updateSubscription(s);
        }
        ds.removeSubscription(s);
    }

    private void onSubscriptionGrace(HTSMessage msg) {
        Subscription s = ds.getSubscription(msg.getLong("subscriptionId"));
        if (s == null) {
            return;
        }
        long gt = msg.getLong("graceTimeout", 0);
        if (s.graceTimeout != gt) {
            s.graceTimeout = gt;
            ds.updateSubscription(s);
        }
    }

    private void onSubscriptionSignalStatus(HTSMessage msg) {
        Subscription s = ds.getSubscription(msg.getLong("subscriptionId"));
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
        Subscription sub = ds.getSubscription(msg.getLong("subscriptionId"));
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
        ds.broadcastPacket(packet);
    }

    private void onQueueStatus(HTSMessage msg) {
        Subscription sub = ds.getSubscription(msg.getLong("subscriptionId"));
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

        ds.updateSubscription(sub);
    }

    public void onMessage(HTSMessage msg) {
        logger.log(TAG, "onMessage() called with: msg = [" + msg.getMethod() + "]");
        String method = msg.getMethod();

        switch (method) {
            case "tagAdd":
                onTagAdd(msg);
                break;
            case "tagUpdate":
                onTagUpdate(msg);
                break;
            case "tagDelete":
                onTagDelete(msg);
                break;
            case "channelAdd":
                onChannelAdd(msg);
                break;
            case "channelUpdate":
                onChannelUpdate(msg);
                break;
            case "channelDelete":
                onChannelDelete(msg);
                break;
            case "initialSyncCompleted":
                onInitialSyncCompleted();
                break;
            case "dvrEntryAdd":
                onDvrEntryAdd(msg);
                break;
            case "dvrEntryUpdate":
                onDvrEntryUpdate(msg);
                break;
            case "dvrEntryDelete":
                onDvrEntryDelete(msg);
                break;
            case "timerecEntryAdd":
                onTimerRecEntryAdd(msg);
                break;
            case "timerecEntryUpdate":
                onTimerRecEntryUpdate(msg);
                break;
            case "timerecEntryDelete":
                onTimerRecEntryDelete(msg);
                break;
            case "subscriptionStart":
                onSubscriptionStart(msg);
                break;
            case "subscriptionStatus":
                onSubscriptionStatus(msg);
                break;
            case "subscriptionStop":
                onSubscriptionStop(msg);
                break;
            case "subscriptionGrace":
                onSubscriptionGrace(msg);
                break;
            case "muxpkt":
                onMuxPacket(msg);
                break;
            case "queueStatus":
                onQueueStatus(msg);
                break;
            case "autorecEntryAdd":
                onAutorecEntryAdd(msg);
                break;
            case "autorecEntryUpdate":
                onAutorecEntryUpdate(msg);
                break;
            case "autorecEntryDelete":
                onAutorecEntryDelete(msg);
                break;
            case "signalStatus":
                onSubscriptionSignalStatus(msg);
                break;
            case "eventAdd":
                onEventAdd(msg);
                break;
            case "eventUpdate":
                onEventUpdate(msg);
                break;
            case "eventDelete":
                onEventDelete(msg);
                break;
            default:
                break;
        }
    }

    private String hashString(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & aMessageDigest));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // NOP
        }

        return "";
    }

    private void cacheImage(String url, File f) throws IOException {
        InputStream is;

        if (url.startsWith("http")) {
            is = new BufferedInputStream(new URL(url).openStream());
        } else if (connection.getProtocolVersion() > 9) {
            is = new HTSFileInputStream(connection, url);
        } else {
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

    private Bitmap getIcon(final String url) throws IOException {

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
                    ds.updateChannel(ch);
                } catch (Throwable ex) {
                    logger.log(TAG, "run: Could not load channel icon. " + ex.getLocalizedMessage());
                }
            }
        });
    }

    private void getChannelTagIcon(final ChannelTag tag) {
        execService.execute(new Runnable() {
            public void run() {
                try {
                    tag.iconBitmap = getIcon(tag.icon);
                    ds.updateChannelTag(tag);
                } catch (Throwable ex) {
                    logger.log(TAG, "run: Could not load tag icon. " + ex.getLocalizedMessage());
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

                for (Object obj : response.getList("events")) {
                    HTSMessage sub = (HTSMessage) obj;
                    Program p = getProgramFromEventMessage(sub);
                    p.channel = ch;

                    if (ch.epg.add(p)) {
                        ds.addProgram(p);
                    }
                }
                ds.updateChannel(ch);
            }
        });
    }

    private Program getProgramFromEventMessage(HTSMessage sub) {
        Program p = new Program();
        p.id = sub.getLong("eventId", 0);
        p.nextId = sub.getLong("nextEventId", 0);
        p.description = sub.getString("description");
        p.summary = sub.getString("summary");
        p.subtitle = sub.getString("subtitle");
        p.recording = ds.getRecording(sub.getLong("dvrId", 0));
        p.contentType = sub.getInt("contentType", -1);
        p.title = sub.getString("title");
        p.start = sub.getDate("start");
        p.stop = sub.getDate("stop");
        p.seriesInfo = buildSeriesInfo(sub);
        p.starRating = sub.getInt("starRating", -1);
        p.image = sub.getString("image");
        return p;
    }

    private void getEvent(long eventId) {
        HTSMessage request = new HTSMessage();
        request.setMethod("getEvent");
        request.putField("eventId", eventId);

        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                Channel ch = ds.getChannel(response.getLong("channelId"));
                Program p = getProgramFromEventMessage(response);
                p.channel = ch;

                if (ch.epg.add(p)) {
                    ds.addProgram(p);
                    ds.updateChannel(ch);
                }
            }
        });
    }

    private SeriesInfo buildSeriesInfo(HTSMessage msg) {
        SeriesInfo info = new SeriesInfo();
        info.episodeCount = msg.getInt("episodeCount", 0);
        info.episodeNumber = msg.getInt("episodeNumber", 0);
        info.onScreen = msg.getString("episodeOnscreen");
        info.partCount = msg.getInt("partCount", 0);
        info.partNumber = msg.getInt("partNumber", 0);
        info.seasonCount = msg.getInt("seasonCount", 0);
        info.seasonNumber = msg.getInt("seasonNumber", 0);
        info.serieslinkId = msg.getInt("serieslinkId", 0);
        return info;
    }

    private void epgQuery(final Intent intent) {

        final Channel ch = ds.getChannel(intent.getLongExtra("channelId", 0));
        final String query = intent.getStringExtra("query");
        final long tagId = intent.getLongExtra("tagId", 0);

        HTSMessage request = new HTSMessage();
        request.setMethod("epgQuery");
        request.putField("query", query);

        // The default values will be set in case a server with a htsp API
        // version 12 or lower is used
        request.putField("minduration", 0);
        request.putField("maxduration", Integer.MAX_VALUE);

        if (ch != null && ch.id > 0) {
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

    /**
     * Cancels a regular recording from the server with the given id.
     * If it was successful a positive message is shown
     * otherwise a negative one.
     *
     * @param id The id of the regular recording that shall be stopped
     */
    private void cancelDvrEntry(final long id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("cancelDvrEntry");
        request.putField("id", id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    ds.showMessage(getString(R.string.error_removing_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_removing_recording));
                }
            }
        });
    }

    /**
     * Stops a regular recording from the server with the given id. If the
     * stopping was not successful then the fallback method to cancel a
     * recording is used. If it was successful a positive message is shown
     * otherwise a negative one.
     *
     * @param id The id of the regular recording that shall be stopped
     */
    private void stopDvrEntry(final long id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("stopDvrEntry");
        request.putField("id", id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    cancelDvrEntry(id);
                } else {
                    ds.showMessage(getString(R.string.success_removing_recording));
                }
            }
        });
    }

    /**
     * Updates a regular recording on the server with the given id and values.
     * If the update was successful a positive message is shown otherwise a
     * negative one.
     *
     * @param intent Contains the intent with the (un)changed parameters of the regular
     *               recording
     */
    private void updateDvrEntry(final Intent intent) {

        final long id = intent.getLongExtra("id", 0);
        final long channelId = intent.getLongExtra("channelId", 0);
        final long start = intent.getLongExtra("start", 0);
        final long stop = intent.getLongExtra("stop", 0);
        final long retention = intent.getLongExtra("retention", 0);
        final long priority = intent.getLongExtra("priority", 2);
        final long enabled = intent.getLongExtra("enabled", 1);
        final long startExtra = intent.getLongExtra("startExtra", 0);
        final long stopExtra = intent.getLongExtra("stopExtra", 0);
        final String title = intent.getStringExtra("title");
        final String subtitle = intent.getStringExtra("subtitle");
        final String description = intent.getStringExtra("description");
        final String configName = intent.getStringExtra("configName");
        final boolean isRecording = intent.getBooleanExtra("isRecording", false);

        HTSMessage request = new HTSMessage();
        request.setMethod("updateDvrEntry");
        request.putField("id", id);
        request.putField("stop", stop);
        request.putField("stopExtra", stopExtra);

        // Only add these fields when the recording is only scheduled and not
        // being recorded
        if (!isRecording) {
            request.putField("start", start);
            request.putField("retention", retention);
            request.putField("priority", priority);
            request.putField("startExtra", startExtra);

            // Do not add these fields if not supported by the server. Updating
            // these strings was fixed in server version v4.1-111-g807b9c8
            if (title != null && ds.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_TITLE) {
                request.putField("title", title);
            }
            if (subtitle != null && ds.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_SUBTITLE) {
                request.putField("subtitle", subtitle);
            }
            if (description != null && ds.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DESCRIPTION) {
                request.putField("description", description);
            }
            if (channelId != 0 && ds.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_UPDATE_CHANNEL) {
                request.putField("channelId", channelId);
            }
            if (configName != null) {
                request.putField("configName", configName);
            }
            // Enabled flag (Added in version 23)
            request.putField("enabled", enabled);
        }

        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    ds.showMessage(getString(R.string.error_updating_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_updating_recording));

                    // Force a reconnect. This is a workaround because no
                    // onDvrUpdate event is sent
                    Utils.connect(TVHClientApplication.getInstance().getApplicationContext(), true);
                }
            }
        });
    }

    /**
     * Deletes a regular recording from the server with the given id. If the
     * removal was successful a positive message is shown otherwise a negative
     * one.
     *
     * @param id The id of the regular recording that shall be deleted
     */
    private void deleteDvrEntry(long id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("deleteDvrEntry");
        request.putField("id", id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    ds.showMessage(getString(R.string.error_removing_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_removing_recording));
                }
            }
        });
    }

    /**
     * Adds a regular recording to the server with the given values. If the
     * adding was successful a positive message is shown otherwise a negative
     * one.
     *
     * @param intent The intent with the parameters of the recording that shall be added
     */
    private void addDvrEntry(final Intent intent) {

        final long eventId = intent.getLongExtra("eventId", 0);
        final long channelId = intent.getLongExtra("channelId", 0);
        final long start = intent.getLongExtra("start", 0);
        final long stop = intent.getLongExtra("stop", 0);
        final long retention = intent.getLongExtra("retention", 0);
        final String creator = intent.getStringExtra("creator");
        final long priority = intent.getLongExtra("priority", 2);
        final long enabled = intent.getLongExtra("enabled", 1);
        final long startExtra = intent.getLongExtra("startExtra", 0);
        final long stopExtra = intent.getLongExtra("stopExtra", 0);
        final String title = intent.getStringExtra("title");
        final String subtitle = intent.getStringExtra("subtitle");
        final String description = intent.getStringExtra("description");
        final String configName = intent.getStringExtra("configName");

        HTSMessage request = new HTSMessage();
        request.setMethod("addDvrEntry");

        // If the eventId is set then an existing program from the program guide
        // shall be recorded. The server will then ignore the other fields
        // automatically.
        request.putField("eventId", eventId);
        request.putField("channelId", channelId);
        request.putField("start", start);
        request.putField("stop", stop);
        request.putField("startExtra", startExtra);
        request.putField("stopExtra", stopExtra);
        request.putField("retention", retention);
        request.putField("priority", priority);

        // Enabled flag (Added in version 23)
        request.putField("enabled", enabled);

        if (title != null) {
            request.putField("title", title);
        }
        if (subtitle != null) {
            request.putField("subtitle", subtitle);
        }
        if (description != null) {
            request.putField("description", description);
        }
        if (creator != null) {
            request.putField("creator", creator);
        }
        if (configName != null) {
            request.putField("configName", configName);
        }

        // Get the channel from the id to update the program list of the channel
        // when the recording was added.
        final Channel ch = ds.getChannel(channelId);

        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (success && ch != null) {
                    for (Program p : ch.epg) {
                        if (p.id == eventId) {
                            p.recording = ds.getRecording(response.getLong("id", 0));
                            ds.updateProgram(p);
                            break;
                        }
                    }
                }

                if (!success) {
                    ds.showMessage(getString(R.string.error_adding_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_adding_recording));
                }
            }
        });
    }

    /**
     * Updates a manual recording to the server with the given values. If the
     * update was successful a positive message is shown otherwise a negative
     * one.
     *
     * @param intent Contains the parameters of the manual recording that shall be
     *               updated
     */
    private void updateTimerRecEntry(final Intent intent) {

        final String id = intent.getStringExtra("id");
        final long channelId = intent.getLongExtra("channelId", 0);
        final long start = intent.getLongExtra("start", 0);
        final long stop = intent.getLongExtra("stop", 0);
        final long retention = intent.getLongExtra("retention", 0);
        final long priority = intent.getLongExtra("priority", 2);
        final long daysOfWeek = intent.getLongExtra("daysOfWeek", 0);
        final long enabled = intent.getLongExtra("enabled", 1);
        final String directory = intent.getStringExtra("directory");
        final String title = intent.getStringExtra("title");
        final String name = intent.getStringExtra("name");
        final String configName = intent.getStringExtra("configName");

        HTSMessage request = new HTSMessage();
        request.setMethod("updateTimerecEntry");
        request.putField("id", id);
        request.putField("title", title);
        request.putField("name", name);
        request.putField("start", start);
        request.putField("stop", stop);
        request.putField("channelId", channelId);
        request.putField("retention", retention);
        request.putField("daysOfWeek", daysOfWeek);
        request.putField("priority", priority);
        request.putField("enabled", enabled);
        request.putField("directory", directory);

        if (configName != null) {
            request.putField("configName", configName);
        }

        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    ds.showMessage(getString(R.string.error_adding_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_adding_recording));
                }
            }
        });
    }

    /**
     * Deletes a manual recording from the server with the given id. If the
     * removal was successful a positive message is shown otherwise a negative
     * one.
     *
     * @param id The id of the manual recording that shall be deleted
     */
    private void deleteTimerRecEntry(String id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("deleteTimerecEntry");
        request.putField("id", id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    ds.showMessage(getString(R.string.error_removing_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_removing_recording));
                }
            }
        });
    }

    /**
     * Adds a manual recording to the server with the given values. If the
     * adding was successful a positive message is shown otherwise a negative
     * one.
     *
     * @param intent Contains the parameters of the manual recording that shall be
     *               added
     */
    private void addTimerRecEntry(final Intent intent) {

        final long channelId = intent.getLongExtra("channelId", 0);
        final long start = intent.getLongExtra("start", 0);
        final long stop = intent.getLongExtra("stop", 0);
        final long retention = intent.getLongExtra("retention", 0);
        final long priority = intent.getLongExtra("priority", 2);
        final long daysOfWeek = intent.getLongExtra("daysOfWeek", 0);
        final long enabled = intent.getLongExtra("enabled", 1);
        final String directory = intent.getStringExtra("directory");
        final String title = intent.getStringExtra("title");
        final String name = intent.getStringExtra("name");
        final String configName = intent.getStringExtra("configName");

        HTSMessage request = new HTSMessage();
        request.setMethod("addTimerecEntry");
        request.putField("title", title);
        request.putField("name", name);
        request.putField("start", start);
        request.putField("stop", stop);
        request.putField("channelId", channelId);
        request.putField("retention", retention);
        request.putField("daysOfWeek", daysOfWeek);
        request.putField("priority", priority);
        request.putField("enabled", enabled);
        request.putField("directory", directory);

        if (configName != null) {
            request.putField("configName", configName);
        }

        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    ds.showMessage(getString(R.string.error_adding_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_adding_recording));
                }
            }
        });
    }

    private void subscribe(long channelId, long subscriptionId, int maxWidth, int maxHeight, String aCodec, String vCodec) {
        Subscription subscription = new Subscription();
        subscription.id = subscriptionId;
        subscription.status = "Subscribing";

        ds.addSubscription(subscription);

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
        ds.removeSubscription(subscriptionId);

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
                String webroot = connection.getWebRoot();

                if (path != null && ticket != null) {
                    ds.addTicket(new HttpTicket(webroot + path, ticket));
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
                    ds.addTicket(new HttpTicket(path, ticket));
                }
            }
        });
    }

    private void getDiscSpace() {
        HTSMessage request = new HTSMessage();
        request.setMethod("getDiskSpace");
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                DiscSpace ds = new DiscSpace();
                ds.freediskspace = response.getString("freediskspace", null);
                ds.totaldiskspace = response.getString("totaldiskspace", null);
                HTSService.this.ds.addDiscSpace(ds);
            }
        });
    }

    private void getSystemTime() {
        HTSMessage request = new HTSMessage();
        request.setMethod("getSysTime");
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                SystemTime st = new SystemTime();
                st.time = response.getString("time", null);
                st.timezone = response.getString("timezone", null);
                st.gmtoffset = response.getString("gmtoffset", null);
                ds.addSystemTime(st);
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
                List<Profiles> pList = new ArrayList<>();
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
                ds.addDvrConfigs(pList);
            }
        });
    }

    /**
     * Updates a series recording to the server with the given values. If the
     * update was successful a positive message is shown otherwise a negative
     * one.
     *
     * @param intent Contains the parameters of the series recording that shall be
     *               updated
     */
    private void updateAutorecEntry(final Intent intent) {

        final String id = intent.getStringExtra("id");
        final String title = intent.getStringExtra("title");
        final String name = intent.getStringExtra("name");
        final long channelId = intent.getLongExtra("channelId", 0);
        final long maxDuration = intent.getLongExtra("maxDuration", 0);
        final long minDuration = intent.getLongExtra("minDuration", 0);
        final long retention = intent.getLongExtra("retention", 0);
        final long daysOfWeek = intent.getLongExtra("daysOfWeek", 127);
        final long priority = intent.getLongExtra("priority", 2);
        final long enabled = intent.getLongExtra("enabled", 1);
        final String directory = intent.getStringExtra("directory");
        final long startExtra = intent.getLongExtra("startExtra", 0);
        final long stopExtra = intent.getLongExtra("stopExtra", 0);
        final long dupDetect = intent.getLongExtra("dupDetect", 0);
        final long start = intent.getLongExtra("start", -1);
        final long startWindow = intent.getLongExtra("startWindow", -1);
        final String configName = intent.getStringExtra("configName");

        HTSMessage request = new HTSMessage();
        request.setMethod("updateAutorecEntry");
        request.putField("id", id);
        request.putField("title", title);
        request.putField("name", name);

        // Don't add the channel id if none was given. Assume the user wants to
        // record on all channels 
        if (channelId > 0) {
            request.putField("channelId", channelId);
        }

        // Minimal duration in seconds (0 = Any)
        request.putField("minDuration", minDuration);
        // Maximal duration in seconds (0 = Any)
        request.putField("maxDuration", maxDuration);
        request.putField("retention", retention);
        request.putField("daysOfWeek", daysOfWeek);
        request.putField("priority", priority);
        request.putField("startExtra", startExtra);
        request.putField("stopExtra", stopExtra);

        // Minutes from midnight (up to 24*60) for the start of the time
        // window (including) (Added in version 18). Do not send the value
        // if the default of -1 (no time specified) was set
        if (start >= 0) {
            request.putField("start", start);
        }
        // Minutes from midnight (up to 24*60) for the end of the time
        // window (including, cross-noon allowed) (Added in version 18). Do
        // not send the value if the default of -1 (no time specified) was set
        if (startWindow >= 0) {
            request.putField("startWindow", startWindow);
        }
        // Minutes from midnight (up to 24*60) (window +- 15 minutes)
        // (Obsoleted from version 18). Do not send the value if the default
        // of -1 (no time specified) was set
        if (start >= 0) {
            request.putField("approxTime", start);
        }

        request.putField("enabled", enabled);
        request.putField("directory", directory);
        request.putField("dupDetect", dupDetect);

        if (configName != null) {
            request.putField("configName", configName);
        }

        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    ds.showMessage(getString(R.string.error_adding_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_adding_recording));
                }
            }
        });
    }

    /**
     * Deletes a series recording from the server with the given id. If the
     * removal was successful a positive message is shown otherwise a negative
     * one.
     *
     * @param id The id of the series recording that shall be deleted
     */
    private void deleteAutorecEntry(final String id) {
        HTSMessage request = new HTSMessage();
        request.setMethod("deleteAutorecEntry");
        request.putField("id", id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    ds.showMessage(getString(R.string.error_removing_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_removing_recording));
                }
            }
        });
    }

    /**
     * Adds a series recording to the server with the given values. If the
     * adding was successful a positive message is shown otherwise a negative
     * one.
     *
     * @param intent Contains the parameters of the series recording that shall be
     *               added
     */
    private void addAutorecEntry(final Intent intent) {

        final String title = intent.getStringExtra("title");
        final String fulltext = intent.getStringExtra("fulltext");
        final String name = intent.getStringExtra("name");
        final long channelId = intent.getLongExtra("channelId", 0);
        final long maxDuration = intent.getLongExtra("maxDuration", 0);
        final long minDuration = intent.getLongExtra("minDuration", 0);
        final long retention = intent.getLongExtra("retention", 0);
        final long daysOfWeek = intent.getLongExtra("daysOfWeek", 127);
        final long priority = intent.getLongExtra("priority", 2);
        final long enabled = intent.getLongExtra("enabled", 1);
        final String directory = intent.getStringExtra("directory");
        final long startExtra = intent.getLongExtra("startExtra", 0);
        final long stopExtra = intent.getLongExtra("stopExtra", 0);
        final long dupDetect = intent.getLongExtra("dupDetect", 0);
        final long start = intent.getLongExtra("start", -1);
        final long startWindow = intent.getLongExtra("startWindow", -1);
        final String configName = intent.getStringExtra("configName");

        HTSMessage request = new HTSMessage();
        request.setMethod("addAutorecEntry");
        request.putField("title", title);
        request.putField("name", name);

        // Don't add the channel id if none was given. Assume the user wants to
        // record on all channels 
        if (channelId > 0) {
            request.putField("channelId", channelId);
        }

        // Minimal duration in seconds (0 = Any)
        request.putField("minDuration", minDuration);
        // Maximal duration in seconds (0 = Any)
        request.putField("maxDuration", maxDuration);
        request.putField("retention", retention);
        request.putField("daysOfWeek", daysOfWeek);
        request.putField("priority", priority);
        request.putField("startExtra", startExtra);
        request.putField("stopExtra", stopExtra);

        // Minutes from midnight (up to 24*60) for the start of the time
        // window (including) (Added in version 18). Do not send the value
        // if the default of -1 (no time specified) was set
        if (start >= 0) {
            request.putField("start", start);
        }
        // Minutes from midnight (up to 24*60) for the end of the time
        // window (including, cross-noon allowed) (Added in version 18). Do
        // not send the value if the default of -1 (no time specified) was set
        if (startWindow >= 0) {
            request.putField("startWindow", startWindow);
        }
        // Minutes from midnight (up to 24*60) (window +- 15 minutes)
        // (Obsoleted from version 18). Do not send the value if the default
        // of -1 (no time specified) was set
        if (start >= 0) {
            request.putField("approxTime", start);
        }

        request.putField("enabled", enabled);
        request.putField("directory", directory);
        request.putField("dupDetect", dupDetect);

        if (configName != null) {
            request.putField("configName", configName);
        }
        if (fulltext != null) {
            request.putField("fulltext", fulltext);
        }

        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                @SuppressWarnings("unused")
                boolean success = response.getInt("success", 0) == 1;
                if (!success) {
                    ds.showMessage(getString(R.string.error_adding_recording,
                            response.getString("error", "")));
                } else {
                    ds.showMessage(getString(R.string.success_adding_recording));
                }
            }
        });
    }

    private void getDvrCutpoints(final Recording rec) {

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
                }
            }
        });
    }

    private void subscriptionFilterStream() {
        // NOP
    }

    private void getChannel(final Channel ch) {
        HTSMessage request = new HTSMessage();
        request.setMethod("getChannel");
        request.putField("channelId", ch.id);
        connection.sendMessage(request, new HTSResponseHandler() {
            public void handleResponse(HTSMessage response) {
                // NOP
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
                List<Profiles> pList = new ArrayList<>();
                for (Object obj : response.getList("profiles")) {
                    HTSMessage sub = (HTSMessage) obj;

                    Profiles p = new Profiles();
                    p.uuid = sub.getString("uuid");
                    p.name = sub.getString("name");
                    p.comment = sub.getString("comment");
                    pList.add(p);
                }
                ds.addProfiles(pList);
            }
        });
    }


    /**
     * Server to client method.
     * A tag has been added on the server.
     *
     * @param msg The message with the new tag data
     */
    private void onTagAdd(HTSMessage msg) {
        Log.d(TAG, "onTagAdd() called");

        ContentValues values = new ContentValues();
        values.put(DataContract.Tags.ID, msg.getInt("tagId"));                      // u32   required   ID of tag.
        values.put(DataContract.Tags.NAME, msg.getString("tagName"));               // str   required   Name of tag.
        values.put(DataContract.Tags.INDEX, msg.getInt("tagIndex", 0));             // u32   optional   Index value for sorting (default by from min to max) (Added in version 18).
        values.put(DataContract.Tags.ICON, msg.getString("tagIcon", null));         // str   optional   URL to an icon representative for the channel.
        values.put(DataContract.Tags.TITLED_ICON, msg.getInt("tagTitledIcon", 0));  // u32   optional   Icon includes a title

        getContentResolver().insert(DataContract.Tags.CONTENT_URI, values);

        // TODO remove old stuff when possible
        ChannelTag tag = new ChannelTag();
        tag.id = msg.getLong("tagId");
        tag.name = msg.getString("tagName", null);
        tag.icon = msg.getString("tagIcon", null);
        ds.addChannelTag(tag);
        if (tag.icon != null) {
            getChannelTagIcon(tag);
        }
    }

    /**
     * Server to client method.
     * A tag has been updated on the server.
     *
     * @param msg The message with the updated tag data
     */
    private void onTagUpdate(HTSMessage msg) {
        Log.d(TAG, "onTagUpdate() called");

        int id = msg.getInt("tagId");
        ContentValues values = new ContentValues();
        values.put(DataContract.Tags.NAME, msg.getString("tagName"));               // str   required   Name of tag.
        values.put(DataContract.Tags.INDEX, msg.getInt("tagIndex", 0));             // u32   optional   Index value for sorting (default by from min to max) (Added in version 18).
        values.put(DataContract.Tags.ICON, msg.getString("tagIcon", null));         // str   optional   URL to an icon representative for the channel.
        values.put(DataContract.Tags.TITLED_ICON, msg.getInt("tagTitledIcon", 0));  // u32   optional   Icon includes a title

        getContentResolver().update(DataContract.Tags.CONTENT_URI, values,
                DataContract.Tags.ID + "=?", new String[]{String.valueOf(id)});

        // TODO remove old stuff when possible

        ChannelTag tag = ds.getChannelTag(msg.getLong("tagId"));
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

    /**
     * Server to client method.
     * A tag has been deleted on the server.
     *
     * @param msg The message with the tag id that was deleted
     */
    private void onTagDelete(HTSMessage msg) {
        Log.d(TAG, "onTagDelete() called");

        int id = msg.getInt("tagId");
        getContentResolver().delete(DataContract.Tags.CONTENT_URI,
                DataContract.Tags.ID + "=?", new String[]{String.valueOf(id)});

        // TODO remove old stuff when possible
        ds.removeChannelTag(msg.getLong("tagId"));
    }

    /**
     * Server to client method.
     * A channel has been added on the server.
     *
     * @param msg The message with the new channel data
     */
    private void onChannelAdd(HTSMessage msg) {
        Log.d(TAG, "onChannelAdd() called");

        ContentValues values = new ContentValues();
        values.put(DataContract.Channels.ID, msg.getInt("channelId"));                          // u32 required   ID of channel
        values.put(DataContract.Channels.NUMBER, msg.getInt("channelNumber"));                  // u32 required   Channel number, 0 means unconfigured.
        values.put(DataContract.Channels.NUMBER_MINOR, msg.getInt("channelNumberMinor", 0));    // u32 optional   Minor channel number (Added in version 13).
        values.put(DataContract.Channels.NAME, msg.getString("channelName"));                   // str required   Name of channel.
        values.put(DataContract.Channels.ICON, msg.getString("channelIcon", null));             // str optional   URL to an icon representative for the channel
        values.put(DataContract.Channels.EVENT_ID, msg.getInt("eventId", 0));                   // u32 optional   ID of the current event on this channel.
        values.put(DataContract.Channels.NEXT_EVENT_ID, msg.getInt("nextEventId", 0));          // u32 optional   ID of the next event on the channel.

        getContentResolver().insert(DataContract.Channels.CONTENT_URI, values);

        // TODO remove old stuff when possible

        final Channel ch = new Channel();
        ch.id = msg.getLong("channelId");
        ch.name = msg.getString("channelName", null);
        ch.number = msg.getInt("channelNumber", 0);

        // The default values will be set in case a server with a htsp API
        // version 12 or lower is used
        ch.numberMinor = msg.getInt("channelNumberMinor", 0);

        ch.icon = msg.getString("channelIcon", null);
        ch.tags = msg.getIntList("tags", ch.tags);

        if (ch.number == 0) {
            ch.number = (int) (ch.id + 25000);
        }

        ds.addChannel(ch);
        if (ch.icon != null) {
            getChannelIcon(ch);
        }
        long currEventId = msg.getLong("eventId", 0);
        long nextEventId = msg.getLong("nextEventId", 0);

        ch.isTransmitting = (currEventId != 0);

        if (currEventId > 0) {
            getEvents(ch, currEventId, Constants.PREF_PROGRAMS_TO_LOAD);
        } else if (nextEventId > 0) {
            getEvents(ch, nextEventId, Constants.PREF_PROGRAMS_TO_LOAD);
        }
    }

    /**
     * Server to client method.
     * A channel has been updated on the server.
     *
     * @param msg The message with the updated channel data
     */
    private void onChannelUpdate(HTSMessage msg) {
        Log.d(TAG, "onChannelUpdate() called");

        int id = msg.getInt("channelId");
        ContentValues values = new ContentValues();
        values.put(DataContract.Channels.NUMBER, msg.getInt("channelNumber"));                  // u32   Channel number, 0 means unconfigured.
        values.put(DataContract.Channels.NUMBER_MINOR, msg.getInt("channelNumberMinor", 0));    // u32   Minor channel number (Added in version 13).
        values.put(DataContract.Channels.NAME, msg.getString("channelName"));                   // str   Name of channel.
        values.put(DataContract.Channels.ICON, msg.getString("channelIcon", null));             // str   URL to an icon representative for the channel
        values.put(DataContract.Channels.EVENT_ID, msg.getInt("eventId", 0));                   // u32   ID of the current event on this channel.
        values.put(DataContract.Channels.NEXT_EVENT_ID, msg.getInt("nextEventId", 0));          // u32   ID of the next event on the channel.

        getContentResolver().update(DataContract.Channels.CONTENT_URI, values,
                DataContract.Channels.ID + "=?", new String[]{String.valueOf(id)});

        // TODO remove old stuff when possible

        final Channel ch = ds.getChannel(msg.getLong("channelId"));
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
        ArrayList<Program> tmp = new ArrayList<>();

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
            ds.removeProgram(p);
        }

        final long eventId = currEventId != 0 ? currEventId : nextEventId;
        if (eventId > 0 && ch.epg.size() < 2) {
            execService.schedule(new Runnable() {
                public void run() {
                    getEvents(ch, eventId, 5);
                }
            }, 30, TimeUnit.SECONDS);
        } else {
            ds.updateChannel(ch);
        }
    }

    /**
     * Server to client method.
     * A channel has been deleted on the server.
     *
     * @param msg The message with the channel id that was deleted
     */
    private void onChannelDelete(HTSMessage msg) {
        Log.d(TAG, "onChannelDelete() called");

        int id = msg.getInt("channelId");
        getContentResolver().delete(DataContract.Channels.CONTENT_URI,
                DataContract.Channels.ID + "=?", new String[]{String.valueOf(id)});

        // TODO remove old stuff when possible
        ds.removeChannel(msg.getLong("channelId"));
    }

    /**
     * Server to client method.
     * A recording has been added on the server.
     *
     * @param msg The message with the new recording data
     */
    private void onDvrEntryAdd(HTSMessage msg) {
        Log.d(TAG, "onDvrEntryAdd() called");

        ContentValues values = new ContentValues();
        values.put(DataContract.Recordings.ID, msg.getLong("id"));                              // u32   required   ID of dvrEntry.
        values.put(DataContract.Recordings.CHANNEL, msg.getInt("channel", 0));                  // u32   optional   Channel of dvrEntry.
        values.put(DataContract.Recordings.START, msg.getInt("start"));                         // s64   required   Time of when this entry was scheduled to start recording.
        values.put(DataContract.Recordings.STOP, msg.getInt("stop"));                           // s64   required   Time of when this entry was scheduled to stop recording.
        values.put(DataContract.Recordings.START_EXTRA, msg.getInt("startExtra"));              // s64   required   Extra start time (pre-time) in minutes (Added in version 13).
        values.put(DataContract.Recordings.STOP_EXTRA, msg.getInt("stopExtra"));                // s64   required   Extra stop time (post-time) in minutes (Added in version 13).
        values.put(DataContract.Recordings.RETENTION, msg.getInt("retention"));                 // s64   required   DVR Entry retention time in days (Added in version 13).
        values.put(DataContract.Recordings.PRIORITY, msg.getInt("priority"));                   // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set) (Added in version 13).
        values.put(DataContract.Recordings.EVENT_ID, msg.getInt("eventId", 0));                 // u32   optional   Associated EPG Event ID (Added in version 13).
        values.put(DataContract.Recordings.AUTOREC_ID, msg.getString("autorecId", null));       // str   optional   Associated Autorec UUID (Added in version 13).
        values.put(DataContract.Recordings.TIMEREC_ID, msg.getString("timerecId", null));       // str   optional   Associated Timerec UUID (Added in version 18).
        values.put(DataContract.Recordings.TYPE_OF_CONTENT, msg.getInt("contentType", 0));      // u32   optional   Content Type (like in the DVB standard) (Added in version 13).
        values.put(DataContract.Recordings.TITLE, msg.getString("title", null));                // str   optional   Title of recording
        values.put(DataContract.Recordings.SUBTITLE, msg.getString("subtitle", null));          // str   optional   Subtitle of recording (Added in version 20).
        values.put(DataContract.Recordings.SUMMARY, msg.getString("summary", null));            // str   optional   Short description of the recording (Added in version 6).
        values.put(DataContract.Recordings.DESCRIPTION, msg.getString("description", null));    // str   optional   Long description of the recording.
        values.put(DataContract.Recordings.STATE, msg.getString("state"));                      // str   required   Recording state
        values.put(DataContract.Recordings.ERROR, msg.getString("error", null));                // str   optional   Plain english error description (e.g. "Aborted by user").
        values.put(DataContract.Recordings.OWNER, msg.getString("owner", null));                // str   optional   Name of the entry owner (Added in version 18).
        values.put(DataContract.Recordings.CREATOR, msg.getString("creator", null));            // str   optional   Name of the entry creator (Added in version 18).
        values.put(DataContract.Recordings.SUBSCRIPTION_ERROR, msg.getString("subscriptionError", null));    // str   optional   Subscription error string (Added in version 20).
        values.put(DataContract.Recordings.STREAM_ERRORS, msg.getString("streamErrors", null)); // str   optional   Number of recording errors (Added in version 20).
        values.put(DataContract.Recordings.DATA_ERRORS, msg.getString("dataErrors", null));     // str   optional   Number of stream data errors (Added in version 20).
        values.put(DataContract.Recordings.PATH, msg.getString("path", null));                  // str   optional   Recording path for playback.
        values.put(DataContract.Recordings.DATA_SIZE, msg.getInt("dataSize", 0));               // s64   optional   Actual file size of the last recordings (Added in version 21).
        values.put(DataContract.Recordings.ENABLED, msg.getInt("enabled", 0));                  // u32   optional   Enabled flag (Added in version 23).

        getContentResolver().insert(DataContract.Recordings.CONTENT_URI, values);

        // TODO remove old stuff when possible

        Recording rec = new Recording();
        rec.id = msg.getLong("id");
        rec.description = msg.getString("description");
        rec.summary = msg.getString("summary");
        rec.error = msg.getString("error");
        rec.start = msg.getDate("start");
        rec.state = msg.getString("state");
        rec.stop = msg.getDate("stop");
        rec.title = msg.getString("title");
        rec.subtitle = msg.getString("subtitle");
        rec.enabled = msg.getLong("enabled", 1) != 0;

        rec.channel = ds.getChannel(msg.getLong("channel", 0));
        if (rec.channel != null) {
            rec.channel.recordings.add(rec);
        }

        // Not all fields can be set with default values, so check if the server
        // provides a supported HTSP API version. These entries are available
        // only on version 13 and higher
        if (connection.getProtocolVersion() >= Constants.MIN_API_VERSION_SERIES_RECORDINGS) {
            rec.eventId = msg.getLong("eventId", 0);
            rec.autorecId = msg.getString("autorecId");
            rec.startExtra = msg.getLong("startExtra", 0);
            rec.stopExtra = msg.getLong("stopExtra", 0);
            rec.retention = msg.getLong("retention");
            rec.priority = msg.getLong("priority");
            rec.contentType = msg.getLong("contentType", -1);
        }

        // Not all fields can be set with default values, so check if the server
        // provides a supported HTSP API version. These entries are available
        // only on version 17 and higher
        if (connection.getProtocolVersion() >= Constants.MIN_API_VERSION_TIMER_RECORDINGS) {
            rec.timerecId = msg.getString("timerecId");
        }

        rec.episode = msg.getString("episode", null);
        rec.comment = msg.getString("comment", null);
        rec.subscriptionError = msg.getString("subscriptionError", null);
        rec.streamErrors = msg.getLong("streamErrors", 0);
        rec.dataErrors = msg.getLong("dataErrors", 0);
        rec.dataSize = msg.getLong("dataSize", 0);

        if (rec.channel != null && rec.channel.epg != null) {
            for (Program p : rec.channel.epg) {
                if (p != null
                        && p.title.equals(rec.title)
                        && p.start.getTime() == rec.start.getTime()
                        && p.stop.getTime() == rec.stop.getTime()) {
                    p.recording = rec;
                    break;
                }
            }
        }

        rec.owner = msg.getString("owner", null);
        rec.creator = msg.getString("creator", null);
        rec.path = msg.getString("path", null);
        rec.files = msg.getString("files", null);

        ds.addRecording(rec);
    }

    /**
     * Server to client method.
     * A recording has been updated on the server.
     *
     * @param msg The message with the updated recording data
     */
    private void onDvrEntryUpdate(HTSMessage msg) {
        Log.d(TAG, "onDvrEntryUpdate() called");

        int id = msg.getInt("id");
        ContentValues values = new ContentValues();
        values.put(DataContract.Recordings.CHANNEL, msg.getInt("channel", 0));                  // u32   Channel of dvrEntry.
        values.put(DataContract.Recordings.START, msg.getInt("start"));                         // s64   Time of when this entry was scheduled to start recording.
        values.put(DataContract.Recordings.STOP, msg.getInt("stop"));                           // s64   Time of when this entry was scheduled to stop recording.
        values.put(DataContract.Recordings.START_EXTRA, msg.getInt("startExtra"));              // s64   Extra start time (pre-time) in minutes (Added in version 13).
        values.put(DataContract.Recordings.STOP_EXTRA, msg.getInt("stopExtra"));                // s64   Extra stop time (post-time) in minutes (Added in version 13).
        values.put(DataContract.Recordings.RETENTION, msg.getInt("retention"));                 // s64   DVR Entry retention time in days (Added in version 13).
        values.put(DataContract.Recordings.PRIORITY, msg.getInt("priority"));                   // u32   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set) (Added in version 13).
        values.put(DataContract.Recordings.EVENT_ID, msg.getInt("eventId", 0));                 // u32   Associated EPG Event ID (Added in version 13).
        values.put(DataContract.Recordings.AUTOREC_ID, msg.getString("autorecId", null));       // str   Associated Autorec UUID (Added in version 13).
        values.put(DataContract.Recordings.TIMEREC_ID, msg.getString("timerecId", null));       // str   Associated Timerec UUID (Added in version 18).
        values.put(DataContract.Recordings.TYPE_OF_CONTENT, msg.getInt("contentType", 0));      // u32   Content Type (like in the DVB standard) (Added in version 13).
        values.put(DataContract.Recordings.TITLE, msg.getString("title", null));                // str   Title of recording
        values.put(DataContract.Recordings.SUBTITLE, msg.getString("subtitle", null));          // str   Subtitle of recording (Added in version 20).
        values.put(DataContract.Recordings.SUMMARY, msg.getString("summary", null));            // str   Short description of the recording (Added in version 6).
        values.put(DataContract.Recordings.DESCRIPTION, msg.getString("description", null));    // str   Long description of the recording.
        values.put(DataContract.Recordings.STATE, msg.getString("state"));                      // str   Recording state
        values.put(DataContract.Recordings.ERROR, msg.getString("error", null));                // str   Plain english error description (e.g. "Aborted by user").
        values.put(DataContract.Recordings.OWNER, msg.getString("owner", null));                // str   Name of the entry owner (Added in version 18).
        values.put(DataContract.Recordings.CREATOR, msg.getString("creator", null));            // str   Name of the entry creator (Added in version 18).
        values.put(DataContract.Recordings.SUBSCRIPTION_ERROR, msg.getString("subscriptionError", null));    // str   Subscription error string (Added in version 20).
        values.put(DataContract.Recordings.STREAM_ERRORS, msg.getString("streamErrors", null)); // str   Number of recording errors (Added in version 20).
        values.put(DataContract.Recordings.DATA_ERRORS, msg.getString("dataErrors", null));     // str   Number of stream data errors (Added in version 20).
        values.put(DataContract.Recordings.PATH, msg.getString("path", null));                  // str   Recording path for playback.
        values.put(DataContract.Recordings.DATA_SIZE, msg.getInt("dataSize", 0));               // s64   Actual file size of the last recordings (Added in version 21).
        values.put(DataContract.Recordings.ENABLED, msg.getInt("enabled", 0));                  // u32   Enabled flag (Added in version 23).

        getContentResolver().update(DataContract.Recordings.CONTENT_URI, values,
                DataContract.Recordings.ID + "=?", new String[]{String.valueOf(id)});

        // TODO remove old stuff when possible

        Recording rec = ds.getRecording(msg.getLong("id"));
        if (rec == null) {
            return;
        }

        rec.description = msg.getString("description", rec.description);
        rec.summary = msg.getString("summary", rec.summary);
        rec.error = msg.getString("error", rec.error);
        rec.start = msg.getDate("start");
        rec.state = msg.getString("state", rec.state);
        rec.stop = msg.getDate("stop");
        rec.title = msg.getString("title", rec.title);
        rec.subtitle = msg.getString("subtitle", rec.subtitle);
        rec.enabled = msg.getLong("enabled", 1) != 0;

        // Not all fields can be set with default values, so check if the server
        // provides a supported HTSP API version. These entries are available
        // only on version 13 and higher
        if (connection.getProtocolVersion() >= Constants.MIN_API_VERSION_SERIES_RECORDINGS) {
            rec.eventId = msg.getLong("eventId", 0);
            rec.autorecId = msg.getString("autorecId");
            rec.startExtra = msg.getLong("startExtra", 0);
            rec.stopExtra = msg.getLong("stopExtra", 0);
            rec.retention = msg.getLong("retention");
            rec.priority = msg.getLong("priority");
            rec.contentType = msg.getLong("contentType", -1);
        }

        // Not all fields can be set with default values, so check if the server
        // provides a supported HTSP API version. These entries are available
        // only on version 17 and higher
        if (connection.getProtocolVersion() >= Constants.MIN_API_VERSION_TIMER_RECORDINGS) {
            rec.timerecId = msg.getString("timerecId");
        }

        rec.episode = msg.getString("episode", null);
        rec.comment = msg.getString("comment", null);
        rec.subscriptionError = msg.getString("subscriptionError", null);
        rec.streamErrors = msg.getLong("streamErrors", 0);
        rec.dataErrors = msg.getLong("dataErrors", 0);
        rec.dataSize = msg.getLong("dataSize", 0);
        rec.owner = msg.getString("owner", rec.owner);
        rec.creator = msg.getString("creator", rec.creator);
        rec.path = msg.getString("path", rec.path);
        rec.files = msg.getString("files", rec.files);

        ds.updateRecording(rec);
    }

    /**
     * Server to client method.
     * A recording has been deleted on the server.
     *
     * @param msg The message with the recording id that was deleted
     */
    private void onDvrEntryDelete(HTSMessage msg) {
        Log.d(TAG, "onDvrEntryDelete() called");

        int id = msg.getInt("id");
        getContentResolver().delete(DataContract.Recordings.CONTENT_URI,
                DataContract.Recordings.ID + "=?", new String[]{String.valueOf(id)});

        // TODO remove old stuff when possible

        Recording rec = ds.getRecording(msg.getLong("id"));

        if (rec == null || rec.channel == null) {
            return;
        }

        rec.channel.recordings.remove(rec);
        for (Program p : rec.channel.epg) {
            if (p.recording == rec) {
                p.recording = null;
                ds.updateProgram(p);
                break;
            }
        }
        ds.removeRecording(rec);
    }

    /**
     * Server to client method.
     * A series recording has been added on the server.
     *
     * @param msg The message with the new series recording data
     */
    private void onAutorecEntryAdd(HTSMessage msg) {
        Log.d(TAG, "onAutorecEntryAdd() called");

        ContentValues values = new ContentValues();
        values.put(DataContract.SeriesRecordings.ID, msg.getString("id"));                      // str   required   ID (string!) of dvrAutorecEntry.
        values.put(DataContract.SeriesRecordings.ENABLED, msg.getInt("enabled"));               // u32   required   If autorec entry is enabled (activated).
        values.put(DataContract.SeriesRecordings.NAME, msg.getString("name"));                  // str   required   Name of the autorec entry (Added in version 18).
        values.put(DataContract.SeriesRecordings.MIN_DURATION, msg.getInt("minDuration"));      // u32   required   Minimal duration in seconds (0 = Any).
        values.put(DataContract.SeriesRecordings.MAX_DURATION, msg.getInt("maxDuration"));      // u32   required   Maximal duration in seconds (0 = Any).
        values.put(DataContract.SeriesRecordings.RETENTION, msg.getInt("retention"));           // u32   required   Retention time (in days).
        values.put(DataContract.SeriesRecordings.DAYS_OF_WEEK, msg.getInt("daysOfWeek"));       // u32   required   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
        values.put(DataContract.SeriesRecordings.PRIORITY, msg.getInt("priority"));             // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
        values.put(DataContract.SeriesRecordings.APPROX_TIME, msg.getInt("approxTime"));        // u32   required   Minutes from midnight (up to 24*60).
        values.put(DataContract.SeriesRecordings.START, msg.getInt("start"));                   // s32   required   Exact start time (minutes from midnight) (Added in version 18).
        values.put(DataContract.SeriesRecordings.START_WINDOW, msg.getInt("startWindow"));      // s32   required   Exact stop time (minutes from midnight) (Added in version 18).
        values.put(DataContract.SeriesRecordings.START_EXTRA, msg.getInt("startExtra"));        // s64   required   Extra start minutes (pre-time).
        values.put(DataContract.SeriesRecordings.STOP_EXTRA, msg.getInt("stopExtra"));          // s64   required   Extra stop minutes (post-time).
        values.put(DataContract.SeriesRecordings.TITLE, msg.getString("title", null));          // str   optional   Title.
        values.put(DataContract.SeriesRecordings.FULLTEXT, msg.getInt("fulltext", 0));          // u32   optional   Fulltext flag (Added in version 20).
        values.put(DataContract.SeriesRecordings.DIRECTORY, msg.getString("directory", null));  // str   optional   Forced directory name (Added in version 19).
        values.put(DataContract.SeriesRecordings.CHANNEL, msg.getInt("channel", 0));            // u32   optional   Channel ID.
        values.put(DataContract.SeriesRecordings.OWNER, msg.getString("owner", null));          // str   optional   Owner of this autorec entry (Added in version 18).
        values.put(DataContract.SeriesRecordings.CREATOR, msg.getString("creator", null));      // str   optional   Creator of this autorec entry (Added in version 18).
        values.put(DataContract.SeriesRecordings.DUP_DETECT, msg.getInt("dupDetect", 0));       // u32   optional   Duplicate detection (see addAutorecEntry) (Added in version 20).

        getContentResolver().insert(DataContract.SeriesRecordings.CONTENT_URI, values);

        // TODO remove old stuff when possible

        SeriesRecording srec = new SeriesRecording();
        srec.id = msg.getString("id");
        srec.enabled = msg.getLong("enabled", 0) != 0;
        srec.maxDuration = msg.getLong("maxDuration");
        srec.minDuration = msg.getLong("minDuration");
        srec.retention = msg.getLong("retention");
        srec.daysOfWeek = msg.getLong("daysOfWeek");
        srec.approxTime = msg.getLong("approxTime", -1);
        srec.priority = msg.getLong("priority");
        srec.start = msg.getLong("start", -1);
        srec.startWindow = msg.getLong("startWindow", -1);
        srec.startExtra = msg.getLong("startExtra", 0);
        srec.stopExtra = msg.getLong("stopExtra", 0);
        srec.dupDetect = msg.getLong("dupDetect", 0);
        srec.title = msg.getString("title");
        srec.name = msg.getString("name");
        srec.directory = msg.getString("directory");
        srec.channel = ds.getChannel(msg.getLong("channel", 0));
        srec.fulltext = msg.getString("fulltext");

        ds.addSeriesRecording(srec);
    }

    /**
     * Server to client method.
     * A series recording has been updated on the server.
     *
     * @param msg The message with the updated series recording data
     */
    private void onAutorecEntryUpdate(HTSMessage msg) {
        Log.d(TAG, "onAutorecEntryUpdate() called");

        String id = msg.getString("id");                                                        // str   ID (string!) of dvrAutorecEntry.
        ContentValues values = new ContentValues();
        values.put(DataContract.SeriesRecordings.ENABLED, msg.getInt("enabled"));               // u32   If autorec entry is enabled (activated).
        values.put(DataContract.SeriesRecordings.NAME, msg.getString("name"));                  // str   Name of the autorec entry (Added in version 18).
        values.put(DataContract.SeriesRecordings.MIN_DURATION, msg.getInt("minDuration"));      // u32   Minimal duration in seconds (0 = Any).
        values.put(DataContract.SeriesRecordings.MAX_DURATION, msg.getInt("maxDuration"));      // u32   Maximal duration in seconds (0 = Any).
        values.put(DataContract.SeriesRecordings.RETENTION, msg.getInt("retention"));           // u32   Retention time (in days).
        values.put(DataContract.SeriesRecordings.DAYS_OF_WEEK, msg.getInt("daysOfWeek"));       // u32   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
        values.put(DataContract.SeriesRecordings.PRIORITY, msg.getInt("priority"));             // u32   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
        values.put(DataContract.SeriesRecordings.APPROX_TIME, msg.getInt("approxTime"));        // u32   Minutes from midnight (up to 24*60).
        values.put(DataContract.SeriesRecordings.START, msg.getInt("start"));                   // s32   Exact start time (minutes from midnight) (Added in version 18).
        values.put(DataContract.SeriesRecordings.START_WINDOW, msg.getInt("startWindow"));      // s32   Exact stop time (minutes from midnight) (Added in version 18).
        values.put(DataContract.SeriesRecordings.START_EXTRA, msg.getInt("startExtra"));        // s64   Extra start minutes (pre-time).
        values.put(DataContract.SeriesRecordings.STOP_EXTRA, msg.getInt("stopExtra"));          // s64   Extra stop minutes (post-time).
        values.put(DataContract.SeriesRecordings.TITLE, msg.getString("title", null));          // str   Title.
        values.put(DataContract.SeriesRecordings.FULLTEXT, msg.getInt("fulltext", 0));          // u32   Fulltext flag (Added in version 20).
        values.put(DataContract.SeriesRecordings.DIRECTORY, msg.getString("directory", null));  // str   Forced directory name (Added in version 19).
        values.put(DataContract.SeriesRecordings.CHANNEL, msg.getInt("channel", 0));            // u32   Channel ID.
        values.put(DataContract.SeriesRecordings.OWNER, msg.getString("owner", null));          // str   Owner of this autorec entry (Added in version 18).
        values.put(DataContract.SeriesRecordings.CREATOR, msg.getString("creator", null));      // str   Creator of this autorec entry (Added in version 18).
        values.put(DataContract.SeriesRecordings.DUP_DETECT, msg.getInt("dupDetect", 0));       // u32   Duplicate detection (see addAutorecEntry) (Added in version 20).

        getContentResolver().update(DataContract.SeriesRecordings.CONTENT_URI, values,
                DataContract.SeriesRecordings.ID + "=?", new String[]{id});

        // TODO remove old stuff when possible

        SeriesRecording srec = ds.getSeriesRecording(msg.getString("id"));
        if (srec == null) {
            return;
        }
        srec.enabled = msg.getLong("enabled", 0) != 0;
        srec.maxDuration = msg.getLong("maxDuration");
        srec.minDuration = msg.getLong("minDuration");
        srec.retention = msg.getLong("retention");
        srec.daysOfWeek = msg.getLong("daysOfWeek");
        srec.approxTime = msg.getLong("approxTime", -1);
        srec.priority = msg.getLong("priority");
        srec.start = msg.getLong("start", -1);
        srec.startWindow = msg.getLong("startWindow", -1);
        srec.startExtra = msg.getLong("startExtra", 0);
        srec.stopExtra = msg.getLong("stopExtra", 0);
        srec.dupDetect = msg.getLong("dupDetect", 0);
        srec.title = msg.getString("title", srec.title);
        srec.name = msg.getString("name", srec.name);
        srec.directory = msg.getString("directory", srec.directory);
        srec.fulltext = msg.getString("fulltext", srec.fulltext);

        ds.updateSeriesRecording(srec);
    }

    /**
     * Server to client method.
     * A series recording has been deleted on the server.
     *
     * @param msg The message with the series recording id that was deleted
     */
    private void onAutorecEntryDelete(HTSMessage msg) {
        Log.d(TAG, "onAutorecEntryDelete() called");

        String id = msg.getString("id");
        getContentResolver().delete(DataContract.SeriesRecordings.CONTENT_URI,
                DataContract.SeriesRecordings.ID + "=?", new String[]{id});

        // TODO remove old stuff when possible

        if (id == null) {
            return;
        }
        // Remove the series recording from the list and also update all
        // recordings by removing the series id
        ds.removeSeriesRecording(id);
        for (Recording rec : ds.getRecordings()) {
            if (rec.autorecId != null && rec.autorecId.equals(id)) {
                rec.autorecId = null;
            }
        }
    }

    /**
     * Server to client method.
     * A timer recording has been added on the server.
     *
     * @param msg The message with the new timer recording data
     */
    private void onTimerRecEntryAdd(HTSMessage msg) {
        Log.d(TAG, "onTimerRecEntryAdd() called");

        ContentValues values = new ContentValues();
        values.put(DataContract.TimerRecordings.ID, msg.getString("id"));                       // str   required   ID (string!) of timerecEntry.
        values.put(DataContract.TimerRecordings.TITLE, msg.getString("title"));                 // str   required   Title for the recordings.
        values.put(DataContract.TimerRecordings.DIRECTORY, msg.getString("directory", null));   // str   optional   Forced directory name (Added in version 19).
        values.put(DataContract.TimerRecordings.ENABLED, msg.getInt("enabled"));                // u32   required   Title for the recordings.
        values.put(DataContract.TimerRecordings.NAME, msg.getString("name"));                   // str   required   Name for this timerec entry.
        values.put(DataContract.TimerRecordings.CONFIG_NAME, msg.getString("configName"));      // str   required   DVR Configuration Name / UUID.
        values.put(DataContract.TimerRecordings.CHANNEL, msg.getInt("channel"));                // u32   required   Channel ID.
        values.put(DataContract.TimerRecordings.DAYS_OF_WEEK, msg.getInt("daysOfWeek"));        // u32   optional   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
        values.put(DataContract.TimerRecordings.PRIORITY, msg.getInt("priority"));              // u32   optional   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
        values.put(DataContract.TimerRecordings.START, msg.getInt("start"));                    // u32   required   Minutes from midnight (up to 24*60) for the start of the time window (including)
        values.put(DataContract.TimerRecordings.STOP, msg.getInt("stop"));                      // u32   required   Minutes from modnight (up to 24*60) for the end of the time window (including, cross-noon allowed)
        values.put(DataContract.TimerRecordings.RETENTION, msg.getInt("retention"));            // u32   optional   Retention in days.
        values.put(DataContract.TimerRecordings.OWNER, msg.getString("owner"));                 // str   optional   Owner of this timerec entry.
        values.put(DataContract.TimerRecordings.CREATOR, msg.getString("creator"));             // str   optional   Creator of this timerec entry.

        getContentResolver().insert(DataContract.TimerRecordings.CONTENT_URI, values);

        // TODO remove old stuff when possible

        TimerRecording rec = new TimerRecording();
        rec.id = msg.getString("id", "");

        rec.daysOfWeek = msg.getLong("daysOfWeek", 0);
        rec.retention = msg.getLong("retention", 0);
        rec.priority = msg.getLong("priority", 0);
        rec.start = msg.getLong("start", 0);
        rec.stop = msg.getLong("stop", 0);
        rec.title = msg.getString("title");
        rec.name = msg.getString("name");
        rec.directory = msg.getString("directory");
        rec.channel = ds.getChannel(msg.getLong("channel", 0));

        // The enabled flag was added in HTSP API version 18. The support for
        // timer recordings are available since version 17.
        if (connection.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_ENABLED) {
            rec.enabled = msg.getLong("enabled", 0) != 0;
        }
        ds.addTimerRecording(rec);
    }

    /**
     * Server to client method.
     * A timer recording has been updated on the server.
     *
     * @param msg The message with the updated timer recording data
     */
    private void onTimerRecEntryUpdate(HTSMessage msg) {
        Log.d(TAG, "onTimerRecEntryUpdate() called");

        String id = msg.getString("id");                                                        // str   ID (string!) of timerecEntry.
        ContentValues values = new ContentValues();
        values.put(DataContract.TimerRecordings.TITLE, msg.getString("title"));                 // str   Title for the recordings.
        values.put(DataContract.TimerRecordings.DIRECTORY, msg.getString("directory", null));   // str   Forced directory name (Added in version 19).
        values.put(DataContract.TimerRecordings.ENABLED, msg.getInt("enabled"));                // u32   Title for the recordings.
        values.put(DataContract.TimerRecordings.NAME, msg.getString("name"));                   // str   Name for this timerec entry.
        values.put(DataContract.TimerRecordings.CONFIG_NAME, msg.getString("configName"));      // str   DVR Configuration Name / UUID.
        values.put(DataContract.TimerRecordings.CHANNEL, msg.getInt("channel"));                // u32   Channel ID.
        values.put(DataContract.TimerRecordings.DAYS_OF_WEEK, msg.getInt("daysOfWeek"));        // u32   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
        values.put(DataContract.TimerRecordings.PRIORITY, msg.getInt("priority"));              // u32   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
        values.put(DataContract.TimerRecordings.START, msg.getInt("start"));                    // u32   Minutes from midnight (up to 24*60) for the start of the time window (including)
        values.put(DataContract.TimerRecordings.STOP, msg.getInt("stop"));                      // u32   Minutes from modnight (up to 24*60) for the end of the time window (including, cross-noon allowed)
        values.put(DataContract.TimerRecordings.RETENTION, msg.getInt("retention"));            // u32   Retention in days.
        values.put(DataContract.TimerRecordings.OWNER, msg.getString("owner"));                 // str   Owner of this timerec entry.
        values.put(DataContract.TimerRecordings.CREATOR, msg.getString("creator"));             // str   Creator of this timerec entry.

        getContentResolver().update(DataContract.TimerRecordings.CONTENT_URI, values,
                DataContract.TimerRecordings.ID + "=?", new String[]{id});

        // TODO remove old stuff when possible

        TimerRecording rec = ds.getTimerRecording(msg.getString("id"));
        if (rec == null) {
            return;
        }

        rec.daysOfWeek = msg.getLong("daysOfWeek", rec.daysOfWeek);
        rec.retention = msg.getLong("retention", rec.retention);
        rec.priority = msg.getLong("priority", rec.priority);
        rec.start = msg.getLong("start", rec.start);
        rec.stop = msg.getLong("stop", rec.stop);
        rec.title = msg.getString("title", rec.title);
        rec.name = msg.getString("name", rec.name);
        rec.directory = msg.getString("directory", rec.name);
        rec.channel = ds.getChannel(msg.getLong("channel", 0));

        // The enabled flag was added in HTSP API version 18. The support for
        // timer recordings are available since version 17.
        if (connection.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_ENABLED) {
            rec.enabled = msg.getLong("enabled", 0) != 0;
        }
        ds.updateTimerRecording(rec);
    }

    /**
     * Server to client method.
     * A timer recording has been deleted on the server.
     *
     * @param msg The message with the recording id that was deleted
     */
    private void onTimerRecEntryDelete(HTSMessage msg) {
        Log.d(TAG, "onTimerRecEntryDelete() called");

        String id = msg.getString("id");
        getContentResolver().delete(DataContract.TimerRecordings.CONTENT_URI,
                DataContract.TimerRecordings.ID + "=?", new String[]{id});

        // TODO remove old stuff when possible

        TimerRecording rec = ds.getTimerRecording(msg.getString("id"));

        if (rec == null || rec.channel == null) {
            return;
        }

        rec.channel = null;
        ds.removeTimerRecording(rec);
    }

    /**
     * Server to client method.
     * An epg event has been added on the server.
     *
     * @param msg The message with the new epg event data
     */
    private void onEventAdd(HTSMessage msg) {
        Log.d(TAG, "onEventAdd() called");

        ContentValues values = new ContentValues();
        values.put(DataContract.Programs.ID, msg.getInt("eventId"));                          // u32   required   Event ID
        values.put(DataContract.Programs.CHANNEL_ID, msg.getInt("channelId"));                // u32   required   The channel this event is related to.
        values.put(DataContract.Programs.START, msg.getInt("start"));                         // u64   required   Start time of event, UNIX time.
        values.put(DataContract.Programs.STOP, msg.getInt("stop"));                           // u64   required   Ending time of event, UNIX time.
        values.put(DataContract.Programs.TITLE, msg.getString("title", null));                // str   optional   Title of event.
        values.put(DataContract.Programs.SUMMARY, msg.getString("summary", null));            // str   optional   Short description of the event (Added in version 6).
        values.put(DataContract.Programs.DESCRIPTION, msg.getString("description", null));    // str   optional   Long description of the event.
        values.put(DataContract.Programs.SERIES_LINK_ID, msg.getInt("serieslinkId", 0));      // u32   optional   Series Link ID (Added in version 6).
        values.put(DataContract.Programs.EPISODE_ID, msg.getInt("episodeId", 0));             // u32   optional   Episode ID (Added in version 6).
        values.put(DataContract.Programs.SEASON_ID, msg.getInt("seasonId", 0));               // u32   optional   Season ID (Added in version 6).
        values.put(DataContract.Programs.BRAND_ID, msg.getInt("brandId", 0));                 // u32   optional   Brand ID (Added in version 6).
        values.put(DataContract.Programs.TYPE_OF_CONTENT, msg.getInt("contentType", 0));      // u32   optional   DVB content code (Added in version 4, Modified in version 6*).
        values.put(DataContract.Programs.AGE_RATING, msg.getInt("ageRating", 0));             // u32   optional   Minimum age rating (Added in version 6).
        values.put(DataContract.Programs.STAR_RATING, msg.getInt("starRating", 0));           // u32   optional   Star rating (1-5) (Added in version 6).
        values.put(DataContract.Programs.FIRST_AIRED, msg.getInt("firstAired", 0));           // s64   optional   Original broadcast time, UNIX time (Added in version 6).
        values.put(DataContract.Programs.SEASON_NUMBER, msg.getInt("seasonNumber", 0));       // u32   optional   Season number (Added in version 6).
        values.put(DataContract.Programs.SEASON_COUNT, msg.getInt("seasonCount", 0));         // u32   optional   Show season count (Added in version 6).
        values.put(DataContract.Programs.EPISODE_NUMBER, msg.getInt("episodeNumber", 0));     // u32   optional   Episode number (Added in version 6).
        values.put(DataContract.Programs.EPISODE_COUNT, msg.getInt("episodeCount", 0));       // u32   optional   Season episode count (Added in version 6).
        values.put(DataContract.Programs.PART_NUMBER, msg.getInt("partNumber", 0));           // u32   optional   Multi-part episode part number (Added in version 6).
        values.put(DataContract.Programs.PART_COUNT, msg.getInt("partCount", 0));             // u32   optional   Multi-part episode part count (Added in version 6).
        values.put(DataContract.Programs.EPISODE_ON_SCREEN, msg.getString("episodeOnscreen", null));  // str   optional   Textual representation of episode number (Added in version 6).
        values.put(DataContract.Programs.IMAGE, msg.getString("image", null));                // str   optional   URL to a still capture from the episode (Added in version 6).
        values.put(DataContract.Programs.DVR_ID, msg.getInt("dvrId", 0));                     // u32   optional   ID of a recording (Added in version 5).
        values.put(DataContract.Programs.NEXT_EVENT_ID, msg.getInt("nextEventId", 0));        // u32   optional   ID of next event on the same channel.

        getContentResolver().insert(DataContract.Programs.CONTENT_URI, values);
    }

    /**
     * Server to client method.
     * An epg event has been updated on the server.
     *
     * @param msg The message with the updated epg event data
     */
    private void onEventUpdate(HTSMessage msg) {
        Log.d(TAG, "onEventUpdate() called");

        int id = msg.getInt("eventId");
        ContentValues values = new ContentValues();
        values.put(DataContract.Programs.ID, msg.getInt("eventId"));                          // u32   Event ID
        values.put(DataContract.Programs.CHANNEL_ID, msg.getInt("channelId"));                // u32   The channel this event is related to.
        values.put(DataContract.Programs.START, msg.getInt("start"));                         // u64   Start time of event, UNIX time.
        values.put(DataContract.Programs.STOP, msg.getInt("stop"));                           // u64   Ending time of event, UNIX time.
        values.put(DataContract.Programs.TITLE, msg.getString("title", null));                // str   Title of event.
        values.put(DataContract.Programs.SUMMARY, msg.getString("summary", null));            // str   Short description of the event (Added in version 6).
        values.put(DataContract.Programs.DESCRIPTION, msg.getString("description", null));    // str   Long description of the event.
        values.put(DataContract.Programs.SERIES_LINK_ID, msg.getInt("serieslinkId", 0));      // u32   Series Link ID (Added in version 6).
        values.put(DataContract.Programs.EPISODE_ID, msg.getInt("episodeId", 0));             // u32   Episode ID (Added in version 6).
        values.put(DataContract.Programs.SEASON_ID, msg.getInt("seasonId", 0));               // u32   Season ID (Added in version 6).
        values.put(DataContract.Programs.BRAND_ID, msg.getInt("brandId", 0));                 // u32   Brand ID (Added in version 6).
        values.put(DataContract.Programs.TYPE_OF_CONTENT, msg.getInt("contentType", 0));      // u32   DVB content code (Added in version 4, Modified in version 6*).
        values.put(DataContract.Programs.AGE_RATING, msg.getInt("ageRating", 0));             // u32   Minimum age rating (Added in version 6).
        values.put(DataContract.Programs.STAR_RATING, msg.getInt("starRating", 0));           // u32   Star rating (1-5) (Added in version 6).
        values.put(DataContract.Programs.FIRST_AIRED, msg.getInt("firstAired", 0));           // s64   Original broadcast time, UNIX time (Added in version 6).
        values.put(DataContract.Programs.SEASON_NUMBER, msg.getInt("seasonNumber", 0));       // u32   Season number (Added in version 6).
        values.put(DataContract.Programs.SEASON_COUNT, msg.getInt("seasonCount", 0));         // u32   Show season count (Added in version 6).
        values.put(DataContract.Programs.EPISODE_NUMBER, msg.getInt("episodeNumber", 0));     // u32   Episode number (Added in version 6).
        values.put(DataContract.Programs.EPISODE_COUNT, msg.getInt("episodeCount", 0));       // u32   Season episode count (Added in version 6).
        values.put(DataContract.Programs.PART_NUMBER, msg.getInt("partNumber", 0));           // u32   Multi-part episode part number (Added in version 6).
        values.put(DataContract.Programs.PART_COUNT, msg.getInt("partCount", 0));             // u32   Multi-part episode part count (Added in version 6).
        values.put(DataContract.Programs.EPISODE_ON_SCREEN, msg.getString("episodeOnscreen", null));  // str   Textual representation of episode number (Added in version 6).
        values.put(DataContract.Programs.IMAGE, msg.getString("image", null));                // str   URL to a still capture from the episode (Added in version 6).
        values.put(DataContract.Programs.DVR_ID, msg.getInt("dvrId", 0));                     // u32   ID of a recording (Added in version 5).
        values.put(DataContract.Programs.NEXT_EVENT_ID, msg.getInt("nextEventId", 0));        // u32   ID of next event on the same channel.

        getContentResolver().update(DataContract.Programs.CONTENT_URI, values,
                DataContract.Programs.ID + "=?", new String[]{String.valueOf(id)});
    }

    /**
     * Server to client method.
     * An epg event has been deleted on the server.
     *
     * @param msg The message with the epg event id that was deleted
     */
    private void onEventDelete(HTSMessage msg) {
        Log.d(TAG, "onEventDelete() called");

        int id = msg.getInt("eventId");
        getContentResolver().delete(DataContract.Programs.CONTENT_URI,
                DataContract.Programs.ID + "=?", new String[]{String.valueOf(id)});
    }
}
