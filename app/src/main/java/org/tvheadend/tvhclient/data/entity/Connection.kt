package org.tvheadend.tvhclient.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "connections", indices = [Index(value = ["id"], unique = true)])
data class Connection(

        @PrimaryKey(autoGenerate = true)
        var id: Int = 0,
        var name: String? = "",
        var hostname: String? = "",
        var port: Int = 9982,
        var username: String? = "",
        var password: String? = "",
        @ColumnInfo(name = "active")
        var isActive: Boolean = false,
        @ColumnInfo(name = "streaming_port")
        var streamingPort: Int = 9981,
        @ColumnInfo(name = "wol_enabled")
        var isWolEnabled: Boolean = false,
        @ColumnInfo(name = "wol_hostname")
        var wolMacAddress: String? = "",
        @ColumnInfo(name = "wol_port")
        var wolPort: Int = 9,
        @ColumnInfo(name = "wol_use_broadcast")
        var isWolUseBroadcast: Boolean = false,
        @ColumnInfo(name = "last_update")
        var lastUpdate: Long = 0,
        @ColumnInfo(name = "sync_required")
        var isSyncRequired: Boolean = true,
        @ColumnInfo(name = "server_url")
        var serverUrl: String? = "",
        @ColumnInfo(name = "streaming_url")
        var streamingUrl: String? = ""
)
