package org.tvheadend.tvhclient.model;


public class SeriesRecording implements Comparable<SeriesRecording> {

    public String id;
    public boolean enabled;
    public long maxDuration;
    public long minDuration;
    public long retention;
    public long daysOfWeek;
    public long approxTime;
    public long start;
    public long startWindow;
    public long priority;
    public long startExtra;
    public long stopExtra;
    public String title;
    public String name;
    public String directory;
    public String owner;
    public String creator;
    public Channel channel;

    // Required only when a new timer recording is added
    public String configName;

    @Override
    public int compareTo(SeriesRecording that) {
        return (this.id.equals(that.id)) ? 0 : 1;
    }
}
