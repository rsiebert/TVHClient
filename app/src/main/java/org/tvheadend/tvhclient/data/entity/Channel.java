package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.RoomWarnings;


@SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
@Entity(tableName = "channels")
public class Channel {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private int channelId;             // u32   required   ID of channel.
    @ColumnInfo(name = "channel_number")
    private int channelNumber;         // u32   required   Channel number, 0 means unconfigured.
    @ColumnInfo(name = "channel_number_minor")
    private int channelNumberMinor;    // u32   optional   Minor channel number (Added in version 13).
    @ColumnInfo(name = "channel_name")
    private String channelName;        // str   required   Name of channel.
    @ColumnInfo(name = "channel_icon")
    private String channelIcon;        // str   optional   URL to an icon representative for the channel
    @ColumnInfo(name = "event_id")
    private int eventId;               // u32   optional   ID of the current event on this channel.
    @ColumnInfo(name = "next_event_id")
    private int nextEventId;           // u32   optional   ID of the next event on the channel.

    @Embedded(prefix = "program_")
    private Program program;
    @Embedded(prefix = "next_program_")
    private Program nextProgram;
    @Embedded(prefix = "recording_")
    private Recording recording;

    public Recording getRecording() {
        return recording;
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Program getNextProgram() {
        return nextProgram;
    }

    public void setNextProgram(Program nextProgram) {
        this.nextProgram = nextProgram;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    public int getChannelNumberMinor() {
        return channelNumberMinor;
    }

    public void setChannelNumberMinor(int channelNumberMinor) {
        this.channelNumberMinor = channelNumberMinor;
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

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getNextEventId() {
        return nextEventId;
    }

    public void setNextEventId(int nextEventId) {
        this.nextEventId = nextEventId;
    }
}
