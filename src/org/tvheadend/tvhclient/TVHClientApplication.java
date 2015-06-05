package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.SparseArray;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

public class TVHClientApplication extends Application implements BillingProcessor.IBillingHandler {

    @SuppressWarnings("unused")
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

    // Indication that data is being loaded
    private volatile boolean loading = false;

    // The default protocol version that is assumed the server supports
    private int protocolVersion = 10;
    private String serverName = "";
    private String serverVersion = "";

    // Holds a list of channels that are not allowed to load because the EPG
    // size did not change after the last loading call.
    private List<Channel> channelBlockingList = new ArrayList<Channel>();

    public void blockChannel(Channel channel) {
        channelBlockingList.add(channel);
    }
    
    public void unblockChannel(Channel channel) {
        channelBlockingList.remove(channel);
    }

    public void unblockAllChannels() {
        channelBlockingList.clear();
    }

    public Boolean isChannelBlocked(Channel channel) {
        return channelBlockingList.contains(channel);
    }

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
            Collections.sort(tags, ChannelTag.ChannelTagNameSorter);
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
            tags.add(tag);
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
            channels.add(channel);
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
            SnackbarManager.show(Snackbar.with(getApplicationContext())
                    .type(SnackbarType.MULTI_LINE)
                    .text(getString(R.string.unlocker_purchase_successful)));
        } else {
            SnackbarManager.show(Snackbar.with(getApplicationContext())
                    .type(SnackbarType.MULTI_LINE)
                    .text(getString(R.string.unlocker_purchase_not_successful)));
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
    public boolean isConnected() {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        final boolean wifiConnected = ((wifi != null) ? wifi.isConnected() : false);
        final boolean mobileConnected = ((mobile != null) ? mobile.isConnected() : false);

        return (wifiConnected || mobileConnected);
    }
}
