package org.tvheadend.tvhclient.data;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.local.NotificationHandler;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.ChannelTag;
import org.tvheadend.tvhclient.data.model.DiscSpace;
import org.tvheadend.tvhclient.data.model.HttpTicket;
import org.tvheadend.tvhclient.data.model.Packet;
import org.tvheadend.tvhclient.data.model.Profiles;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.data.model.SeriesRecording;
import org.tvheadend.tvhclient.data.model.Subscription;
import org.tvheadend.tvhclient.data.model.SystemTime;
import org.tvheadend.tvhclient.data.model.TimerRecording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStorage {
    private final static String TAG = TVHClientApplication.class.getSimpleName();

    private final List<Subscription> subscriptions = Collections.synchronizedList(new ArrayList<Subscription>());
    private final List<Profiles> dvrConfigs = Collections.synchronizedList(new ArrayList<Profiles>());
    private final List<Profiles> profiles = Collections.synchronizedList(new ArrayList<Profiles>());

    private final Map<Integer, Program> programArray = new ConcurrentHashMap<>();
    private final Map<Integer, Recording> recordingArray = new ConcurrentHashMap<>();
    private final Map<Integer, Channel> channelArray = new ConcurrentHashMap<>();
    private final Map<Integer, ChannelTag> tagArray = new ConcurrentHashMap<>();
    private final Map<String, SeriesRecording> seriesRecordingArray = new ConcurrentHashMap<>();
    private final Map<String, TimerRecording> timerRecordingArray = new ConcurrentHashMap<>();

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
     * Informes all registered listeners about the loading status.
     *
     * @param b The loading state
     */
    public void setLoading(boolean b) {
        if (loading != b) {
            app.broadcastMessage(Constants.ACTION_LOADING, b);
        }
        loading = b;

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
        tagArray.clear();
        recordingArray.clear();
        seriesRecordingArray.clear();
        timerRecordingArray.clear();
        channelArray.clear();

        for (Subscription s : subscriptions) {
            s.streams.clear();
        }
        subscriptions.clear();
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

    public Program getProgramFromArray(int id) {
        synchronized (programArray) {
            return programArray.get(id);
        }
    }

    public Map<Integer, Program> getProgramsFromArray() {
        synchronized (programArray) {
            return programArray;
        }
    }

    public void addProgramToArray(Program program) {
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
            app.broadcastMessage("eventDelete", id);
        }
    }

    public void updateProgramInArray(Program program) {
        synchronized (programArray) {
            programArray.put(program.eventId, program);
        }
        if (!loading) {
            app.broadcastMessage("eventUpdate", program);
        }
    }

    public Recording getRecordingFromArray(int id) {
        synchronized (recordingArray) {
            return recordingArray.get(id);
        }
    }

    public Map<Integer, Recording> getRecordingsFromArray() {
        synchronized (recordingArray) {
            return recordingArray;
        }
    }

    public void addRecordingToArray(Recording recording) {
        synchronized (recordingArray) {
            recordingArray.put(recording.id, recording);
        }
        if (!loading) {
            app.broadcastMessage("dvrEntryAdd", recording);
            // Add a notification for scheduled recordings
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
            if (prefs.getBoolean("pref_show_notifications", false) && recording.isScheduled()) {
                NotificationHandler.getInstance().addNotification(recording.id);
            }
        }
    }

    public void removeRecordingFromArray(int id) {
        synchronized (recordingArray) {
            recordingArray.remove(id);
        }
        if (!loading) {
            app.broadcastMessage("dvrEntryDelete", id);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
            if (prefs.getBoolean("pref_show_notifications", false)) {
                NotificationHandler.getInstance().cancelNotification(id);
            }
        }
    }

    public void updateRecordingInArray(Recording recording) {
        synchronized (recordingArray) {
            recordingArray.put(recording.id, recording);
        }
        if (!loading) {
            app.broadcastMessage("dvrEntryUpdate", recording);
        }
    }


    public Channel getChannelFromArray(int id) {
        synchronized (channelArray) {
            return channelArray.get(id);
        }
    }

    public Map<Integer, Channel> getChannelsFromArray() {
        synchronized (channelArray) {
            return channelArray;
        }
    }

    public void addChannelToArray(Channel channel) {
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
            app.broadcastMessage("channelDelete", id);
        }
    }

    public void updateChannelInArray(Channel channel) {
        synchronized (channelArray) {
            channelArray.put(channel.channelId, channel);
        }
        if (!loading) {
            app.broadcastMessage("channelUpdate", channel);
        }
    }

    public ChannelTag getTagFromArray(int id) {
        synchronized (tagArray) {
            return tagArray.get(id);
        }
    }

    public Map<Integer, ChannelTag> getTagsFromArray() {
        synchronized (tagArray) {
            return tagArray;
        }
    }

    public void addTagToArray(ChannelTag tag) {
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

    public void updateTagInArray(ChannelTag tag) {
        synchronized (tagArray) {
            tagArray.put(tag.tagId, tag);
        }
        if (!loading) {
            app.broadcastMessage("tagUpdate", tag);
        }
    }

    public SeriesRecording getSeriesRecordingFromArray(String id) {
        synchronized (seriesRecordingArray) {
            return seriesRecordingArray.get(id);
        }
    }

    public Map<String, SeriesRecording> getSeriesRecordingsFromArray() {
        synchronized (seriesRecordingArray) {
            return seriesRecordingArray;
        }
    }

    public void addSeriesRecordingToArray(SeriesRecording seriesRecording) {
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
            app.broadcastMessage("autorecEntryDelete", id);
        }
    }

    public void updateSeriesRecordingInArray(SeriesRecording seriesRecording) {
        synchronized (seriesRecordingArray) {
            seriesRecordingArray.put(seriesRecording.id, seriesRecording);
        }
        if (!loading) {
            app.broadcastMessage("autorecEntryUpdate", seriesRecording);
        }
    }

    public TimerRecording getTimerRecordingFromArray(String id) {
        synchronized (timerRecordingArray) {
            return timerRecordingArray.get(id);
        }
    }

    public Map<String, TimerRecording> getTimerRecordingsFromArray() {
        synchronized (timerRecordingArray) {
            return timerRecordingArray;
        }
    }

    public void addTimerRecordingToArray(TimerRecording timerRecording) {
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
            app.broadcastMessage("timerecEntryDelete", id);
        }
    }

    public void updateTimerRecordingInArray(TimerRecording timerRecording) {
        synchronized (timerRecordingArray) {
            timerRecordingArray.put(timerRecording.id, timerRecording);
        }
        if (!loading) {
            app.broadcastMessage("timerecEntryUpdate", timerRecording);
        }
    }
}
