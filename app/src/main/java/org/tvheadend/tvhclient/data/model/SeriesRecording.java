package org.tvheadend.tvhclient.data.model;

public class SeriesRecording {
    public String id;           // str   required   ID (string!) of dvrAutorecEntry.
    public int enabled;         // u32   required   If autorec entry is enabled (activated).
    public String name;         // str   required   Name of the autorec entry (Added in version 18).
    public int minDuration;     // u32   required   Minimal duration in seconds (0 = Any).
    public int maxDuration;     // u32   required   Maximal duration in seconds (0 = Any).
    public int retention;       // u32   required   Retention time (in days).
    public int daysOfWeek;      // u32   required   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
    public int priority;        // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
    public int approxTime;      // u32   required   Minutes from midnight (up to 24*60).
    public long start;          // s32   required   Exact start time (minutes from midnight) (Added in version 18).
    public long startWindow;    // s32   required   Exact stop time (minutes from midnight) (Added in version 18).
    public long startExtra;     // s64   required   Extra start minutes (pre-time).
    public long stopExtra;      // s64   required   Extra stop minutes (post-time).
    public String title;        // str   optional   Title.
    public int fulltext;        // u32   optional   Fulltext flag (Added in version 20).
    public String directory;    // str   optional   Forced directory name (Added in version 19).
    public int channel;         // u32   optional   Channel ID.
    public String owner;        // str   optional   Owner of this autorec entry (Added in version 18).
    public String creator;      // str   optional   Creator of this autorec entry (Added in version 18).
    public int dupDetect;       // u32   optional   Duplicate detection (see addAutorecEntry) (Added in version 20).

    public int getDuration() {
        return (int) ((startWindow - start) / 60 / 1000);
    }
}
