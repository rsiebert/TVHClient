package org.tvheadend.tvhclient;

public class Constants {
    public static final int RESULT_CODE_CONNECTIONS = 100;
    public static final int RESULT_CODE_SETTINGS = 101;
    public static final int RESULT_CODE_RECORDINGS = 102;
    public static final int RESULT_CODE_PROGRAM_GUIDE = 103;
    public static final int RESULT_CODE_START_PLAYER = 104;
    
    // Amount of programs of a channel that shall be loaded from the server 
    public static final int PREF_PROGRAMS_TO_LOAD = 15;
    
    // Fixed names which are used to identify the information passed via a bundle
    public static final String BUNDLE_SHOWS_ONLY_CHANNELS = "show_only_channels";
    public static final String BUNDLE_RECORDING_TYPE = "recording_type";
    public static final String BUNDLE_DUAL_PANE = "is_dual_pane";
    public static final String BUNDLE_CHANNEL_ID = "channel_id";
    public static final String BUNDLE_PROGRAM_ID = "program_id";
    public static final String BUNDLE_RECONNECT = "reconnect";
    public static final String BUNDLE_RESTART = "reload";
}
