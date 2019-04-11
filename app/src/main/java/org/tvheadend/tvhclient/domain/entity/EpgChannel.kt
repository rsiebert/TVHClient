package org.tvheadend.tvhclient.domain.entity

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
