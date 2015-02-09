package org.tvheadend.tvhclient.model;

public class Profile {
    public int id = 0;
    // use the new profile names (requires htsp api version > 15)
    public boolean enabled = false;
    // The uuid and name define the profile to be used
    public String uuid = "";
    public String name = "";

    // The old profile definitions that are passed to the server in the url 
    public String container = "matroska";
    public boolean transcode = false;
    public String resolution = "384";
    public String audio_codec = "AAC";
    public String video_codec = "H264";
    public String subtitle_codec = "NONE";
}
