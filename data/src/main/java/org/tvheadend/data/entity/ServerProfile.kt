package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

data class ServerProfile(

        var id: Int = 0,
        var connectionId: Int = 0,
        var isEnabled: Boolean = false,
        var name: String? = null,
        var uuid: String? = null,
        var comment: String? = null,
        var type: String? = null
) {
    companion object {
        const val HTSP_PROFILE: String = "htsp_playback"
        const val HTTP_PROFILE: String = "http_playback"
        const val RECORDING_PROFILE: String = "recording"
    }
}

@Entity(tableName = "server_profiles", indices = [Index(value = ["id"], unique = true)])
data class ServerProfileEntity(

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
) {
    companion object {
        fun from(serverProfile: ServerProfile): ServerProfileEntity {
            return ServerProfileEntity(
                    serverProfile.id,
                    serverProfile.connectionId,
                    serverProfile.isEnabled,
                    serverProfile.name,
                    serverProfile.uuid,
                    serverProfile.comment,
                    serverProfile.type)
        }
    }

    fun toServerProfile(): ServerProfile {
        return ServerProfile(id, connectionId, isEnabled, name, uuid, comment, type)
    }
}
