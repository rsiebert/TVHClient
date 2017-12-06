package org.tvheadend.tvhclient.model;

import java.util.List;

public class ChannelTag {
    public int tagId;               // u32   required   ID of tag.
    public String tagName;          // str   required   Name of tag.
    public int tagIndex;            // u32   optional   Index value for sorting (default by from min to max) (Added in version 18).
    public String tagIcon;          // str   optional   URL to an icon representative for the channel.
    public int tagTitledIcon;       // u32   optional   Icon includes a title
    public List<Integer> members;   // u32[] optional   Channel IDs of those that belong to the tag
}
