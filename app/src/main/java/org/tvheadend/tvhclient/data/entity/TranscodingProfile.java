package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "transcoding_profiles")
public class TranscodingProfile {

    @PrimaryKey(autoGenerate = true)
    private int id = 0;
    @ColumnInfo(name = "connection_id")
    private int connectionId;
    @ColumnInfo(name = "is_enabled")
    private boolean isEnabled = false;
    private String container = "matroska";
    private boolean transcode = false;
    private String resolution = "384";
    @ColumnInfo(name = "audio_codec")
    private String audioCodec = "AAC";
    @ColumnInfo(name = "video_codec")
    private String videoCodec = "H264";
    @ColumnInfo(name = "subtitle_codec")
    private String subtitleCodec = "NONE";

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

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public boolean isTranscode() {
        return transcode;
    }

    public void setTranscode(boolean transcode) {
        this.transcode = transcode;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getSubtitleCodec() {
        return subtitleCodec;
    }

    public void setSubtitleCodec(String subtitleCodec) {
        this.subtitleCodec = subtitleCodec;
    }
}
