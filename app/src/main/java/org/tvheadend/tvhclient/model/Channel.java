package org.tvheadend.tvhclient.model;

import java.util.List;

public class Channel {

    public int channelId;             // u32   required   ID of channel.
    public int channelNumber;         // u32   required   Channel number, 0 means unconfigured.
    public int channelNumberMinor;    // u32   optional   Minor channel number (Added in version 13).
    public String channelName;        // str   required   Name of channel.
    public String channelIcon;        // str   optional   URL to an icon representative for the channel
    public int eventId;               // u32   optional   ID of the current event on this channel.
    public int nextEventId;           // u32   optional   ID of the next event on the channel.
    public List<Integer> tags;        // u32[] optional   Tags this channel is mapped to.
    //services           msg[] optional   List of available services (Added in version 5)
}
