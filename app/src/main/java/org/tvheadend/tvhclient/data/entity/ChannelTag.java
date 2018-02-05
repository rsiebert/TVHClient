package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "channel_tags")
public class ChannelTag {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private int tagId;               // u32   required   ID of tag.
    @ColumnInfo(name = "tag_name")
    private String tagName;          // str   required   Name of tag.
    @ColumnInfo(name = "tag_index")
    private int tagIndex;            // u32   optional   Index value for sorting (default by from min to max) (Added in version 18).
    @ColumnInfo(name = "tag_icon")
    private String tagIcon;          // str   optional   URL to an icon representative for the channel.
    @ColumnInfo(name = "tag_titled_icon")
    private int tagTitledIcon;       // u32   optional   Icon includes a title
    private boolean active;
    @Ignore
    private List<Integer> members;   // u32[] optional   Channel IDs of those that belong to the tag

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public int getTagIndex() {
        return tagIndex;
    }

    public void setTagIndex(int tagIndex) {
        this.tagIndex = tagIndex;
    }

    public String getTagIcon() {
        return tagIcon;
    }

    public void setTagIcon(String tagIcon) {
        this.tagIcon = tagIcon;
    }

    public int getTagTitledIcon() {
        return tagTitledIcon;
    }

    public void setTagTitledIcon(int tagTitledIcon) {
        this.tagTitledIcon = tagTitledIcon;
    }

    public List<Integer> getMembers() {
        return members;
    }

    public void setMembers(List<Integer> members) {
        this.members = members;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
