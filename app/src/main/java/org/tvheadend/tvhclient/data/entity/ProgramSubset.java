package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;

@Entity(tableName = "programs", primaryKeys = {"id", "connection_id"})
public class ProgramSubset {

    @ColumnInfo(name = "id")
    private int eventId;                    // u32   required   Event ID
    @ColumnInfo(name = "channel_id")
    private int channelId;                  // u32   required   The channel this event is related to.
    private long start;                     // u64   required   Start time of event, UNIX time.
    private long stop;                      // u64   required   Ending time of event, UNIX time.
    private String title;                   // str   optional   Title of event.
    private String subtitle;                // str   optional   Subtitle of event.
    @ColumnInfo(name = "content_type")
    private int contentType;                // u32   optional   DVB content code (Added in version 4, Modified in version 6*).

    @ColumnInfo(name = "connection_id")
    private int connectionId;
    @ColumnInfo(name = "channel_name")
    private String channelName;
    @ColumnInfo(name = "channel_icon")
    private String channelIcon;

    @Ignore
    private Recording recording;

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
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

    public Recording getRecording() {
        return this.recording;
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }
}
