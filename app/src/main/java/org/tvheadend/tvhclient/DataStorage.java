package org.tvheadend.tvhclient;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Channel2;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.ChannelTag2;
import org.tvheadend.tvhclient.model.DiscSpace;
import org.tvheadend.tvhclient.model.HttpTicket;
import org.tvheadend.tvhclient.model.Packet;
import org.tvheadend.tvhclient.model.Profiles;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Program2;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.Recording2;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.model.SeriesRecording2;
import org.tvheadend.tvhclient.model.Subscription;
import org.tvheadend.tvhclient.model.SystemTime;
import org.tvheadend.tvhclient.model.TimerRecording;
import org.tvheadend.tvhclient.model.TimerRecording2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataStorage {
    private final static String TAG = TVHClientApplication.class.getSimpleName();

    private final List<ChannelTag> tags = Collections.synchronizedList(new ArrayList<ChannelTag>());
    private final List<Channel> channels = Collections.synchronizedList(new ArrayList<Channel>());
    private final List<Recording> recordings = Collections.synchronizedList(new ArrayList<Recording>());
    private final List<SeriesRecording> seriesRecordings = Collections.synchronizedList(new ArrayList<SeriesRecording>());
    private final List<TimerRecording> timerRecordings = Collections.synchronizedList(new ArrayList<TimerRecording>());
    private final List<Subscription> subscriptions = Collections.synchronizedList(new ArrayList<Subscription>());
    private final List<Profiles> dvrConfigs = Collections.synchronizedList(new ArrayList<Profiles>());
    private final List<Profiles> profiles = Collections.synchronizedList(new ArrayList<Profiles>());

    private final SparseArray<Program2> programArray = new SparseArray<>();
    private final SparseArray<Recording2> recordingArray = new SparseArray<>();
    private final SparseArray<Channel2> channelArray = new SparseArray<>();
    private final SparseArray<ChannelTag2> tagArray = new SparseArray<>();
    private final Map<String, SeriesRecording2> seriesRecordingArray = new HashMap<>();
    private final Map<String, TimerRecording2> timerRecordingArray = new HashMap<>();

    private final TVHClientApplication app;
    private SystemTime systemTime = new SystemTime();
    private DiscSpace discSpace = new DiscSpace();
    private volatile boolean loading = false;

    // The default protocol version that is assumed the server supports
    private int protocolVersion = 10;
    private String serverName = "";
    private String serverVersion = "";
    private String webRoot = "";

    private static DataStorage mInstance = null;

    public DataStorage() {
        app = TVHClientApplication.getInstance();
    }

    public static synchronized DataStorage getInstance() {
        if (mInstance == null)
            mInstance = new DataStorage();
        return mInstance;
    }

    /**
     * Informs all listeners about the current connection state.
     */
    public void setConnectionState(final String state) {
        app.broadcastMessage(state, null);
    }

    /**
     * Sends the given packet object for video playback to all registered
     * listeners
     *
     * @param p Packet
     */
    public void broadcastPacket(Packet p) {
        app.broadcastMessage(Constants.ACTION_PLAYBACK_PACKET, p);
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
                        if (y.name.equals(app.getString(R.string.all_channels))) {
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
            app.broadcastMessage("tagAdd", tag);
        }
    }

    /**
     * Removes the given channel tag from the list. If loading is not in progress
     * all registered listeners will be informed.
     *
     * @param tag Channel tag
     */
    private void removeChannelTag(ChannelTag tag) {
        synchronized (tags) {
            tags.remove(tag);
        }
        if (!loading) {
            app.broadcastMessage("tagDelete", tag);
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
            app.broadcastMessage("tagUpdate", tag);
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
            app.broadcastMessage("channelAdd", channel);
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
    private void removeChannel(Channel channel) {
        synchronized (channels) {
            channels.remove(channel);
        }
        if (!loading) {
            app.broadcastMessage("channelDelete", channel);
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
            app.broadcastMessage("channelUpdate", ch);
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
            app.broadcastMessage("eventAdd", p);
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
            app.broadcastMessage("eventDelete", p);
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
            app.broadcastMessage("eventUpdate", p);
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
            app.broadcastMessage("dvrEntryAdd", rec);

            // Add a notification for scheduled recordings
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
            if (prefs.getBoolean("pref_show_notifications", false) && rec.isScheduled()) {
                NotificationHandler.getInstance().addNotification(rec.id);
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
                        if (rec.isCompleted()) {
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
                        if (rec.isRecording() || rec.isScheduled()) {
                            recs.add(rec);
                        }
                    }
                }
                break;

            case Constants.RECORDING_TYPE_FAILED:
                synchronized (recordings) {
                    for (Recording rec : recordings) {
                        // Include all failed recordings in the list
                        if (rec.isFailed() || rec.isMissed() || rec.isAborted()) {
                            recs.add(rec);
                        }
                    }
                }
                break;

            case Constants.RECORDING_TYPE_REMOVED:
                synchronized (recordings) {
                    for (Recording rec : recordings) {
                        // Include all removed recordings in the list
                        if (rec.isRemoved()) {
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
            app.broadcastMessage("dvrEntryDelete", rec);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
            if (prefs.getBoolean("pref_show_notifications", false)) {
                NotificationHandler.getInstance().cancelNotification(rec.id);
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
            app.broadcastMessage("dvrEntryUpdate", rec);
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
            app.broadcastMessage("autorecEntryAdd", rec);
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
    private void removeSeriesRecording(SeriesRecording srec) {
        synchronized (seriesRecordings) {
            seriesRecordings.remove(srec);
        }
        if (!loading) {
            app.broadcastMessage("autorecEntryDelete", srec);
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
            app.broadcastMessage("autorecEntryUpdate", srec);
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
            app.broadcastMessage("timerecEntryAdd", rec);
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
            app.broadcastMessage("timerecEntryDelete", rec);
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
            app.broadcastMessage("timerecEntryUpdate", rec);
        }
    }

    /**
     * Informes all registered listeners about the loading status.
     *
     * @param b The loading state
     */
    public void setLoading(boolean b) {
        if (loading != b) {
            app.broadcastMessage(Constants.ACTION_LOADING, b);
        }
        loading = b;

        if (!loading) {
            Log.d(TAG, "setLoading: channels:   " + getChannelsFromArray().size());
            Log.d(TAG, "setLoading: programs:   " + getProgramsFromArray().size());
            Log.d(TAG, "setLoading: recordings: " + getRecordingsFromArray().size());
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (!loading && prefs.getBoolean("pref_show_notifications", false)) {
            long offset = 0;
            try {
                offset = Integer.valueOf(prefs.getString("pref_show_notification_offset", "0"));
            } catch(NumberFormatException ex) {
                // NOP
            }
            NotificationHandler.getInstance().addNotifications(offset);
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
        tag.name = app.getString(R.string.all_channels);
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
            app.broadcastMessage(Constants.ACTION_SUBSCRIPTION_ADD, s);
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
            app.broadcastMessage(Constants.ACTION_SUBSCRIPTION_DELETE, s);
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
            app.broadcastMessage(Constants.ACTION_SUBSCRIPTION_UPDATE, s);
        }
    }

    /**
     * Informs all listeners that the given ticket has been added.
     *
     * @param t Ticket id
     */
    public void addTicket(HttpTicket t) {
        app.broadcastMessage(Constants.ACTION_TICKET_ADD, t);
    }

    /**
     * Returns weather the application is still loading data or not.
     *
     * @return True if loading, false otherwise
     */
    public boolean isLoading() {
        return loading;
    }


    public void addDvrConfigs(List<Profiles> list) {
        dvrConfigs.clear();
        Collections.sort(list, Profiles.ProfilesNameSorter);
        dvrConfigs.addAll(list);
        if (!loading) {
            app.broadcastMessage("getDvrConfigs", null);
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
            app.broadcastMessage("getProfiles", null);
        }
    }

    public List<Profiles> getProfiles() {
        return profiles;
    }


    public void addSystemTime(SystemTime st) {
        systemTime = st;
        if (!loading) {
            app.broadcastMessage(Constants.ACTION_SYSTEM_TIME, st);
        }
    }

    public void addDiscSpace(DiscSpace dataStorage) {
        discSpace = dataStorage;
        if (!loading) {
            app.broadcastMessage(Constants.ACTION_DISC_SPACE, dataStorage);
        }
    }

    public DiscSpace getDiscSpace() {
        return discSpace;
    }

    public SystemTime getSystemTime() {
        return systemTime;
    }

    public void setWebRoot(String webRoot) {
        this.webRoot = webRoot;
    }

    public String getWebRoot() {
        return webRoot;
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


    public void showMessage(String msg) {
        if (!loading) {
            app.broadcastMessage(Constants.ACTION_SHOW_MESSAGE, msg);
        }
    }




    public SparseArray<Program2> getProgramsFromArray() {
        synchronized (programArray) {
            return programArray;
        }
    }

    public void addProgramToArray(Program2 program) {
        synchronized (programArray) {
            programArray.put(program.eventId, program);
        }
        if (!loading) {
            app.broadcastMessage("eventAdd", program);
        }
    }

    public void removeProgramFromArray(int id) {
        synchronized (programArray) {
            programArray.remove(id);
        }
        if (!loading) {
            app.broadcastMessage("eventDelete", null);
        }
    }

    public void updateProgramInArray(Program2 program) {
        synchronized (programArray) {
            programArray.put(program.eventId, program);
        }
        if (!loading) {
            app.broadcastMessage("eventUpdate", program);
        }
    }

    public Recording2 getRecordingFromArray(int id) {
        synchronized (recordingArray) {
            return recordingArray.valueAt(id);
        }
    }

    public SparseArray<Recording2> getRecordingsFromArray() {
        synchronized (recordingArray) {
            return recordingArray;
        }
    }

    public void addRecordingToArray(Recording2 recording) {
        synchronized (recordingArray) {
            recordingArray.put(recording.id, recording);
        }
        if (!loading) {
            app.broadcastMessage("dvrEntryAdd", recording);
        }
    }

    public void removeRecordingFromArray(int id) {
        synchronized (recordingArray) {
            recordingArray.remove(id);
        }
        if (!loading) {
            app.broadcastMessage("dvrEntryDelete", null);
        }
    }

    public void updateRecordingInArray(Recording2 recording) {
        synchronized (recordingArray) {
            recordingArray.put(recording.id, recording);
        }
        if (!loading) {
            app.broadcastMessage("dvrEntryUpdate", recording);
        }
    }


    public Channel2 getChannelFromArray(int id) {
        synchronized (channelArray) {
            return channelArray.valueAt(id);
        }
    }

    public SparseArray<Channel2> getChannelsFromArray() {
        synchronized (channelArray) {
            return channelArray;
        }
    }

    public void addChannelToArray(Channel2 channel) {
        synchronized (channelArray) {
            channelArray.put(channel.channelId, channel);
        }
        if (!loading) {
            app.broadcastMessage("channelAdd", channel);
        }
    }

    public void removeChannelFromArray(int id) {
        synchronized (channelArray) {
            channelArray.remove(id);
        }
        if (!loading) {
            app.broadcastMessage("channelDelete", null);
        }
    }

    public void updateChannelInArray(Channel2 channel) {
        synchronized (channelArray) {
            channelArray.put(channel.channelId, channel);
        }
        if (!loading) {
            app.broadcastMessage("channelUpdate", channel);
        }
    }

    public ChannelTag2 getTagFromArray(int id) {
        synchronized (tagArray) {
            return tagArray.valueAt(id);
        }
    }

    public SparseArray<ChannelTag2> getTagsFromArray() {
        synchronized (tagArray) {
            return tagArray;
        }
    }

    public void addTagToArray(ChannelTag2 tag) {
        synchronized (tagArray) {
            tagArray.put(tag.tagId, tag);
        }
        if (!loading) {
            app.broadcastMessage("tagAdd", tag);
        }
    }

    public void removeTagFromArray(int id) {
        synchronized (tagArray) {
            tagArray.remove(id);
        }
        if (!loading) {
            app.broadcastMessage("tagDelete", null);
        }
    }

    public void updateTagInArray(ChannelTag2 tag) {
        synchronized (tagArray) {
            tagArray.put(tag.tagId, tag);
        }
        if (!loading) {
            app.broadcastMessage("tagUpdate", tag);
        }
    }

    public SeriesRecording2 getSeriesRecordingFromArray(String id) {
        synchronized (seriesRecordingArray) {
            return seriesRecordingArray.get(id);
        }
    }

    public Map<String, SeriesRecording2> getSeriesRecordingsFromArray() {
        synchronized (seriesRecordingArray) {
            return seriesRecordingArray;
        }
    }

    public void addSeriesRecordingToArray(SeriesRecording2 seriesRecording) {
        synchronized (seriesRecordingArray) {
            seriesRecordingArray.put(seriesRecording.id, seriesRecording);
        }
        if (!loading) {
            app.broadcastMessage("autorecEntryAdd", seriesRecording);
        }
    }

    public void removeSeriesRecordingFromArray(String id) {
        synchronized (seriesRecordingArray) {
            seriesRecordingArray.remove(id);
        }
        if (!loading) {
            app.broadcastMessage("autorecEntryDelete", null);
        }
    }

    public void updateSeriesRecordingInArray(SeriesRecording2 seriesRecording) {
        synchronized (seriesRecordingArray) {
            seriesRecordingArray.put(seriesRecording.id, seriesRecording);
        }
        if (!loading) {
            app.broadcastMessage("autorecEntryUpdate", seriesRecording);
        }
    }

    public TimerRecording2 getTimerRecordingFromArray(String id) {
        synchronized (timerRecordingArray) {
            return timerRecordingArray.get(id);
        }
    }

    public Map<String, TimerRecording2> getTimerRecordingsFromArray() {
        synchronized (timerRecordingArray) {
            return timerRecordingArray;
        }
    }

    public void addTimerRecordingToArray(TimerRecording2 timerRecording) {
        synchronized (timerRecordingArray) {
            timerRecordingArray.put(timerRecording.id, timerRecording);
        }
        if (!loading) {
            app.broadcastMessage("timerecEntryAdd", timerRecording);
        }
    }

    public void removeTimerRecordingFromArray(String id) {
        synchronized (timerRecordingArray) {
            timerRecordingArray.remove(id);
        }
        if (!loading) {
            app.broadcastMessage("timerecEntryDelete", null);
        }
    }

    public void updateTimerRecordingInArray(TimerRecording2 timerRecording) {
        synchronized (timerRecordingArray) {
            timerRecordingArray.put(timerRecording.id, timerRecording);
        }
        if (!loading) {
            app.broadcastMessage("timerecEntryUpdate", timerRecording);
        }
    }
}
