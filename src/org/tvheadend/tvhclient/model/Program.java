package org.tvheadend.tvhclient.model;

import java.util.Date;

public class Program extends Model implements Comparable<Program> {

    public long nextId;
    public int contentType;
    public Date start;
    public Date stop;
    public String title;
    public String description;
    public String summary;
    public SeriesInfo seriesInfo;
    public int starRating;
    public Channel channel;
    public Recording recording;

    public int compareTo(Program that) {
        return this.start.compareTo(that.start);
    }

    public boolean isRecording() {
        return recording != null && "recording".equals(recording.state);
    }

    public boolean isScheduled() {
        return recording != null && "scheduled".equals(recording.state);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Program) {
            return ((Program) o).id == id;
        }

        return false;
    }
}
