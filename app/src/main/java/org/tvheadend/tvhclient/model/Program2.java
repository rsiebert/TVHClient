package org.tvheadend.tvhclient.model;

public class Program2 {

    public int eventId;             // u32   required   Event ID
    public int channelId;           // u32   required   The channel this event is related to.
    public long start;              // u64   required   Start time of event, UNIX time.
    public long stop;               // u64   required   Ending time of event, UNIX time.
    public String title;            // str   optional   Title of event.
    public String subtitle;         // str   optional   Subitle of event.
    public String summary;          // str   optional   Short description of the event (Added in version 6).
    public String description;      // str   optional   Long description of the event.
    public int serieslinkId;        // u32   optional   Series Link ID (Added in version 6).
    public int episodeId;           // u32   optional   Episode ID (Added in version 6).
    public int seasonId;            // u32   optional   Season ID (Added in version 6).
    public int brandId;             // u32   optional   Brand ID (Added in version 6).
    public int contentType;         // u32   optional   DVB content code (Added in version 4, Modified in version 6*).
    public int ageRating;           // u32   optional   Minimum age rating (Added in version 6).
    public int starRating;          // u32   optional   Star rating (1-5) (Added in version 6).
    public long firstAired;         // s64   optional   Original broadcast time, UNIX time (Added in version 6).
    public int seasonNumber;        // u32   optional   Season number (Added in version 6).
    public int seasonCount;         // u32   optional   Show season count (Added in version 6).
    public int episodeNumber;       // u32   optional   Episode number (Added in version 6).
    public int episodeCount;        // u32   optional   Season episode count (Added in version 6).
    public int partNumber;          // u32   optional   Multi-part episode part number (Added in version 6).
    public int partCount;           // u32   optional   Multi-part episode part count (Added in version 6).
    public String episodeOnscreen;  // str   optional   Textual representation of episode number (Added in version 6).
    public String image;            // str   optional   URL to a still capture from the episode (Added in version 6).
    public int dvrId;               // u32   optional   ID of a recording (Added in version 5).
    public int nextEventId;         // u32   optional   ID of next event on the same channel.
}
