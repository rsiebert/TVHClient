package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

public class ChannelWithPrograms {

    @Embedded
    private Channel channel;
    @Relation(parentColumn = "event_id", entityColumn = "id", entity = Program.class)
    private List<Program> program;
    @Relation(parentColumn = "next_event_id", entityColumn = "id", entity = Program.class)
    private List<Program> nextProgram;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public List<Program> getProgram() {
        return program;
    }

    public void setProgram(List<Program> program) {
        this.program = program;
    }

    public List<Program> getNextProgram() {
        return nextProgram;
    }

    public void setNextProgram(List<Program> nextProgram) {
        this.nextProgram = nextProgram;
    }
}
