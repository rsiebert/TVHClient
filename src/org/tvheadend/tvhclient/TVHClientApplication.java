package org.tvheadend.tvhclient;

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
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.util.SparseArray;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

public class TVHClientApplication extends Application implements BillingProcessor.IBillingHandler {

    private final static String TAG = TVHClientApplication.class.getSimpleName();

    private final List<HTSListener> listeners = new ArrayList<HTSListener>();
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
    private BillingProcessor bp;

    // File name and path for the internal logging functionality
    private File logPath = null;
    private File logFile = null;
    private BufferedOutputStream logfileBuffer = null;
    // The prefix with the date in each log entry
    private final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    // Indication that data is being loaded
    private volatile boolean loading = false;

    // The default protocol version that is assumed the server supports
    private int protocolVersion = 10;
    private String serverName = "";
    private String serverVersion = "";

    /**
     * Adds a single listener to the list.
     * 
     * @param listener
     */
    public void addListener(HTSListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a single listener from the list.
     * 
     * @param listener
     */
    public void removeListener(HTSListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Removes all registered listeners from the list. This can be used prior
     * stopping the service before the application is closed.
     */
    public void removeListeners() {
        listeners.clear();
    }

    /**
     * Sends the given action and possible object with the data to all
     * registered listeners.
     * 
     * @param action
     * @param obj
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
     * @param version
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
     * @return
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
     * @param p
     */
    public void broadcastPacket(Packet p) {
        broadcastMessage(Constants.ACTION_PLAYBACK_PACKET, p);
    }

    /**
     * Returns the list of available channel tags
     * 
     * @return
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
     * @param tag
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
     * @param tag
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
     * @param tag
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
     * @param id
     * @return
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
     * @param tag
     */
    public void updateChannelTag(ChannelTag tag) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_TAG_UPDATE, tag);
        }
    }

    /**
     * Adds the given channel to the list of available channels.
     * 
     * @param channel
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
     * @return
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
     * @param channel
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
     * @param id
     * @return
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
     * @param id
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
     * @param ch
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
     * @param p
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
     * @param p
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
     * @param p
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
     * @param rec
     */
    public void addRecording(Recording rec) {
        synchronized (recordings) {
            recordings.add(rec);
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
     * @return
     */
    public List<Recording> getRecordings() {
        synchronized (recordings) {
            return recordings;
        }
    }

    /**
     * Returns a single recording that matches the given id.
     * 
     * @param id
     * @return
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
     * @param type
     * @return
     */
    public List<Recording> getRecordingsByType(int type) {
        List<Recording> recs = new ArrayList<Recording>();

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
     * @param rec
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
     * @param id
     */
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
     * @param rec
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
     * @param srec
     */
    public void addSeriesRecording(SeriesRecording srec) {
        synchronized (seriesRecordings) {
            seriesRecordings.add(srec);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_SERIES_DVR_ADD, srec);
        }
    }

    /**
     * Adds the given series recording to the list of available series
     * recordings
     * 
     * @return
     */
    public List<SeriesRecording> getSeriesRecordings() {
        synchronized (seriesRecordings) {
            return seriesRecordings;
        }
    }

    /**
     * Returns a single series recording that matches the given id.
     * 
     * @param id
     * @return
     */
    public SeriesRecording getSeriesRecording(String id) {
        synchronized (seriesRecordings) {
            for (SeriesRecording srec : getSeriesRecordings()) {
                if (srec.id == id) {
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
     * @param srec
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
     * @param id
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
     * @param srec
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
     * @param rec
     */
    public void addTimerRecording(TimerRecording rec) {
        synchronized (timerRecordings) {
            timerRecordings.add(rec);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_TIMER_DVR_ADD, rec);
        }
    }

    /**
     * Adds the given timer recording to the list of available timer
     * recordings
     * 
     * @return
     */
    public List<TimerRecording> getTimerRecordings() {
        return timerRecordings;
    }

    /**
     * Returns a single timer recording that matches the given id.
     * 
     * @param id
     * @return
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
     * @param rec
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
     * @param id
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
     * @param srec
     */
    public void updateTimerRecording(TimerRecording rec) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_TIMER_DVR_UPDATE, rec);
        }
    }

    /**
     * Informes all registered listeners about the loading status.
     * 
     * @param b
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

        ChannelTag tag = new ChannelTag();
        tag.id = 0;
        tag.name = getString(R.string.all_channels);
        tags.add(tag);
    }

    /**
     * Adds the given subscription to the list of available subscriptions. If
     * loading has finished any listener will be informed that a subscription
     * has been added.
     * 
     * @param s
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
     * @return
     */
    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    /**
     * Removes the given subscription from the list of available subscriptions.
     * If loading has finished any listener will be informed that a subscription
     * has been removed.
     * 
     * @param s
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
     * @param id
     * @return
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
     * @param id
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
     * @param t
     */
    public void addTicket(HttpTicket t) {
        broadcastMessage(Constants.ACTION_TICKET_ADD, t);
    }

    /**
     * Returns weather the application is still loading data or not.
     *  
     * @return
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
        SparseArray<String> ret = new SparseArray<String>();

        String[] s = ctx.getResources().getStringArray(R.array.pr_content_type0);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x00 + i, s[i]);
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
        bp = new BillingProcessor(this, Utils.getPublicKey(this), this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("pref_debug_mode", false)) {
            enableLogToFile();
        }
    }

    /**
     * Checks if the user has purchased the unlocker from the play store. If yes
     * then all extra features shall be accessible. The application is unlocked.
     * 
     * @return True if the application is unlocked otherwise false
     */
    public boolean isUnlocked() {
        return (bp.isInitialized() && bp.isPurchased(Constants.UNLOCKER));
    }

    @Override
    public void onTerminate() {
        if (bp != null) {
            bp.release();
        }

        disableLogToFile();
        removeOldLogfiles();
        super.onTerminate();
    }

    /**
     * Returns the billing processor object that can be used by other classes to
     * access billing related features
     * 
     * @return
     */
    public BillingProcessor getBillingProcessor() {
        return bp;
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        // NOP
    }

    @Override
    public void onBillingInitialized() {
        // NOP
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        if (bp.isValid(details)) {
            Snackbar.make(null, getString(R.string.unlocker_purchase_successful), 
                    Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(null, getString(R.string.unlocker_purchase_not_successful), 
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPurchaseHistoryRestored() {
        // NOP
    }

    /**
     * Check if wifi or mobile network is available. If none of these two are
     * available show the status page otherwise continue and show the desired
     * screen.
     * 
     * @return
     */
    @SuppressLint("InlinedApi")
    public boolean isConnected() {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        final boolean wifiConnected = ((wifi != null) ? wifi.isConnected() : false);
        final boolean mobileConnected = ((mobile != null) ? mobile.isConnected() : false);

        // Get the status of the Ethernet connection, some tablets can use an
        // Ethernet cable
        boolean ethConnected = false;
        if (Build.VERSION.SDK_INT >= 13) {
            final NetworkInfo eth = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
            ethConnected = ((eth != null) ? eth.isConnected() : false);
        }

        return (wifiConnected || mobileConnected || ethConnected);
    }

    /**
     * Writes the given tag name and the message into the log file
     * 
     * @param tag
     * @param msg
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
     * Combines the given message and exception message into one string and
     * forwards it to the other log method which writes the given tag name and
     * message into the log file.
     * 
     * @param tag
     * @param msg
     * @param ex
     */
    public void log(String tag, String msg, Exception ex) {
        log(tag, msg + ", Exception: " + ex.getLocalizedMessage());
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
            logPath.mkdirs();
        }

        // Open the log file with the current date. This ensures that the log
        // files are rotated daily 
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        logFile = new File(logPath, "tvhclient_" + sdf.format(new Date().getTime()) + ".log");

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
        for (int i = 0; i < files.length; i++) {
            long diff = new Date().getTime() - files[i].lastModified();
            if (diff > 7 * 24 * 60 * 60 * 1000) {
                files[i].delete();
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
     * @param rec       The recording for which the notification shall be shown
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
        if (offset > 0) {
            // Subtract the offset from the time when the notification shall be shown. 
            time -= (offset * 60000);
            msg = getString(R.string.recording_starts_in, offset);
        }

        // Create the intent for the start and stop notifications
        createNotification(rec.id, time, msg);
        createNotification(rec.id * 100, rec.stop.getTime(), getString(R.string.recording_completed));
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
}
