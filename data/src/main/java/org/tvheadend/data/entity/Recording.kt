package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import java.util.*

@Entity(tableName = "recordings", primaryKeys = ["id", "connection_id"])
data class Recording(

        @Ignore
        private val time: Long = Calendar.getInstance().timeInMillis,
        @Ignore
        private val offset: Long = (30 * 60 * 1000).toLong(),

        var id: Int = 0,                            // u32   required   ID of dvrEntry.
        @ColumnInfo(name = "channel_id")
        var channelId: Int = 0,                     // u32   optional   Channel of dvrEntry.
        var start: Long = time,                     // s64   required   Time of when this entry was scheduled to start recording.
        var stop: Long = time + offset,             // s64   required   Time of when this entry was scheduled to stop recording.
        @ColumnInfo(name = "start_extra")
        var startExtra: Long = 1,                   // s64   required   Extra start time (pre-time) in minutes (Added in version 13).
        @ColumnInfo(name = "stop_extra")
        var stopExtra: Long = 15,                   // s64   required   Extra stop time (post-time) in minutes (Added in version 13).
        var retention: Long = 0,                    // s64   required   DVR Entry retention time in days (Added in version 13).
        var priority: Int = 2,                      // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set) (Added in version 13).
        @ColumnInfo(name = "event_id")
        var eventId: Int = 0,                       // u32   optional   Associated EPG Event ID (Added in version 13).
        @ColumnInfo(name = "autorec_id")
        var autorecId: String? = "",                 // str   optional   Associated Autorec UUID (Added in version 13).
        @ColumnInfo(name = "timerec_id")
        var timerecId: String? = "",                 // str   optional   Associated Timerec UUID (Added in version 18).
        @ColumnInfo(name = "content_type")
        var contentType: Int = 0,                   // u32   optional   Content Type (like in the DVB standard) (Added in version 13).
        var title: String? = null,                  // str   optional   Title of recording
        var subtitle: String? = null,               // str   optional   Subtitle of recording (Added in version 20).
        var summary: String? = null,                // str   optional   Short description of the recording (Added in version 6).
        var description: String? = null,            // str   optional   Long description of the recording.
        var state: String? = null,                  // str   required   Recording state
        var error: String? = null,                  // str   optional   Plain english error description (e.g. "Aborted by user").
        var owner: String? = null,                  // str   optional   Name of the entry owner (Added in version 18).
        var creator: String? = null,                // str   optional   Name of the entry creator (Added in version 18).
        @ColumnInfo(name = "subscription_error")
        var subscriptionError: String? = null,      // str   optional   Subscription error string (Added in version 20).
        @ColumnInfo(name = "stream_errors")
        var streamErrors: String? = null,           // str   optional   Number of recording errors (Added in version 20).
        @ColumnInfo(name = "data_errors")
        var dataErrors: String? = null,             // str   optional   Number of stream data errors (Added in version 20).
        var path: String? = null,                   // str   optional   Recording path for playback.
        @ColumnInfo(name = "data_size")
        var dataSize: Long = 0,                     // s64   optional   Actual file size of the last recordings (Added in version 21).
        @ColumnInfo(name = "enabled")
        var isEnabled: Boolean = true,              // u32   optional   Enabled flag (Added in version 23).
        var duplicate: Int = 0,                     // u32   optional   Duplicate flag (Added in version 33).
        var episode: String? = null,                // str   optional   Episode (Added in version 18).
        var comment: String? = null,                // str   optional   Comment (Added in version 18).
        var image: String? = null,                  // str   optional   Artwork for a recording
        @ColumnInfo(name = "fanart_image")
        var fanartImage: String? = null,            // str   optional   Fanbased artwork for a recording (Added in version 33)
        @ColumnInfo(name = "copyright_year")
        var copyrightYear: Int = 0,                 // str   optional   The copyright year (Added in version 33)
        var removal: Int = 0,                       // u32   optional   Number of days to keep recorded files (Added in version 32)

        @Ignore
        var files: List<String>? = null,            // msg   optional   All recorded files for playback (Added in version 21).

        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0,
        @ColumnInfo(name = "channel_name")
        var channelName: String? = null,
        @ColumnInfo(name = "channel_icon")
        var channelIcon: String? = null,

        var duration: Int = 0
) {

    val isCompleted: Boolean
        get() = error == null && state.isEqualTo("completed")

    val isRecording: Boolean
        get() = error == null && state.isEqualTo("recording")

    val isScheduled: Boolean
        get() = error == null && state.isEqualTo("scheduled")

    val isFailed: Boolean
        get() = state.isEqualTo("invalid")

    val isMissed: Boolean
        get() = state.isEqualTo("missed")

    val isAborted: Boolean
        get() = error.isEqualTo("Aborted by user") && state.isEqualTo("completed")

    val isFileMissing: Boolean
        get() = error.isEqualTo("File missing") && state.isEqualTo("completed")

    private fun CharSequence?.isEqualTo(s: String?): Boolean {
        return if (this == null && s == null) {
            true
        } else if (this != null && s != null) {
            this == s
        } else {
            false
        }
    }
}
