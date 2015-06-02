package org.tvheadend.tvhclient.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Recording implements Comparable<Recording> {

    public long id;
    public Date start;
    public Date stop;
    public String title;
    public String summary;
    public String description;
    public Channel channel;
    public String state;
    public String error;
    public long eventId;
    public String autorecId = null;
    public String timerecId = null;
    public long startExtra;
    public long stopExtra;
    public long retention;
    public long priority;
    public long contentType;
    public List<DvrCutpoint> dvrCutPoints = new ArrayList<DvrCutpoint>();

    @Override
    public int compareTo(Recording that) {
        if (this.state() == 1 && that.state() == 1) {
            return this.start.compareTo(that.start);
        } else {
            return that.start.compareTo(this.start);
        }
    }

    public boolean isRecording() {
        return state() == 0;
    }

    public boolean isScheduled() {
        return state() == 1;
    }

    private int state() {
        if ("recording".equals(state)) {
            return 0;
        } else if ("scheduled".equals(state)) {
            return 1;
        }
        return 2;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Recording) {
            return ((Recording) o).id == id;
        }

        return false;
    }
}
