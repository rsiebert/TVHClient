package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;

import java.util.List;

@Entity(tableName = "channels", primaryKeys = {"id", "connection_id"})
public class Channel {

    @ColumnInfo(name = "id")
    private int id;                      // u32   required   ID of channel.
    @ColumnInfo(name = "number")
    private int number;                  // u32   required   Channel number, 0 means not configured.
    @ColumnInfo(name = "number_minor")
    private int numberMinor;             // u32   optional   Minor channel number (Added in version 13).
    @ColumnInfo(name = "name")
    private String name;                 // str   required   Name of channel.
    @ColumnInfo(name = "icon")
    private String icon;                 // str   optional   URL to an icon representative for the channel
    @ColumnInfo(name = "event_id")
    private int eventId;                        // u32   optional   ID of the current event on this channel.
    @ColumnInfo(name = "next_event_id")
    private int nextEventId;                    // u32   optional   ID of the next event on the channel.

    @ColumnInfo(name = "connection_id")
    private int connectionId;

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
    @ColumnInfo(name = "next_program_id")
    private int nextProgramId;
    @ColumnInfo(name = "next_program_title")
    private String nextProgramTitle;

    @ColumnInfo(name = "display_number")
    private String displayNumber;
    @Ignore
    private List<Integer> tags;

    @Ignore
    private Recording recording;

    public Recording getRecording() {
        return recording;
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumberMinor() {
        return numberMinor;
    }

    public void setNumberMinor(int numberMinor) {
        this.numberMinor = numberMinor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public int getNextProgramId() {
        return nextProgramId;
    }

    public void setNextProgramId(int nextProgramId) {
        this.nextProgramId = nextProgramId;
    }

    public String getNextProgramTitle() {
        return nextProgramTitle;
    }

    public void setNextProgramTitle(String nextProgramTitle) {
        this.nextProgramTitle = nextProgramTitle;
    }

    public List<Integer> getTags() {
        return tags;
    }

    public void setTags(List<Integer> tags) {
        this.tags = tags;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public String getDisplayNumber() {
        return displayNumber;
    }

    public void setDisplayNumber(String displayNumber) {
        this.displayNumber = displayNumber;
    }
}
