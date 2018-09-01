package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "server_status", indices = {@Index(value = {"id", "connection_id"}, unique = true)})
public class ServerStatus {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "connection_id")
    public int connectionId;
    @ColumnInfo(name = "connection_name")
    private String connectionName;
    @ColumnInfo(name = "server_name")
    private String serverName;
    @ColumnInfo(name = "server_version")
    private String serverVersion;
    private String webroot;
    private long time = 0;
    private String timezone;
    @ColumnInfo(name = "gmt_offset")
    private int gmtoffset = 0;
    @ColumnInfo(name = "htsp_version")
    private int htspVersion = 13;
    @ColumnInfo(name = "free_disc_space")
    private long freeDiskSpace = 0;
    @ColumnInfo(name = "total_disc_space")
    private long totalDiskSpace = 0;
    @ColumnInfo(name = "channel_tag_id")
    private int channelTagId = 0;
    @ColumnInfo(name = "playback_server_profile_id")
    private int playbackServerProfileId = 0;
    @ColumnInfo(name = "recording_server_profile_id")
    private int recordingServerProfileId = 0;
    @ColumnInfo(name = "casting_server_profile_id")
    private int castingServerProfileId = 0;
    @ColumnInfo(name = "playback_transcoding_profile_id")
    private int playbackTranscodingProfileId = 0;
    @ColumnInfo(name = "recording_transcoding_profile_id")
    private int recordingTranscodingProfileId = 0;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getGmtoffset() {
        return gmtoffset;
    }

    public void setGmtoffset(int gmtoffset) {
        this.gmtoffset = gmtoffset;
    }

    public int getHtspVersion() {
        return htspVersion;
    }

    public void setHtspVersion(int htspVersion) {
        this.htspVersion = htspVersion;
    }

    public long getFreeDiskSpace() {
        return freeDiskSpace;
    }

    public void setFreeDiskSpace(long freeDiskSpace) {
        this.freeDiskSpace = freeDiskSpace;
    }

    public long getTotalDiskSpace() {
        return totalDiskSpace;
    }

    public void setTotalDiskSpace(long totalDiskSpace) {
        this.totalDiskSpace = totalDiskSpace;
    }

    public int getChannelTagId() {
        return channelTagId;
    }

    public void setChannelTagId(int channelTagId) {
        this.channelTagId = channelTagId;
    }

    public int getPlaybackServerProfileId() {
        return playbackServerProfileId;
    }

    public void setPlaybackServerProfileId(int playbackServerProfileId) {
        this.playbackServerProfileId = playbackServerProfileId;
    }

    public int getRecordingServerProfileId() {
        return recordingServerProfileId;
    }

    public void setRecordingServerProfileId(int recordingServerProfileId) {
        this.recordingServerProfileId = recordingServerProfileId;
    }

    public int getCastingServerProfileId() {
        return castingServerProfileId;
    }

    public void setCastingServerProfileId(int castingServerProfileId) {
        this.castingServerProfileId = castingServerProfileId;
    }

    public int getPlaybackTranscodingProfileId() {
        return playbackTranscodingProfileId;
    }

    public void setPlaybackTranscodingProfileId(int playbackTranscodingProfileId) {
        this.playbackTranscodingProfileId = playbackTranscodingProfileId;
    }

    public int getRecordingTranscodingProfileId() {
        return recordingTranscodingProfileId;
    }

    public void setRecordingTranscodingProfileId(int recordingTranscodingProfileId) {
        this.recordingTranscodingProfileId = recordingTranscodingProfileId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getWebroot() {
        return webroot;
    }

    public void setWebroot(String webroot) {
        this.webroot = webroot;
    }
}
