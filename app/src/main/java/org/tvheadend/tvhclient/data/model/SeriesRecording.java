package org.tvheadend.tvhclient.data.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "series_recordings")
public class SeriesRecording {

    @PrimaryKey
    public String id;           // str   required   ID (string!) of dvrAutorecEntry.
    public int enabled;         // u32   required   If autorec entry is enabled (activated).
    public String name;         // str   required   Name of the autorec entry (Added in version 18).
    @ColumnInfo(name = "min_duration")
    public int minDuration;     // u32   required   Minimal duration in seconds (0 = Any).
    @ColumnInfo(name = "max_duration")
    public int maxDuration;     // u32   required   Maximal duration in seconds (0 = Any).
    public int retention;       // u32   required   Retention time (in days).
    @ColumnInfo(name = "days_of_week")
    public int daysOfWeek;      // u32   required   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
    public int priority;        // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
    @ColumnInfo(name = "approx_time")
    public int approxTime;      // u32   required   Minutes from midnight (up to 24*60).
    public long start;          // s32   required   Exact start time (minutes from midnight) (Added in version 18).
    @ColumnInfo(name = "start_window")
    public long startWindow;    // s32   required   Exact stop time (minutes from midnight) (Added in version 18).
    @ColumnInfo(name = "start_extra")
    public long startExtra;     // s64   required   Extra start minutes (pre-time).
    @ColumnInfo(name = "stop_extra")
    public long stopExtra;      // s64   required   Extra stop minutes (post-time).
    public String title;        // str   optional   Title.
    public int fulltext;        // u32   optional   Fulltext flag (Added in version 20).
    public String directory;    // str   optional   Forced directory name (Added in version 19).
    public int channel;         // u32   optional   Channel ID.
    public String owner;        // str   optional   Owner of this autorec entry (Added in version 18).
    public String creator;      // str   optional   Creator of this autorec entry (Added in version 18).
    @ColumnInfo(name = "dup_detect")
    public int dupDetect;       // u32   optional   Duplicate detection (see addAutorecEntry) (Added in version 20).

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public int getMinDuration() {
        return minDuration;
    }

    public void setMinDuration(int minDuration) {
        this.minDuration = minDuration;
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    public int getRetention() {
        return retention;
    }

    public void setRetention(int retention) {
        this.retention = retention;
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
        this.priority = priority;
    }

    public int getApproxTime() {
        return approxTime;
    }

    public void setApproxTime(int approxTime) {
        this.approxTime = approxTime;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getStartWindow() {
        return startWindow;
    }

    public void setStartWindow(long startWindow) {
        this.startWindow = startWindow;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getFulltext() {
        return fulltext;
    }

    public void setFulltext(int fulltext) {
        this.fulltext = fulltext;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
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

    public int getDupDetect() {
        return dupDetect;
    }

    public void setDupDetect(int dupDetect) {
        this.dupDetect = dupDetect;
    }

    public int getDuration() {
        return (int) ((this.startWindow - this.start) / 60 / 1000);
    }
}
