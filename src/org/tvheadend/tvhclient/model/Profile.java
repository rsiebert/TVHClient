package org.tvheadend.tvhclient.model;

public class Profile {
    public int id;
    // use the new profile names (requires htsp api version > 15)
    public boolean enabled;
    // The uuid that the defines the profile to be used
    public String uuid;

    // The old profile definitions that are passed to the server in the url 
    public String container;
    public boolean transcode;
    public String resolution;
    public String audio_codec;
    public String video_codec;
    public String subtitle_codec;
}
