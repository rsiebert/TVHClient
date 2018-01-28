package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

public class ProgramWithRecordingsAndChannels {

    @Embedded
    private Program program;
    @Relation(parentColumn = "dvr_id", entityColumn = "id", entity = Recording.class)
    private List<Recording> recordings;
    @Relation(parentColumn = "channel_id", entityColumn = "id", entity = Channel.class)
    private List<Channel> channels;

    public Program getProgram() {
        return program;
    }

    public List<Recording> getRecordings() {
        return recordings;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public void setRecordings(List<Recording> recordings) {
        this.recordings = recordings;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }
}
