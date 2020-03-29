package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore

data class ChannelTag(

        var tagId: Int = 0,                     // u32   required   ID of tag.
        var tagName: String? = "",               // str   required   Name of tag.
        var tagIndex: Int = 0,                  // u32   optional   Index value for sorting (default by from min to max) (Added in version 18).
        var tagIcon: String? = null,            // str   optional   URL to an icon representative for the channel.
        var tagTitledIcon: Int = 0,             // u32   optional   Icon includes a title
        var channelCount: Int = 0,
        var connectionId: Int = 0,
        var isSelected: Boolean = false,

        var members: List<Int>? = null          // u32[] optional   Channel IDs of those that belong to the tag
)

@Entity(tableName = "channel_tags", primaryKeys = ["id", "connection_id"])
internal data class ChannelTagEntity(

        @ColumnInfo(name = "id")
        var tagId: Int = 0,                     // u32   required   ID of tag.
        @ColumnInfo(name = "tag_name")
        var tagName: String? = "",               // str   required   Name of tag.
        @ColumnInfo(name = "tag_index")
        var tagIndex: Int = 0,                  // u32   optional   Index value for sorting (default by from min to max) (Added in version 18).
        @ColumnInfo(name = "tag_icon")
        var tagIcon: String? = null,            // str   optional   URL to an icon representative for the channel.
        @ColumnInfo(name = "tag_titled_icon")
        var tagTitledIcon: Int = 0,             // u32   optional   Icon includes a title
        @ColumnInfo(name = "channel_count")
        var channelCount: Int = 0,
        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0,
        @ColumnInfo(name = "is_selected")
        var isSelected: Boolean = false,

        @Ignore
        var members: List<Int>? = null          // u32[] optional   Channel IDs of those that belong to the tag
) {
    companion object {
        fun from(channel: ChannelTag): ChannelTagEntity {
            return ChannelTagEntity(
                    channel.tagId,
                    channel.tagName,
                    channel.tagIndex,
                    channel.tagIcon,
                    channel.tagTitledIcon,
                    channel.channelCount,
                    channel.connectionId,
                    channel.isSelected,
                    channel.members
            )
        }
    }

    fun toChannelTag(): ChannelTag {
        return ChannelTag(tagId, tagName, tagIndex, tagIcon, tagTitledIcon, channelCount, connectionId, isSelected, members)
    }
}
