package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.List;

@Entity(tableName = "recordings", primaryKeys = {"id", "connection_id"})
public class Recording {

    @Ignore
    private long time = Calendar.getInstance().getTimeInMillis();
    @Ignore
    private long offset = 30 * 60 * 1000;

    private int id = 0;                     // u32   required   ID of dvrEntry.
    @ColumnInfo(name = "channel_id")
    private int channelId = 0;              // u32   optional   Channel of dvrEntry.
    private long start = time;              // s64   required   Time of when this entry was scheduled to start recording.
    private long stop = time + offset;      // s64   required   Time of when this entry was scheduled to stop recording.
    @ColumnInfo(name = "start_extra")
    private long startExtra = 0;            // s64   required   Extra start time (pre-time) in minutes (Added in version 13).
    @ColumnInfo(name = "stop_extra")
    private long stopExtra = 0;             // s64   required   Extra stop time (post-time) in minutes (Added in version 13).
    private long retention = 0;             // s64   required   DVR Entry retention time in days (Added in version 13).
    private int priority = 2;               // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set) (Added in version 13).
    @ColumnInfo(name = "event_id")
    private int eventId = 0;                // u32   optional   Associated EPG Event ID (Added in version 13).
    @ColumnInfo(name = "autorec_id")
    private String autorecId = "";          // str   optional   Associated Autorec UUID (Added in version 13).
    @ColumnInfo(name = "timerec_id")
    private String timerecId = "";          // str   optional   Associated Timerec UUID (Added in version 18).
    @ColumnInfo(name = "content_type")
    private int contentType;                // u32   optional   Content Type (like in the DVB standard) (Added in version 13).
    private String title;                   // str   optional   Title of recording
    private String subtitle;                // str   optional   Subtitle of recording (Added in version 20).
    private String summary;                 // str   optional   Short description of the recording (Added in version 6).
    private String description;             // str   optional   Long description of the recording.
    private String state;                   // str   required   Recording state
    private String error;                   // str   optional   Plain english error description (e.g. "Aborted by user").
    private String owner;                   // str   optional   Name of the entry owner (Added in version 18).
    private String creator;                 // str   optional   Name of the entry creator (Added in version 18).
    @ColumnInfo(name = "subscription_error")
    private String subscriptionError;       // str   optional   Subscription error string (Added in version 20).
    @ColumnInfo(name = "stream_errors")
    private String streamErrors;            // str   optional   Number of recording errors (Added in version 20).
    @ColumnInfo(name = "data_errors")
    private String dataErrors;              // str   optional   Number of stream data errors (Added in version 20).
    private String path;                    // str   optional   Recording path for playback.
    @ColumnInfo(name = "data_size")
    private long dataSize;                  // s64   optional   Actual file size of the last recordings (Added in version 21).
    private int enabled = 1;                // u32   optional   Enabled flag (Added in version 23).
    private String episode;                 // str   optional   Episode (Added in version 18).
    private String comment;                 // str   optional   Comment (Added in version 18).
    @Ignore
    private List<String> files;             // msg   optional   All recorded files for playback (Added in version 21).

    @ColumnInfo(name = "connection_id")
    private int connectionId;

    @ColumnInfo(name = "channel_name")
    private String channelName;
    @ColumnInfo(name = "channel_icon")
    private String channelIcon;

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelIcon() {
        return channelIcon;
    }

    public void setChannelIcon(String channelIcon) {
        this.channelIcon = channelIcon;
    }

    public boolean isCompleted() {
        return error == null && TextUtils.equals(state, "completed");
    }

    public boolean isRecording() {
        return error == null && (TextUtils.equals(state, "recording") && !TextUtils.equals(state, "scheduled"));
    }

    public boolean isScheduled() {
        return error == null && (TextUtils.equals(state, "recording") || TextUtils.equals(state, "scheduled"));
    }

    public boolean isFailed() {
        return TextUtils.equals(state, "invalid");
    }

    public boolean isMissed() {
        return TextUtils.equals(state, "missed");
    }

    public boolean isAborted() {
        return TextUtils.equals(error, "Aborted by user") && TextUtils.equals(state, "completed");
    }

    public boolean isRemoved() {
        return TextUtils.equals(error, "File missing") && TextUtils.equals(state, "completed");
    }

    public int getDuration() {
        return (int) ((stop - start) / 1000 / 60);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getStop() {
        return stop;
    }

    public void setStop(long stop) {
        this.stop = stop;
    }

    public long getStartExtra() {
        return startExtra;
    }

    public void setStartExtra(long startExtra) {
        this.startExtra = startExtra;
    }

    public long getStopExtra() {
        return stopExtra;
    }

    public void setStopExtra(long stopExtra) {
        this.stopExtra = stopExtra;
    }

    public long getRetention() {
        return retention;
    }

    public void setRetention(long retention) {
        this.retention = retention;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getAutorecId() {
        return autorecId;
    }

    public void setAutorecId(String autorecId) {
        this.autorecId = autorecId;
    }

    public String getTimerecId() {
        return timerecId;
    }

    public void setTimerecId(String timerecId) {
        this.timerecId = timerecId;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getSubscriptionError() {
        return subscriptionError;
    }

    public void setSubscriptionError(String subscriptionError) {
        this.subscriptionError = subscriptionError;
    }

    public String getStreamErrors() {
        return streamErrors;
    }

    public void setStreamErrors(String streamErrors) {
        this.streamErrors = streamErrors;
    }

    public String getDataErrors() {
        return dataErrors;
    }

    public void setDataErrors(String dataErrors) {
        this.dataErrors = dataErrors;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public String getEpisode() {
        return episode;
    }

    public void setEpisode(String episode) {
        this.episode = episode;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }
}
