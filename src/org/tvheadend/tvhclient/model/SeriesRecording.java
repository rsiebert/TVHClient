package org.tvheadend.tvhclient.model;


public class SeriesRecording implements Comparable<SeriesRecording> {

    public String id;
    public long maxDuration;
    public boolean enabled;
    public long minDuration;
    public long retention;
    public long daysOfWeek;
    public long approxTime;
    public long priority;
    public long startExtra;
    public long stopExtra;
    public String title;
    public String name;
    public Channel channel;

    public long start;
    public long startWindow;

    // Required only when a new timer recording is added
    public String configName;

    @Override
    public int compareTo(SeriesRecording that) {
        return (this.id.equals(that.id)) ? 0 : 1;
    }
}
