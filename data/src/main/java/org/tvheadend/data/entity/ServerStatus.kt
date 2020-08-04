package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

data class ServerStatus(

        var id: Int = 0,
        var connectionId: Int = 0,
        var connectionName: String? = null,
        var serverName: String? = null,
        var serverVersion: String? = null,
        var webroot: String? = null,
        var time: Long = 0,
        var timezone: String? = null,
        var gmtoffset: Int = 0,
        var htspVersion: Int = 13,
        var freeDiskSpace: Long = 0,
        var totalDiskSpace: Long = 0,
        var channelTagId: Int = 0,
        var htspPlaybackServerProfileId: Int = 0,
        var httpPlaybackServerProfileId: Int = 0,
        var recordingServerProfileId: Int = 0,
        var seriesRecordingServerProfileId: Int = 0,
        var timerRecordingServerProfileId: Int = 0,
        var castingServerProfileId: Int = 0,
        var playbackTranscodingProfileId: Int = 0,
        var recordingTranscodingProfileId: Int = 0
)

@Entity(tableName = "server_status", indices = [Index(value = ["id", "connection_id"], unique = true)])
internal data class ServerStatusEntity(

        @PrimaryKey(autoGenerate = true)
        var id: Int = 0,
        @ColumnInfo(name = "connection_id")
        var connectionId: Int = 0,
        @ColumnInfo(name = "connection_name")
        var connectionName: String? = null,
        @ColumnInfo(name = "server_name")
        var serverName: String? = null,
        @ColumnInfo(name = "server_version")
        var serverVersion: String? = null,
        var webroot: String? = null,
        var time: Long = 0,
        var timezone: String? = null,
        @ColumnInfo(name = "gmt_offset")
        var gmtoffset: Int = 0,
        @ColumnInfo(name = "htsp_version")
        var htspVersion: Int = 13,
        @ColumnInfo(name = "free_disc_space")
        var freeDiskSpace: Long = 0,
        @ColumnInfo(name = "total_disc_space")
        var totalDiskSpace: Long = 0,
        @ColumnInfo(name = "channel_tag_id")
        var channelTagId: Int = 0,
        @ColumnInfo(name = "playback_server_profile_id")
        var htspPlaybackServerProfileId: Int = 0,
        @ColumnInfo(name = "http_playback_server_profile_id")
        var httpPlaybackServerProfileId: Int = 0,
        @ColumnInfo(name = "recording_server_profile_id")
        var recordingServerProfileId: Int = 0,
        @ColumnInfo(name = "series_recording_server_profile_id")
        var seriesRecordingServerProfileId: Int = 0,
        @ColumnInfo(name = "timer_recording_server_profile_id")
        var timerRecordingServerProfileId: Int = 0,
        @ColumnInfo(name = "casting_server_profile_id")
        var castingServerProfileId: Int = 0,
        @ColumnInfo(name = "playback_transcoding_profile_id")
        var playbackTranscodingProfileId: Int = 0,
        @ColumnInfo(name = "recording_transcoding_profile_id")
        var recordingTranscodingProfileId: Int = 0
) {
    companion object {
        fun from(serverStatus: ServerStatus): ServerStatusEntity {
            return ServerStatusEntity(
                    serverStatus.id,
                    serverStatus.connectionId,
                    serverStatus.connectionName,
                    serverStatus.serverName,
                    serverStatus.serverVersion,
                    serverStatus.webroot,
                    serverStatus.time,
                    serverStatus.timezone,
                    serverStatus.gmtoffset,
                    serverStatus.htspVersion,
                    serverStatus.freeDiskSpace,
                    serverStatus.totalDiskSpace,
                    serverStatus.channelTagId,
                    serverStatus.htspPlaybackServerProfileId,
                    serverStatus.httpPlaybackServerProfileId,
                    serverStatus.recordingServerProfileId,
                    serverStatus.seriesRecordingServerProfileId,
                    serverStatus.timerRecordingServerProfileId,
                    serverStatus.castingServerProfileId,
                    serverStatus.playbackTranscodingProfileId,
                    serverStatus.recordingTranscodingProfileId)
        }
    }

    fun toServerStatus(): ServerStatus {
        return ServerStatus(id, connectionId, connectionName, serverName, serverVersion, webroot, time, timezone, gmtoffset, htspVersion, freeDiskSpace, totalDiskSpace, channelTagId, htspPlaybackServerProfileId, httpPlaybackServerProfileId, recordingServerProfileId, seriesRecordingServerProfileId, timerRecordingServerProfileId, castingServerProfileId, playbackTranscodingProfileId, recordingTranscodingProfileId)
    }
}
