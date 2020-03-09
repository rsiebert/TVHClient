package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

data class Connection(

        var id: Int = 0,
        var name: String? = "",
        var hostname: String? = "",
        var port: Int = 9982,
        var username: String? = "",
        var password: String? = "",
        var isActive: Boolean = false,
        var streamingPort: Int = 9981,
        var isWolEnabled: Boolean = false,
        var wolMacAddress: String? = "",
        var wolPort: Int = 9,
        var isWolUseBroadcast: Boolean = false,
        var lastUpdate: Long = 0,
        var isSyncRequired: Boolean = true,
        var serverUrl: String? = "",
        var streamingUrl: String? = ""
)

@Entity(tableName = "connections", indices = [Index(value = ["id"], unique = true)])
internal data class ConnectionEntity(

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
) {
    companion object {
        fun from(connection: Connection): ConnectionEntity {
            return ConnectionEntity(
                    connection.id,
                    connection.name,
                    connection.hostname,
                    connection.port,
                    connection.username,
                    connection.password,
                    connection.isActive,
                    connection.streamingPort,
                    connection.isWolEnabled,
                    connection.wolMacAddress,
                    connection.wolPort,
                    connection.isWolUseBroadcast,
                    connection.lastUpdate,
                    connection.isSyncRequired,
                    connection.serverUrl,
                    connection.streamingUrl)
        }
    }

    fun toConnection(): Connection {
        return Connection(id, name, hostname, port, username, password, isActive, streamingPort, isWolEnabled, wolMacAddress, wolPort, isWolUseBroadcast, lastUpdate, isSyncRequired, serverUrl, streamingUrl)
    }
}
