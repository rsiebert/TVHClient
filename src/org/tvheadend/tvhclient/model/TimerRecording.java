package org.tvheadend.tvhclient.model;


public class TimerRecording implements Comparable<TimerRecording> {

    public String id;
    public boolean enabled;
    public long retention;
    public long daysOfWeek;
    public long priority;
    public long start;  // value is in seconds
    public long stop;   // value is in seconds
    public String title;
    public String name;
    public String directory;
    public String owner;
    public String creator;
    public Channel channel;

    // Required only when a new timer recording is added
    public String configName;

    @Override
    public int compareTo(TimerRecording that) {
        return (this.id.equals(that.id)) ? 0 : 1;
    }
}