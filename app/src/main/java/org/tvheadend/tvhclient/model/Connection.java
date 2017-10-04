package org.tvheadend.tvhclient.model;

public class Connection {
    public long id;
    public String name;
    public String address;
    public int port;
    public String username;
    public String password;
    public boolean selected;
    public int channelTag;
    public int streaming_port;
    public String wol_mac_address;
    public int wol_port;
    public boolean wol_broadcast;
    public int playback_profile_id;
    public int recording_profile_id;
    public int cast_profile_id;

    public long time;
    public int gmtOffset;
    public int freeDiskSpace;
    public int totalDiskSpace;
    public String htspVersion;
    public String serverName;
    public String serverVersion;
    public String webRoot;
}
