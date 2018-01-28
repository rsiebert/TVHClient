package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "programs")
public class Program {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private int eventId;             // u32   required   Event ID
    @ColumnInfo(name = "channel_id")
    private int channelId;           // u32   required   The channel this event is related to.
    private long start;              // u64   required   Start time of event, UNIX time.
    private long stop;               // u64   required   Ending time of event, UNIX time.
    private String title;            // str   optional   Title of event.
    private String subtitle;         // str   optional   Subitle of event.
    private String summary;          // str   optional   Short description of the event (Added in version 6).
    private String description;      // str   optional   Long description of the event.
    @ColumnInfo(name = "series_link_id")
    private int serieslinkId;        // u32   optional   Series Link ID (Added in version 6).
    @ColumnInfo(name = "episode_id")
    private int episodeId;           // u32   optional   Episode ID (Added in version 6).
    @ColumnInfo(name = "season_id")
    private int seasonId;            // u32   optional   Season ID (Added in version 6).
    @ColumnInfo(name = "brand_id")
    private int brandId;             // u32   optional   Brand ID (Added in version 6).
    @ColumnInfo(name = "content_type")
    private int contentType;         // u32   optional   DVB content code (Added in version 4, Modified in version 6*).
    @ColumnInfo(name = "age_rating")
    private int ageRating;           // u32   optional   Minimum age rating (Added in version 6).
    @ColumnInfo(name = "star_rating")
    private int starRating;          // u32   optional   Star rating (1-5) (Added in version 6).
    @ColumnInfo(name = "first_aired")
    private long firstAired;         // s64   optional   Original broadcast time, UNIX time (Added in version 6).
    @ColumnInfo(name = "season_number")
    private int seasonNumber;        // u32   optional   Season number (Added in version 6).
    @ColumnInfo(name = "season_count")
    private int seasonCount;         // u32   optional   Show season count (Added in version 6).
    @ColumnInfo(name = "episode_number")
    private int episodeNumber;       // u32   optional   Episode number (Added in version 6).
    @ColumnInfo(name = "episode_count")
    private int episodeCount;        // u32   optional   Season episode count (Added in version 6).
    @ColumnInfo(name = "part_number")
    private int partNumber;          // u32   optional   Multi-part episode part number (Added in version 6).
    @ColumnInfo(name = "part_count")
    private int partCount;           // u32   optional   Multi-part episode part count (Added in version 6).
    @ColumnInfo(name = "episode_on_screen")
    private String episodeOnscreen;  // str   optional   Textual representation of episode number (Added in version 6).
    private String image;            // str   optional   URL to a still capture from the episode (Added in version 6).
    @ColumnInfo(name = "dvr_id")
    private int dvrId;               // u32   optional   ID of a recording (Added in version 5).
    @ColumnInfo(name = "next_event_id")
    private int nextEventId;         // u32   optional   ID of next event on the same channel.

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getStop() {
        return stop;
    }

    public void setStop(long stop) {
        this.stop = stop;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSerieslinkId() {
        return serieslinkId;
    }

    public void setSerieslinkId(int serieslinkId) {
        this.serieslinkId = serieslinkId;
    }

    public int getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(int episodeId) {
        this.episodeId = episodeId;
    }

    public int getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(int seasonId) {
        this.seasonId = seasonId;
    }

    public int getBrandId() {
        return brandId;
    }

    public void setBrandId(int brandId) {
        this.brandId = brandId;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public int getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(int ageRating) {
        this.ageRating = ageRating;
    }

    public int getStarRating() {
        return starRating;
    }

    public void setStarRating(int starRating) {
        this.starRating = starRating;
    }

    public long getFirstAired() {
        return firstAired;
    }

    public void setFirstAired(long firstAired) {
        this.firstAired = firstAired;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(int seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public int getSeasonCount() {
        return seasonCount;
    }

    public void setSeasonCount(int seasonCount) {
        this.seasonCount = seasonCount;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public int getPartCount() {
        return partCount;
    }

    public void setPartCount(int partCount) {
        this.partCount = partCount;
    }

    public String getEpisodeOnscreen() {
        return episodeOnscreen;
    }

    public void setEpisodeOnscreen(String episodeOnscreen) {
        this.episodeOnscreen = episodeOnscreen;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getDvrId() {
        return dvrId;
    }

    public void setDvrId(int dvrId) {
        this.dvrId = dvrId;
    }

    public int getNextEventId() {
        return nextEventId;
    }

    public void setNextEventId(int nextEventId) {
        this.nextEventId = nextEventId;
    }
}
