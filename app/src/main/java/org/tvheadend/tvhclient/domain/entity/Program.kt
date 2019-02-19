package org.tvheadend.tvhclient.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import java.util.*

@Entity(tableName = "programs", primaryKeys = ["id", "connection_id"], indices = [Index(value = ["start"]), Index(value = ["channel_id"])])
data class Program(

        @ColumnInfo(name = "id")
        override var eventId: Int = 0,                    // u32   required   Event ID
        @ColumnInfo(name = "channel_id")
        override var channelId: Int = 0,                  // u32   required   The channel this event is related to.
        override var start: Long = 0,                     // u64   required   Start time of event, UNIX time.
        override var stop: Long = 0,                      // u64   required   Ending time of event, UNIX time.
        override var title: String? = null,               // str   optional   Title of event.
        var subtitle: String? = null,            // str   optional   Subtitle of event.
        var summary: String? = null,             // str   optional   Short description of the event (Added in version 6).
        var description: String? = null,         // str   optional   Long description of the event.
        var credits: String? = null,             // str   optional
        var category: String? = null,            // str   optional
        var keyword: String? = null,             // str   optional
        @ColumnInfo(name = "series_link_id")
        var serieslinkId: Int = 0,               // u32   optional   Series Link ID (Added in version 6).
        @ColumnInfo(name = "episode_id")
        var episodeId: Int = 0,                  // u32   optional   Episode ID (Added in version 6).
        @ColumnInfo(name = "season_id")
        var seasonId: Int = 0,                   // u32   optional   Season ID (Added in version 6).
        @ColumnInfo(name = "brand_id")
        var brandId: Int = 0,                    // u32   optional   Brand ID (Added in version 6).
        @ColumnInfo(name = "content_type")
        var contentType: Int = 0,                // u32   optional   DVB content code (Added in version 4, Modified in version 6*).
        @ColumnInfo(name = "age_rating")
        var ageRating: Int = 0,                  // u32   optional   Minimum age rating (Added in version 6).
        @ColumnInfo(name = "star_rating")
        var starRating: Int = 0,                 // u32   optional   Star rating (1-5) (Added in version 6).
        @ColumnInfo(name = "copyright_year")
        var copyrightYear: Int = 0,              // str   optional   The copyright year (Added in version 33)
        @ColumnInfo(name = "first_aired")
        var firstAired: Long = 0,                // s64   optional   Original broadcast time, UNIX time (Added in version 6).
        @ColumnInfo(name = "season_number")
        var seasonNumber: Int = 0,               // u32   optional   Season number (Added in version 6).
        @ColumnInfo(name = "season_count")
        var seasonCount: Int = 0,                // u32   optional   Show season count (Added in version 6).
        @ColumnInfo(name = "episode_number")
        var episodeNumber: Int = 0,              // u32   optional   Episode number (Added in version 6).
        @ColumnInfo(name = "episode_count")
        var episodeCount: Int = 0,               // u32   optional   Season episode count (Added in version 6).
        @ColumnInfo(name = "part_number")
        var partNumber: Int = 0,                 // u32   optional   Multi-part episode part number (Added in version 6).
        @ColumnInfo(name = "part_count")
        var partCount: Int = 0,                  // u32   optional   Multi-part episode part count (Added in version 6).
        @ColumnInfo(name = "episode_on_screen")
        var episodeOnscreen: String? = null,         // str   optional   Textual representation of episode number (Added in version 6).
        var image: String? = null,                   // str   optional   URL to a still capture from the episode (Added in version 6).
        @ColumnInfo(name = "dvr_id")
        var dvrId: Int = 0,                      // u32   optional   ID of a recording (Added in version 5).
        @ColumnInfo(name = "next_event_id")
        var nextEventId: Int = 0,                // u32   optional   ID of next event on the same channel.
        @ColumnInfo(name = "series_link_uri")
        var serieslinkUri: String? = null,           // str   optional
        @ColumnInfo(name = "episode_uri")
        var episodeUri: String? = null,              // str   optional

        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0,
        @ColumnInfo(name = "channel_name")
        var channelName: String? = null,
        @ColumnInfo(name = "channel_icon")
        var channelIcon: String? = null,

        @Ignore
        var recording: Recording? = null
) : ProgramInterface {
    val duration: Int
        get() = ((stop - start) / 1000 / 60).toInt()

    val progress: Int
        get() {
            var percentage = 0.0
            // Get the start and end times to calculate the progress.
            val durationTime = (stop - start).toDouble()
            val elapsedTime = (Date().time - start).toDouble()
            // Show the progress as a percentage
            if (durationTime > 0 && elapsedTime > 0) {
                percentage = elapsedTime / durationTime
            }
            return Math.floor(percentage * 100).toInt()
        }
}
