package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "server_status", indices = [Index(value = ["id", "connection_id"], unique = true)])
data class ServerStatus(

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
        @ColumnInfo(name = "casting_server_profile_id")
        var castingServerProfileId: Int = 0,
        @ColumnInfo(name = "playback_transcoding_profile_id")
        var playbackTranscodingProfileId: Int = 0,
        @ColumnInfo(name = "recording_transcoding_profile_id")
        var recordingTranscodingProfileId: Int = 0
)
