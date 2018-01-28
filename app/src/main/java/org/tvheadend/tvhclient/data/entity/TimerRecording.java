package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "timer_recordings")
public class TimerRecording {

    @PrimaryKey
    @NonNull
    private String id = "";           // str   required   ID (string!) of dvrTimerecEntry.
    private String title;        // str   required   Title for the recordings.
    private String directory;    // str   optional   Forced directory name (Added in version 19).
    private int enabled;         // u32   required   Title for the recordings.
    private String name;         // str   required   Name for this timerec entry.
    @ColumnInfo(name = "config_name")
    private String configName;   // str   required   DVR Configuration Name / UUID.
    @ColumnInfo(name = "channel_id")
    private int channelId;         // u32   required   Channel ID.
    @ColumnInfo(name = "days_of_week")
    private int daysOfWeek;      // u32   optional   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
    private int priority;        // u32   optional   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
    private long start;          // u32   required   Minutes from midnight (up to 24*60) for the start of the time window (including)
    private long stop;           // u32   required   Minutes from modnight (up to 24*60) for the end of the time window (including, cross-noon allowed)
    private int retention;       // u32   optional   Retention in days.
    private String owner;        // str   optional   Owner of this timerec entry.
    private String creator;      // str   optional   Creator of this timerec entry.

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

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(int daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority > 5) {
            priority = 5;
        }
        this.priority = priority;
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

    public int getRetention() {
        return retention;
    }

    public void setRetention(int retention) {
        this.retention = retention;
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

    public int getDuration() {
        return (int) ((this.stop - this.start) / 60 / 1000);
    }
}
