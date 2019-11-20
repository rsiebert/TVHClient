package org.tvheadend.tvhclient.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.*

@Entity(tableName = "timer_recordings", primaryKeys = ["id", "connection_id"])
data class TimerRecording(

        var id: String = "",                // str   required   ID (string!) of dvrTimerecEntry.
        var title: String? = "",            // str   required   Title for the recordings.
        var directory: String? = null,      // str   optional   Forced directory name (Added in version 19).
        @ColumnInfo(name = "enabled")
        var isEnabled: Boolean = true,      // u32   required   Title for the recordings.
        var name: String? = "",             // str   required   Name for this timerec entry.
        @ColumnInfo(name = "config_name")
        var configName: String? = "",       // str   required   DVR Configuration Name / UUID.
        @ColumnInfo(name = "channel_id")
        var channelId: Int = 0,             // u32   required   Channel ID.
        @ColumnInfo(name = "days_of_week")
        var daysOfWeek: Int = 127,          // u32   optional   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
        var priority: Int = 2,              // u32   optional   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
        var start: Long = 0,                // u32   required   Minutes from midnight (up to 24*60) for the start of the time window (including)
        var stop: Long = 0,                 // u32   required   Minutes from midnight (up to 24*60) for the end of the time window (including, cross-noon allowed)
        var retention: Int = 0,             // u32   optional   Retention in days.
        var owner: String? = null,          // str   optional   Owner of this timerec entry.
        var creator: String? = null,        // str   optional   Creator of this timerec entry.
        var removal: Int = 0,               // u32   optional   Number of days to keep recorded files (Added in version 32)

        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0,
        @ColumnInfo(name = "channel_name")
        var channelName: String? = null,
        @ColumnInfo(name = "channel_icon")
        var channelIcon: String? = null
) {
    val duration: Int
        get() = (stop - start).toInt()

    /**
     * The start time in milliseconds from the current time at 0 o'clock plus the given minutes
     */
    @Suppress("unused")
    val startTimeInMillis: Long
        get() {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis + (start * 60 * 1000)
        }

    /**
     * The stop time in milliseconds from the current time at 0 o'clock plus the given minutes
     */
    @Suppress("unused")
    val stopTimeInMillis: Long
        get() {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis + (stop * 60 * 1000)
        }
}
