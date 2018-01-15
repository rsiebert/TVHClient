package org.tvheadend.tvhclient.data.model;

public class TimerRecording {
    public String id;           // str   required   ID (string!) of dvrTimerecEntry.
    public String title;        // str   required   Title for the recordings.
    public String directory;    // str   optional   Forced directory name (Added in version 19).
    public int enabled;         // u32   required   Title for the recordings.
    public String name;         // str   required   Name for this timerec entry.
    public String configName;   // str   required   DVR Configuration Name / UUID.
    public int channel;         // u32   required   Channel ID.
    public int daysOfWeek;      // u32   optional   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
    public int priority;        // u32   optional   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
    public long start;          // u32   required   Minutes from midnight (up to 24*60) for the start of the time window (including)
    public long stop;           // u32   required   Minutes from modnight (up to 24*60) for the end of the time window (including, cross-noon allowed)
    public int retention;       // u32   optional   Retention in days.
    public String owner;        // str   optional   Owner of this timerec entry.
    public String creator;      // str   optional   Creator of this timerec entry.

    public int getDuration() {
        return (int) ((stop - start) / 60 / 1000);
    }
}
