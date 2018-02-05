package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
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

    @ColumnInfo(name = "program_id")
    private int programId;
    @ColumnInfo(name = "program_title")
    private String programTitle;
    @ColumnInfo(name = "program_subtitle")
    private String programSubtitle;
    @ColumnInfo(name = "program_start")
    private long programStart;
    @ColumnInfo(name = "program_stop")
    private long programStop;
    @ColumnInfo(name = "program_content_type")
    private int programContentType;
    @ColumnInfo(name = "next_program_title")
    private String nextProgramTitle;
    @ColumnInfo(name = "recording_id")
    private int recordingId;
    @ColumnInfo(name = "recording_title")
    private String recordingTitle;
    @ColumnInfo(name = "recording_state")
    private String recordingState;
    @ColumnInfo(name = "recording_error")
    private String recordingError;

    @Ignore
    private Recording recording;

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

    public int getProgramId() {
        return programId;
    }

    public void setProgramId(int programId) {
        this.programId = programId;
    }

    public String getProgramTitle() {
        return programTitle;
    }

    public void setProgramTitle(String programTitle) {
        this.programTitle = programTitle;
    }

    public String getProgramSubtitle() {
        return programSubtitle;
    }

    public void setProgramSubtitle(String programSubtitle) {
        this.programSubtitle = programSubtitle;
    }

    public long getProgramStart() {
        return programStart;
    }

    public void setProgramStart(long programStart) {
        this.programStart = programStart;
    }

    public long getProgramStop() {
        return programStop;
    }

    public void setProgramStop(long programStop) {
        this.programStop = programStop;
    }

    public int getProgramContentType() {
        return programContentType;
    }

    public void setProgramContentType(int programContentType) {
        this.programContentType = programContentType;
    }

    public String getNextProgramTitle() {
        return nextProgramTitle;
    }

    public void setNextProgramTitle(String nextProgramTitle) {
        this.nextProgramTitle = nextProgramTitle;
    }

    public int getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(int recordingId) {
        this.recordingId = recordingId;
    }

    public String getRecordingTitle() {
        return recordingTitle;
    }

    public void setRecordingTitle(String recordingTitle) {
        this.recordingTitle = recordingTitle;
    }

    public String getRecordingState() {
        return recordingState;
    }

    public void setRecordingState(String recordingState) {
        this.recordingState = recordingState;
    }

    public String getRecordingError() {
        return recordingError;
    }

    public void setRecordingError(String recordingError) {
        this.recordingError = recordingError;
    }

    public Recording getRecording() {
        return new Recording(recordingId, recordingTitle, recordingState, recordingError);
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }
}
