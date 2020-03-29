package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

data class TagAndChannel(

        var tagId: Int = 0,
        var channelId: Int = 0,
        var connectionId: Int = 0
)

@Entity(tableName = "tags_and_channels", primaryKeys = ["tag_id", "channel_id", "connection_id"])
internal data class TagAndChannelEntity(

        @ColumnInfo(name = "tag_id")
        var tagId: Int = 0,
        @ColumnInfo(name = "channel_id")
        var channelId: Int = 0,
        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0
) {
    companion object {
        fun from(tagAndChannel: TagAndChannel): TagAndChannelEntity {
            return TagAndChannelEntity(
                    tagAndChannel.tagId,
                    tagAndChannel.channelId,
                    tagAndChannel.connectionId)
        }
    }

    fun toTagAndChannel(): TagAndChannel {
        return TagAndChannel(tagId, channelId, connectionId)
    }
}
