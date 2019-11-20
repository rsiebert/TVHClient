package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "server_profiles", indices = [Index(value = ["id"], unique = true)])
data class ServerProfile(

        @PrimaryKey(autoGenerate = true)
        var id: Int = 0,
        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0,
        @ColumnInfo(name = "is_enabled")
        var isEnabled: Boolean = false,
        var name: String? = null,
        var uuid: String? = null,
        var comment: String? = null,
        var type: String? = null
)
