package org.tvheadend.tvhclient.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Ignore

data class EpgProgram(

        @ColumnInfo(name = "id")
        var eventId: Int = 0,                   // u32   required   Event ID
        @ColumnInfo(name = "channel_id")
        var channelId: Int = 0,                 // u32   required   The channel this event is related to.
        var start: Long = 0,                    // u64   required   Start time of event, UNIX time.
        var stop: Long = 0,                     // u64   required   Ending time of event, UNIX time.
        var title: String? = null,              // str   optional   Title of event.
        var subtitle: String? = null,           // str   optional   Subtitle of event.
        @ColumnInfo(name = "content_type")
        var contentType: Int = 0,               // u32   optional   DVB content code (Added in version 4, Modified in version 6*).

        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0,
        @ColumnInfo(name = "channel_name")
        var channelName: String? = null,
        @ColumnInfo(name = "channel_icon")
        var channelIcon: String? = null,

        @Ignore
        var recording: Recording? = null
) {
    val duration: Int
        get() = ((stop - start) / 1000 / 60).toInt()
}
