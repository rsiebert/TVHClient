package org.tvheadend.tvhclient.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "tags_and_channels", primaryKeys = ["tag_id", "channel_id", "connection_id"])
data class TagAndChannel(

        @ColumnInfo(name = "tag_id")
        var tagId: Int = 0,
        @ColumnInfo(name = "channel_id")
        var channelId: Int = 0,
        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0
)
