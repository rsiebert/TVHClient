package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;

@Entity(tableName = "tags_and_channels", primaryKeys = {"tag_id", "channel_id", "connection_id"})
public class TagAndChannel {

    @ColumnInfo(name = "tag_id")
    private int tagId;
    @ColumnInfo(name = "channel_id")
    private int channelId;
    @ColumnInfo(name = "connection_id")
    private int connectionId;

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }
}
