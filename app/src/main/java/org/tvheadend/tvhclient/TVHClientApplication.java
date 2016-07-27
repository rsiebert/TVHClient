package org.tvheadend.tvhclient;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.HttpTicket;
import org.tvheadend.tvhclient.model.Packet;
import org.tvheadend.tvhclient.model.Profiles;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.model.Subscription;
import org.tvheadend.tvhclient.model.TimerRecording;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TVHClientApplication extends Application implements IabHelper.QueryInventoryFinishedListener, IabHelper.OnIabPurchaseFinishedListener {

    private final static String TAG = TVHClientApplication.class.getSimpleName();

    private final List<HTSListener> listeners = new ArrayList<>();
    private final List<ChannelTag> tags = Collections.synchronizedList(new ArrayList<ChannelTag>());
    private final List<Channel> channels = Collections.synchronizedList(new ArrayList<Channel>());
    private final List<Recording> recordings = Collections.synchronizedList(new ArrayList<Recording>());
    private final List<SeriesRecording> seriesRecordings = Collections.synchronizedList(new ArrayList<SeriesRecording>());
    private final List<TimerRecording> timerRecordings = Collections.synchronizedList(new ArrayList<TimerRecording>());
    private final List<Subscription> subscriptions = Collections.synchronizedList(new ArrayList<Subscription>());
    private final List<Profiles> dvrConfigs = Collections.synchronizedList(new ArrayList<Profiles>());
    private final List<Profiles> profiles = Collections.synchronizedList(new ArrayList<Profiles>());
    private final Map<String, String> status = Collections.synchronizedMap(new HashMap<String, String>());

    // This handles all billing related activities like purchasing and checking
    // if a purchase was made
    OpenIabHelper openIabHelper;
    private static final int RC_REQUEST = 10001;
    private boolean isUnlocked = false;
    private Boolean isBillingSetupDone = false;

    // File name and path for the internal logging functionality
    private File logPath = null;
    private BufferedOutputStream logfileBuffer = null;
    // The prefix with the date in each log entry
    private final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    // Indication that data is being loaded
    private volatile boolean loading = false;

    // The default protocol version that is assumed the server supports
    private int protocolVersion = 10;
    private String serverName = "";
    private String serverVersion = "";

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * Adds a single listener to the list.
     * 
     * @param listener Listener class
     */
    public void addListener(HTSListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a single listener from the list.
     * 
     * @param listener Listener class
     */
    public void removeListener(HTSListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Sends the given action and possible object with the data to all
     * registered listeners.
     * 
     * @param action String that defines the action
     * @param obj Object that contains data
     */
    private void broadcastMessage(final String action, final Object obj) {
        synchronized (listeners) {
            for (HTSListener l : listeners) {
                l.onMessage(action, obj);
            }
        }
    }

    public void showMessage(String msg) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_SHOW_MESSAGE, msg);
        }
    }

    /**
     * Informs all listeners about the current connection state.
     */
    public void setConnectionState(final String state) {
        broadcastMessage(state, null);
    }

    /**
     * Sets the protocol version of the currently active connection. This is
     * required to determine if the server supports series recordings and other
     * stuff.
     *
     * @param version Version of the server protocol
     */
    public void setProtocolVersion(final int version) {
        protocolVersion = version;
    }

    public void setServerName(String name) {
        serverName = name;
    }

    public void setServerVersion(String version) {
        serverVersion = version;
    }

    /**
     * Returns the protocol version of the currently active connection. This is
     * required to determine if the server supports series recordings and other
     * stuff.
     *
     * @return Version of the server protocol
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * Sends the given packet object for video playback to all registered
     * listeners
     * 
     * @param p Packet
     */
    public void broadcastPacket(Packet p) {
        broadcastMessage(Constants.ACTION_PLAYBACK_PACKET, p);
    }

    /**
     * Returns the list of available channel tags
     * 
     * @return List of all channel tags
     */
    public List<ChannelTag> getChannelTags() {
        synchronized (tags) {

            // Sort the channel tags ny name, but keep the all channels always on top
            Collections.sort(tags, new Comparator<ChannelTag>() {
                public int compare(ChannelTag x, ChannelTag y) {
                    if (x != null && y != null && x.name != null && y.name != null) {
                        if (y.name.equals(getString(R.string.all_channels))) {
                            return 1;
                        } else {
                            return x.name.toLowerCase(Locale.getDefault())
                                    .compareTo(y.name.toLowerCase(Locale.getDefault()));
                        }
                    }
                    return 0;
                }
            });
            return tags;
        }
    }

    /**
     * Adds the new channel tag to the list. If loading is not in progress all
     * registered listeners will be informed.
     * 
     * @param tag Channel tag
     */
    public void addChannelTag(ChannelTag tag) {
        synchronized (tags) {
            boolean channelTagExists = false;
            for (ChannelTag ct : getChannelTags()) {
                if (ct.id == tag.id) {
                    channelTagExists = true;
                    break;
                }
            }
            if (!channelTagExists) {
                tags.add(tag);
            }
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_TAG_ADD, tag);
        }
    }

    /**
     * Removes the given channel tag from the list. If loading is not in progress
     * all registered listeners will be informed.
     * 
     * @param tag Channel tag
     */
    public void removeChannelTag(ChannelTag tag) {
        synchronized (tags) {
            tags.remove(tag);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_TAG_DELETE, tag);
        }
    }
    
    /**
     * Removes the channel tag (given by the id) from the list. If loading is
     * not in progress all registered listeners will be informed.
     * 
     * @param id Id of the channel tag
     */
    public void removeChannelTag(long id) {
        synchronized (tags) {
            for (ChannelTag tag : getChannelTags()) {
                if (tag.id == id) {
                    removeChannelTag(tag);
                    return;
                }
            }
        }
    }

    /**
     * Returns the channel tag that matches the given id.
     * 
     * @param id Id of the channel tag
     * @return Channel tag
     */
    public ChannelTag getChannelTag(long id) {
        synchronized (tags) {
            for (ChannelTag tag : getChannelTags()) {
                if (tag.id == id) {
                    return tag;
                }
            }
        }
        return null;
    }

    /**
     * If loading has finished any listener will be informed that this channel
     * tag has been updated.
     * 
     * @param tag ChannelTag
     */
    public void updateChannelTag(ChannelTag tag) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_TAG_UPDATE, tag);
        }
    }

    /**
     * Adds the given channel to the list of available channels.
     * 
     * @param channel Channel
     */
    public void addChannel(Channel channel) {
        synchronized (channels) {
            boolean channelExists = false;
            for (Channel ch : getChannels()) {
                if (ch.id == channel.id) {
                    channelExists = true;
                    break;
                }
            }
            if (!channelExists) {
                channels.add(channel);
            }
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_CHANNEL_ADD, channel);
        }
    }

    /**
     * Returns the list of all available channels.
     * 
     * @return List of all channels
     */
    public List<Channel> getChannels() {
        synchronized (channels) {
            return channels;
        }
    }

    /**
     * Removes the given channel from the list of available channels. If loading
     * has finished any listener will be informed that this channel has been
     * removed.
     * 
     * @param channel Channel
     */
    public void removeChannel(Channel channel) {
        synchronized (channels) {
            channels.remove(channel);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_CHANNEL_DELETE, channel);
        }
    }

    /**
     * Returns the channel that matches the given id.
     * 
     * @param id Id of the channel
     * @return Channel
     */
    public Channel getChannel(long id) {
        synchronized (channels) {
            for (Channel ch : getChannels()) {
                if (ch.id == id) {
                    return ch;
                }
            }
        }
        return null;
    }

    /**
     * Removes the channel from the list of available channels that matches the
     * given id. Any listener will be informed about that removal. 
     * 
     * @param id Id of the channel
     */
    public void removeChannel(long id) {
        synchronized (channels) {
            for (Channel ch : getChannels()) {
                if (ch.id == id) {
                    removeChannel(ch);
                    return;
                }
            }
        }
    }

    /**
     * If loading has finished any listener will be informed that the given
     * channel has been updated.
     *  
     * @param ch Channel
     */
    public void updateChannel(Channel ch) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_CHANNEL_UPDATE, ch);
        }
    }

    /**
     * If loading has finished any listener will be informed that the given
     * program has been added.
     * 
     * @param p Program
     */
    public void addProgram(Program p) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_PROGRAM_ADD, p);
        }
    }

    /**
     * If loading has finished any listener will be informed that the given
     * program has been deleted.
     * 
     * @param p Program
     */
    public void removeProgram(Program p) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_PROGRAM_DELETE, p);
        }
    }

    /**
     * If loading has finished any listener will be informed that the given
     * program has been updated.
     * 
     * @param p Program
     */
    public void updateProgram(Program p) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_PROGRAM_UPDATE, p);
        }
    }

    /**
     * Adds the given recording to the list of available recordings. If loading
     * has finished any listener will be informed that a recording has been
     * added.
     * 
     * @param rec Recording
     */
    public void addRecording(Recording rec) {
        synchronized (recordings) {
            if (recordings.indexOf(rec) == -1) {
                recordings.add(rec);
            }
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_DVR_ADD, rec);

            // Add a notification for scheduled recordings
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean("pref_show_notifications", false)
                    && rec.error == null && rec.state.equals("scheduled")) {
                addNotification(rec.id);
            }
        }
    }

    /**
     * Returns the list of all available recordings.
     * 
     * @return List of all recordings
     */
    public List<Recording> getRecordings() {
        synchronized (recordings) {
            return recordings;
        }
    }

    /**
     * Returns a single recording that matches the given id.
     * 
     * @param id The id of the recording
     * @return Recording
     */
    public Recording getRecording(long id) {
        synchronized (recordings) {
            for (Recording rec : getRecordings()) {
                if (rec.id == id) {
                    return rec;
                }
            }
        }
        return null;
    }

    /**
     * Returns a single recording that matches the given type. The type
     * identifies if a recording is completed, scheduled for recording or failed
     * due to some reason.
     * 
     * @param type The type of the recording
     * @return List of recordings of the given type
     */
    public List<Recording> getRecordingsByType(int type) {
        List<Recording> recs = new ArrayList<>();

        switch (type) {
        case Constants.RECORDING_TYPE_COMPLETED:
            synchronized (recordings) {
                for (Recording rec : recordings) {
                    // Include all recordings that are marked as completed, also
                    // include recordings marked as auto recorded
                    if (rec.error == null && rec.state.equals("completed")) {
                        recs.add(rec);
                    }
                }
            }
            break;

        case Constants.RECORDING_TYPE_SCHEDULED:
            synchronized (recordings) {
                for (Recording rec : recordings) {
                    // Include all scheduled recordings in the list, also
                    // include recordings marked as auto recorded
                    if (rec.error == null
                            && (rec.state.equals("scheduled") || rec.state.equals("recording"))) {
                        recs.add(rec);
                    }
                }
            }
            break;

        case Constants.RECORDING_TYPE_FAILED:
            synchronized (recordings) {
                for (Recording rec : recordings) {
                    // Include all failed recordings in the list
                    if ((rec.error != null || (rec.state.equals("missed") || rec.state.equals("invalid")))) {
                        recs.add(rec);
                    }
                }
            }
            break;
        }
        return recs;
    }

    /**
     * Removes the given recording from the list of all available recordings. If
     * loading has finished any listener will be informed that a recording has
     * been removed.
     * 
     * @param rec Recording
     */
    public void removeRecording(Recording rec) {
        synchronized (recordings) {
            recordings.remove(rec);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_DVR_DELETE, rec);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean("pref_show_notifications", false)) {
                log(TAG, "Recording was removed, cancel notification");
                cancelNotification(rec.id);
            }
        }
    }

    /**
     * Removes the recording from the list of all available recordings that
     * matches the given id.
     * 
     * @param id Id of the recording
     */
    @SuppressWarnings("unused")
    public void removeRecording(long id) {
        synchronized (recordings) {
            for (Recording rec : getRecordings()) {
                if (rec.id == id) {
                    removeRecording(rec);
                    return;
                }
            }
        }
    }

    /**
     * If loading has finished any listener will be informed that a recording
     * has been updated.
     * 
     * @param rec Recording
     */
    public void updateRecording(Recording rec) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_DVR_UPDATE, rec);
        }
    }

    /**
     * Returns the list of all available series recordings. If loading has
     * finished any listener will be informed that a series recording has been
     * added.
     * 
     * @param rec Series recording
     */
    public void addSeriesRecording(SeriesRecording rec) {
        synchronized (seriesRecordings) {
            boolean recordingFound = false;
            for (SeriesRecording sr : seriesRecordings) {
                if (sr.id.equals(rec.id)) {
                    recordingFound = true;
                    break;
                }
            }
            if (!recordingFound) {
                seriesRecordings.add(rec);
            }
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_SERIES_DVR_ADD, rec);
        }
    }

    /**
     * Adds the given series recording to the list of available series
     * recordings
     * 
     * @return List of all series recordings
     */
    public List<SeriesRecording> getSeriesRecordings() {
        synchronized (seriesRecordings) {
            return seriesRecordings;
        }
    }

    /**
     * Returns a single series recording that matches the given id.
     * 
     * @param id The id of the series recording
     * @return Series recording
     */
    public SeriesRecording getSeriesRecording(String id) {
        synchronized (seriesRecordings) {
            for (SeriesRecording srec : getSeriesRecordings()) {
                if (srec.id.equals(id)) {
                    return srec;
                }
            }
        }
        return null;
    }

    /**
     * Removes the given series recording from the list of all available series
     * recordings. If loading has finished any listener will be informed that a
     * series recording has been removed.
     * 
     * @param srec Series recording
     */
    public void removeSeriesRecording(SeriesRecording srec) {
        synchronized (seriesRecordings) {
            seriesRecordings.remove(srec);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_SERIES_DVR_DELETE, srec);
        }
    }

    /**
     * Removes the series recording from the list of all available series
     * recordings that matches the given id.
     * 
     * @param id The id of the series recording
     */
    public void removeSeriesRecording(String id) {
        synchronized (seriesRecordings) {
            for (SeriesRecording srec : getSeriesRecordings()) {
                if (srec.id.equals(id)) {
                    removeSeriesRecording(srec);
                    return;
                }
            }
        }
    }

    /**
     * If loading has finished any listener will be informed that a series
     * recording has been updated.
     * 
     * @param srec Series recording
     */
    public void updateSeriesRecording(SeriesRecording srec) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_SERIES_DVR_UPDATE, srec);
        }
    }

    /**
     * Returns the list of all available timer recordings. If loading has
     * finished any listener will be informed that a timer recording has been
     * added.
     * 
     * @param rec Timer recording
     */
    public void addTimerRecording(TimerRecording rec) {
        synchronized (timerRecordings) {
            if (timerRecordings.indexOf(rec) == -1) {
                timerRecordings.add(rec);
            }
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_TIMER_DVR_ADD, rec);
        }
    }

    /**
     * Adds the given timer recording to the list of available timer
     * recordings
     * 
     * @return List of all timer recordings
     */
    public List<TimerRecording> getTimerRecordings() {
        return timerRecordings;
    }

    /**
     * Returns a single timer recording that matches the given id.
     * 
     * @param id The id of the timer recording
     * @return Timer recording
     */
    public TimerRecording getTimerRecording(String id) {
        synchronized (timerRecordings) {
            for (TimerRecording rec : getTimerRecordings()) {
                if (rec.id.equals(id)) {
                    return rec;
                }
            }
        }
        return null;
    }

    /**
     * Removes the given timer recording from the list of all available timer
     * recordings. If loading has finished any listener will be informed that a
     * timer recording has been removed.
     * 
     * @param rec Timer recording
     */
    public void removeTimerRecording(TimerRecording rec) {
        synchronized (timerRecordings) {
            timerRecordings.remove(rec);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_TIMER_DVR_DELETE, rec);
        }
    }

    /**
     * Removes the timer recording from the list of all available timer
     * recordings that matches the given id.
     * 
     * @param id The id of the timer recording
     */
    public void removeTimerRecording(String id) {
        synchronized (timerRecordings) {
            for (TimerRecording rec : getTimerRecordings()) {
                if (rec.id.equals(id)) {
                    removeTimerRecording(rec);
                    return;
                }
            }
        }
    }

    /**
     * If loading has finished any listener will be informed that a series
     * recording has been updated.
     * 
     * @param rec Timer recording
     */
    public void updateTimerRecording(TimerRecording rec) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_TIMER_DVR_UPDATE, rec);
        }
    }

    /**
     * Informes all registered listeners about the loading status.
     * 
     * @param b The loading state
     */
    public void setLoading(boolean b) {
        if (loading != b) {
            broadcastMessage(Constants.ACTION_LOADING, b);
        }
        loading = b;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!loading && prefs.getBoolean("pref_show_notifications", false)) {
            final long offset = Integer.valueOf(prefs.getString("pref_show_notification_offset", "0"));
            addNotifications(offset);
        }
    }

    /**
     * Clears all channels, tags and recordings and series recordings from the
     * lists and sub lists. For the channel tags the default value will be set.
     */
    public void clearAll() {
        tags.clear();
        recordings.clear();
        seriesRecordings.clear();
        timerRecordings.clear();

        for (Channel ch : channels) {
            ch.epg.clear();
            ch.recordings.clear();
        }
        channels.clear();

        for (Subscription s : subscriptions) {
            s.streams.clear();
        }
        subscriptions.clear();

        // Add the default tag (all channels) to the list
        ChannelTag tag = new ChannelTag();
        tag.id = 0;
        tag.name = getString(R.string.all_channels);
        addChannelTag(tag);
    }

    /**
     * Adds the given subscription to the list of available subscriptions. If
     * loading has finished any listener will be informed that a subscription
     * has been added.
     * 
     * @param s Subscription
     */
    public void addSubscription(Subscription s) {
        synchronized (subscriptions) {
            subscriptions.add(s);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_SUBSCRIPTION_ADD, s);
        }
    }

    /**
     * Returns a list of all available subscriptions.
     * 
     * @return List with all subscriptions
     */
    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    /**
     * Removes the given subscription from the list of available subscriptions.
     * If loading has finished any listener will be informed that a subscription
     * has been removed.
     * 
     * @param s Subscription
     */
    public void removeSubscription(Subscription s) {
        s.streams.clear();
        synchronized (subscriptions) {
            subscriptions.remove(s);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_SUBSCRIPTION_DELETE, s);
        }
    }

    /**
     * Returns the subscription that matches the given id.
     * 
     * @param id The id of the subscription
     * @return Subscription
     */
    public Subscription getSubscription(long id) {
        synchronized (subscriptions) {
            for (Subscription s : getSubscriptions()) {
                if (s.id == id) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Removes the subscription from the list of available subscriptions that
     * matches the given id. If loading has finished any listener will be
     * informed that a subscription has been removed.
     * 
     * @param id The id of the subscription
     */
    public void removeSubscription(long id) {
        synchronized (subscriptions) {
            for (Subscription s : getSubscriptions()) {
                if (s.id == id) {
                    removeSubscription(s);
                    return;
                }
            }
        }
    }

    /**
     * If loading has finished any listener will be informed that a subscription
     * has been updated.
     */
    public void updateSubscription(Subscription s) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_SUBSCRIPTION_UPDATE, s);
        }
    }

    /**
     * Informs all listeners that the given ticket has been added.
     * 
     * @param t Ticket id
     */
    public void addTicket(HttpTicket t) {
        broadcastMessage(Constants.ACTION_TICKET_ADD, t);
    }

    /**
     * Returns weather the application is still loading data or not.
     *  
     * @return True if loading, false otherwise
     */
    public boolean isLoading() {
        return loading;
    }

    public void updateStatus(String key, String value) {
        status.put(key, value);
        if (!loading) {
            broadcastMessage(Constants.ACTION_DISC_SPACE, status);
        }
    }

    public static SparseArray<String> getContentTypes(Context ctx) {
        SparseArray<String> ret = new SparseArray<>();

        String[] s = ctx.getResources().getStringArray(R.array.pr_content_type0);
        for (int i = 0; i < s.length; i++) {
            ret.append(i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type1);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x10 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type2);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x20 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type3);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x30 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type4);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x40 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type5);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x50 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type6);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x60 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type7);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x70 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type8);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x80 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type9);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x90 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type10);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xa0 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type11);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xb0 + i, s[i]);
        }

        return ret;
    }

    public void addDvrConfigs(List<Profiles> list) {
        dvrConfigs.clear();
        Collections.sort(list, Profiles.ProfilesNameSorter);
        dvrConfigs.addAll(list);
        if (!loading) {
            broadcastMessage(Constants.ACTION_GET_DVR_CONFIG, null);
        }
    }

    public List<Profiles> getDvrConfigs() {
        return dvrConfigs;
    }

    public void addProfiles(List<Profiles> list) {
        profiles.clear();
        Collections.sort(list, Profiles.ProfilesNameSorter);
        profiles.addAll(list);
        if (!loading) {
            broadcastMessage(Constants.ACTION_GET_PROFILES, null);
        }
    }

    public List<Profiles> getProfiles() {
        return profiles;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log(TAG, "onCreate");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("pref_debug_mode", false)) {
            enableLogToFile();
        }

        setupInAppBilling(this);

        // Add the default tag (all channels) to the list
        ChannelTag tag = new ChannelTag();
        tag.id = 0;
        tag.name = getString(R.string.all_channels);
        addChannelTag(tag);

        // Build a CastConfiguration object and initialize VideoCastManager
        CastConfiguration options = new CastConfiguration.Builder(Constants.CAST_APPLICATION_ID)
                .enableAutoReconnect()
                .enableCaptionManagement()
                .enableDebug()
                .enableLockScreen()
                .enableNotification()
                .enableWifiReconnection()
                .setCastControllerImmersive(true)
                .setLaunchOptions(false, Locale.getDefault())
                .setNextPrevVisibilityPolicy(CastConfiguration.NEXT_PREV_VISIBILITY_POLICY_HIDDEN)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_REWIND, false)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_PLAY_PAUSE, true)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_DISCONNECT, true)
                .setForwardStep(10)
                .build();
        VideoCastManager.initialize(this, options);
    }

    /**
     * Checks if the user has purchased the unlocker from the play store. If yes
     * then all extra features shall be accessible. The application is unlocked.
     * 
     * @return True if the application is unlocked otherwise false
     */
    public boolean isUnlocked() {
        log(TAG, "isUnlocked " + isUnlocked);
        return isUnlocked;
    }

    @Override
    public void onTerminate() {

        if (openIabHelper != null) {
            openIabHelper.dispose();
        }
        openIabHelper = null;

        disableLogToFile();
        removeOldLogfiles();
        super.onTerminate();
    }

    public OpenIabHelper getOpenIabHelper() {
        return openIabHelper;
    }

    /**
     * Check if wifi or mobile network is available. If none of these two are
     * available show the status page otherwise continue and show the desired
     * screen.
     * 
     * @return True if the application is connected somehow with the network, otherwise false
     */
    @SuppressLint("InlinedApi")
    public boolean isConnected() {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        final boolean wifiConnected = (wifi != null) && wifi.isConnected();
        final boolean mobileConnected = (mobile != null) && mobile.isConnected();

        // Get the status of the Ethernet connection, some tablets can use an
        // Ethernet cable
        final NetworkInfo eth = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        boolean ethConnected = (eth != null) && eth.isConnected();

        return (wifiConnected || mobileConnected || ethConnected);
    }

    /**
     * Writes the given tag name and the message into the log file
     * 
     * @param tag The tag which identifies who has made the log statement
     * @param msg The message that shall be logged
     */
    public void log(String tag, String msg) {
        Log.d(tag, msg);
        if (logfileBuffer != null) {
            String timestamp = format.format(new Date()) + ": " + tag + ", " + msg + "\n";
            try {
                logfileBuffer.write(timestamp.getBytes());
            } catch (IOException e) {
                Log.d(TAG, "Error writing to logfile buffer. " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * 
     */
    public void saveLog() {
        if (logfileBuffer != null) {
            try {
                logfileBuffer.flush();
            } catch (IOException e) {
                // NOP
            }
        }
    }

    /**
     * 
     */
    public void enableLogToFile() {

        // Get the path where the logs are stored
        logPath = new File(getCacheDir(), "logs");
        if (!logPath.exists()) {
            if (!logPath.mkdirs()) {
                log(TAG, "Could not create log path");
            }
        }

        // Open the log file with the current date. This ensures that the log
        // files are rotated daily 
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        File logFile = new File(logPath, "tvhclient_" + sdf.format(new Date().getTime()) + ".log");

        try {
            // Open the buffer to write data into the log file. Append the data.
            logfileBuffer = new BufferedOutputStream(new FileOutputStream(logFile, true));
            log(TAG, "Log started");

            try {
                PackageInfo info = getPackageManager().getPackageInfo(this.getPackageName(), 0);
                log(TAG, "Application version: " + info.versionName + " (" + info.versionCode + ")");
                log(TAG, "Android version: " + Build.VERSION.RELEASE + "(" + Build.VERSION.SDK_INT + ")");
            } catch (NameNotFoundException e) {
                // NOP
            }

        } catch (IOException e) {
            Log.d(TAG, "Could not create log, " + e.getLocalizedMessage());
        }
    }

    /**
     * Closes the output buffer to stop logging to the defined file
     */
    public void disableLogToFile() {
        if (logfileBuffer != null) {
            log(TAG, "Log stopped");
            try {
                logfileBuffer.flush();
                logfileBuffer.close();
                logfileBuffer = null;
            } catch (IOException e) {
                // NOP
            }
        }
    }

    /**
     * Removes any log files that are older than a week
     */
    private void removeOldLogfiles() {
        File[] files = logPath.listFiles();
        for(File f : files) {
            long diff = new Date().getTime() - f.lastModified();
            if (diff > 7 * 24 * 60 * 60 * 1000) {
                if (!f.delete()) {
                    log(TAG, "Could not remove old logfile");
                }
            }
        }
    }

    /**
     * Calls the actual method to add a notification for the giving recording id 
     * and the required time that the notification shall be shown earlier.
     *  
     * @param id    The id of the recording
     */
    private void addNotification(long id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final long offset = Integer.valueOf(prefs.getString("pref_show_notification_offset", "0"));
        addNotification(id, offset);
    }

    /**
     * Adds the notification of the given recording id and offset time with the
     * notification message. Two notifications will be created, one that the
     * recording has started and another that it has ended regardless of the
     * recording state.
     * 
     * @param id        Id of the recording for which the notification shall be shown
     * @param offset    Time in minutes that the notification shall be shown earlier
     */
    public void addNotification(final long id, final long offset) {

        final Recording rec = getRecording(id);
        if (loading || rec == null) {
            return;
        }

        // The start time when the notification shall be shown
        String msg = getString(R.string.recording_started);
        long time = rec.start.getTime();
        if (time > (new Date()).getTime()) {
            log(TAG, "Recording start time is in the future, adding notification");
            if (offset > 0) {
                // Subtract the offset from the time when the notification shall be shown.
                time -= (offset * 60000);
                msg = getString(R.string.recording_starts_in, offset);
            }

            // Create the intent for the start and stop notifications
            createNotification(rec.id, time, msg);
            createNotification(rec.id * 100, rec.stop.getTime(), getString(R.string.recording_completed));
        } else {
            log(TAG, "Recording start time was in the past, skipping notification");
        }
    }

    /**
     * Creates the required intent for the notification and passed 
     * it on to the alarm manager to the notification can be shown later
     * 
     * @param id    The id of the recording
     * @param time  Time when the notification shall be shown 
     * @param msg   Message that will be displayed
     */
    private void createNotification(long id, long time, String msg) {

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy, HH.mm", Locale.US);
        log(TAG, "Creating notification for recording id " + id + ", at " + sdf.format(time) + " with msg " + msg);

        Intent intent = new Intent(this, NotificationReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.BUNDLE_RECORDING_ID, id);
        bundle.putString(Constants.BUNDLE_NOTIFICATION_MSG, msg);
        intent.putExtras(bundle);

        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), (int) id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, time, pi);
    }

    /**
     * Cancels any pending start and stop notifications with the given id
     * 
     * @param id    The id of the recording
     */
    public void cancelNotification(long id) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel((int) id);
        nm.cancel((int) id * 100);
    }

    /**
     * Adds notifications for all recordings that are scheduled.
     * 
     * @param offset Time in minutes that the notification shall be shown earlier
     */
    public void addNotifications(final long offset) {
        for (Recording rec : getRecordings()) {
            if (rec.error == null && rec.state.equals("scheduled")) {
                addNotification(rec.id, offset);
            }
        }
    }

    /**
     * Cancels all pending notifications related to recordings
     */
    public void cancelNotifications() {
        for (Recording rec : getRecordings()) {
            cancelNotification(rec.id);
        }
    }

    private void setupInAppBilling(final TVHClientApplication app) {

        app.log(TAG, "Initializing OpenIabHelper builder");
        OpenIabHelper.Options.Builder builder = new OpenIabHelper.Options.Builder()
                .setStoreSearchStrategy(OpenIabHelper.Options.SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT)
                .addStoreKey(OpenIabHelper.NAME_GOOGLE, Utils.getPublicKey(this))
                .setVerifyMode(OpenIabHelper.Options.VERIFY_SKIP);

        app.log(TAG, "Starting setup of OpenIabHelper");
        openIabHelper = new OpenIabHelper(this, builder.build());
        openIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                app.log(TAG, "onIabSetupFinished");
                if (!result.isSuccess()) {
                    isBillingSetupDone = false;
                    log(TAG, "Could not setup in-app billing: " + result);
                    return;
                }
                Log.d(TAG, "Setup successful. Querying inventory.");
                isBillingSetupDone = true;
                openIabHelper.queryInventoryAsync(app);
            }
        });
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return true;
    }

    public boolean isBillingSetupDone() {
        return isBillingSetupDone;
    }

    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
        log(TAG, "Query inventory finished.");

        if (result.isFailure()) {
            log(TAG, "Failed to query inventory: " + result);
            return;
        }

        log(TAG, "Query inventory was successful.");

        // Check for items we own
        Purchase unlockerPurchase = inventory.getPurchase(Constants.UNLOCKER);
        if (unlockerPurchase == null) {
            log(TAG, "Unlocker is null");
        } else {
            log(TAG, "Appstore name " + unlockerPurchase.getAppstoreName());
            log(TAG, "Payload " + unlockerPurchase.getDeveloperPayload());
            log(TAG, "JSON " + unlockerPurchase.getOriginalJson());
            log(TAG, "Sku " + unlockerPurchase.getSku());
        }
        isUnlocked = unlockerPurchase != null && verifyDeveloperPayload(unlockerPurchase);
        log(TAG, "User has unlocked the app " + isUnlocked);
    }

    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
        log(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

        if (result.isFailure()) {
            showMessage(getString(R.string.unlocker_purchase_not_successful));
            return;
        }
        if (!verifyDeveloperPayload(purchase)) {
            showMessage(getString(R.string.unlocker_purchase_not_successful));
            return;
        }

        log(TAG, "Purchase successful.");

        if (purchase.getSku().equals(Constants.UNLOCKER)) {
            showMessage(getString(R.string.unlocker_purchase_successful));
            isUnlocked = true;
        }
    }
}
