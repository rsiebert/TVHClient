package org.tvheadend.tvhclient.model;

import java.util.Date;

public class TimerRecording implements Comparable<TimerRecording> {

    public String id;
    public String configName;
    public Channel channel;
    public String title;
    public Date start; 
    public Date stop;
    public long retention; 
    public long daysOfWeek;
    public long priority;
    public long enabled;
    public String comment;
    public String name;
    public String owner;
    public String creator;

    @Override
    public int compareTo(TimerRecording that) {
        return (this.id.equals(that.id)) ? 0 : 1;
    }
}
