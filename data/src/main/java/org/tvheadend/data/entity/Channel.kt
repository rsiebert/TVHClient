package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import org.tvheadend.data.entity.Recording
import java.util.*
import kotlin.math.floor

@Entity(tableName = "channels", primaryKeys = ["id", "connection_id"])
data class Channel(

        @ColumnInfo(name = "id")
        var id: Int = 0,                        // u32   required   ID of channel.
        @ColumnInfo(name = "number")
        var number: Int = 0,                    // u32   required   Channel number, 0 means not configured.
        @ColumnInfo(name = "number_minor")
        var numberMinor: Int = 0,               // u32   optional   Minor channel number (Added in version 13).
        @ColumnInfo(name = "name")
        var name: String? = null,               // str   required   Name of channel.
        @ColumnInfo(name = "icon")
        var icon: String? = null,               // str   optional   URL to an icon representative for the channel
        @ColumnInfo(name = "event_id")
        var eventId: Int = 0,                   // u32   optional   ID of the current event on this channel.
        @ColumnInfo(name = "next_event_id")
        var nextEventId: Int = 0,               // u32   optional   ID of the next event on the channel.

        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0,

        @ColumnInfo(name = "program_id")
        var programId: Int = 0,
        @ColumnInfo(name = "program_title")
        var programTitle: String? = null,
        @ColumnInfo(name = "program_subtitle")
        var programSubtitle: String? = null,
        @ColumnInfo(name = "program_start")
        var programStart: Long = 0,
        @ColumnInfo(name = "program_stop")
        var programStop: Long = 0,
        @ColumnInfo(name = "program_content_type")
        var programContentType: Int = 0,
        @ColumnInfo(name = "next_program_id")
        var nextProgramId: Int = 0,
        @ColumnInfo(name = "next_program_title")
        var nextProgramTitle: String? = null,

        @ColumnInfo(name = "display_number")
        var displayNumber: String? = null,
        @ColumnInfo(name = "server_order")
        var serverOrder: Int = 0,

        @Ignore
        var tags: List<Int>? = null,

        @Ignore
        var recording: Recording? = null
) {
    val duration: Int
        get() = ((programStop - programStart) / 1000 / 60).toInt()

    val progress: Int
        get() {
            var percentage = 0.0
            // Get the start and end times to calculate the progress.
            val durationTime = (programStop - programStart).toDouble()
            val elapsedTime = (Date().time - programStart).toDouble()
            // Show the progress as a percentage
            if (durationTime > 0) {
                percentage = elapsedTime / durationTime
            }
            return floor(percentage * 100).toInt()
        }
}
