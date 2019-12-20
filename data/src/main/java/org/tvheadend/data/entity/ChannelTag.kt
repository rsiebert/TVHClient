package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore

@Entity(tableName = "channel_tags", primaryKeys = ["id", "connection_id"])
data class ChannelTag(

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
)
