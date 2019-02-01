package org.tvheadend.tvhclient.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import java.util.*

@Entity(tableName = "series_recordings", primaryKeys = ["id", "connection_id"])
data class SeriesRecording(

        @Ignore
        private val time: Long = Calendar.getInstance().timeInMillis,
        @Ignore
        private val offset: Long = (30 * 60 * 1000).toLong(),

        var id: String = "",                    // str   required   ID (string!) of dvrAutorecEntry.
        @ColumnInfo(name = "enabled")
        var isEnabled: Boolean = true,          // u32   required   If autorec entry is enabled (activated).
        var name: String? = null,               // str   required   Name of the autorec entry (Added in version 18).
        @ColumnInfo(name = "min_duration")
        var minDuration: Int = 0,               // u32   required   Minimal duration in seconds (0 = Any).
        @ColumnInfo(name = "max_duration")
        var maxDuration: Int = 0,               // u32   required   Maximal duration in seconds (0 = Any).
        var retention: Int = 0,                 // u32   required   Retention time (in days).
        @ColumnInfo(name = "days_of_week")
        var daysOfWeek: Int = 127,              // u32   required   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
        var priority: Int = 2,                  // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
        @ColumnInfo(name = "approx_time")
        var approxTime: Int = 0,                // u32   required   Minutes from midnight (up to 24*60).
        var start: Long = time,                 // s32   required   Exact start time (minutes from midnight) (Added in version 18).
        @ColumnInfo(name = "start_window")
        var startWindow: Long = time + offset,  // s32   required   Exact stop time (minutes from midnight) (Added in version 18).
        @ColumnInfo(name = "start_extra")
        var startExtra: Long = 0,               // s64   required   Extra start minutes (pre-time).
        @ColumnInfo(name = "stop_extra")
        var stopExtra: Long = 0,                // s64   required   Extra stop minutes (post-time).
        var title: String? = null,              // str   optional   Title.
        var fulltext: Int = 0,                  // u32   optional   Fulltext flag (Added in version 20).
        var directory: String? = null,          // str   optional   Forced directory name (Added in version 19).
        @ColumnInfo(name = "channel_id")
        var channelId: Int = 0,                 // u32   optional   Channel ID.
        var owner: String? = null,              // str   optional   Owner of this autorec entry (Added in version 18).
        var creator: String? = null,            // str   optional   Creator of this autorec entry (Added in version 18).
        @ColumnInfo(name = "dup_detect")
        var dupDetect: Int = 0,                 // u32   optional   Duplicate detection (see addAutorecEntry) (Added in version 20).
        var removal: Int = 0,                   // u32   optional   Number of days to keep recorded files (Added in version 32)
        @ColumnInfo(name = "max_count")
        var maxCount: Int = 0,                  // u32   optional   The maximum number of entries that can be matched (Added in version 32)

        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0,
        @ColumnInfo(name = "channel_name")
        var channelName: String? = null,
        @ColumnInfo(name = "channel_icon")
        var channelIcon: String? = null,

        @Ignore
        var isTimeEnabled: Boolean = start >= 0 || startWindow >= 0
) {
    val duration: Int
        get() = ((startWindow - start) / 60 / 1000).toInt()

}