package org.tvheadend.data.entity

import androidx.room.ColumnInfo

data class EpgChannel(

        @ColumnInfo(name = "id")
        var id: Int = 0,
        @ColumnInfo(name = "number")
        var number: Int = 0,
        @ColumnInfo(name = "number_minor")
        var numberMinor: Int = 0,
        @ColumnInfo(name = "name")
        var name: String? = null,
        @ColumnInfo(name = "icon")
        var icon: String? = null,
        @ColumnInfo(name = "display_number")
        var displayNumber: String? = null
)

internal data class EpgChannelEntity(

        @ColumnInfo(name = "id")
        var id: Int = 0,
        @ColumnInfo(name = "number")
        var number: Int = 0,
        @ColumnInfo(name = "number_minor")
        var numberMinor: Int = 0,
        @ColumnInfo(name = "name")
        var name: String? = null,
        @ColumnInfo(name = "icon")
        var icon: String? = null,
        @ColumnInfo(name = "display_number")
        var displayNumber: String? = null
) {
    companion object {
        fun from(channel: EpgChannel): EpgChannelEntity {
            return EpgChannelEntity(
                    channel.id,
                    channel.number,
                    channel.numberMinor,
                    channel.name,
                    channel.icon,
                    channel.displayNumber
            )
        }
    }

    fun toEpgChannel(): EpgChannel {
        return EpgChannel(id, number, numberMinor, name, icon, displayNumber)
    }
}
