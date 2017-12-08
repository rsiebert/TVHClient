package org.tvheadend.tvhclient.model;

public class Connection {
    public long id = 0;
    public String name = "";
    public String address = "";
    public int port = 9982;
    public String username = "";
    public String password = "";
    public boolean selected = false;
    public int channelTag = 0;
    public int streaming_port = 9981;
    public String wol_mac_address = "";
    public int wol_port = 9;
    public boolean wol_broadcast = false;
    public int playback_profile_id = 0;
    public int recording_profile_id = 0;
    public int cast_profile_id = 0;
}
