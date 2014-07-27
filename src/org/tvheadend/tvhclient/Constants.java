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
    public static final String BUNDLE_SHOWS_ONLY_CHANNELS = "showOnlyChannels";
    public static final String BUNDLE_RECORDING_TYPE = "recordingType";
    public static final String BUNDLE_DUAL_PANE = "isDualPane";
    public static final String BUNDLE_CHANNEL_ID = "channelId";
    public static final String BUNDLE_PROGRAM_ID = "eventId";
    public static final String BUNDLE_RECORDING_ID = "dvrId";
    public static final String BUNDLE_RECONNECT = "reconnect";
    public static final String BUNDLE_RESTART = "reload";
    public static final String BUNDLE_LIST_POSITION = "prevListPosition";
    public static final String BUNDLE_MENU_POSITION = "menuPosition";
    public static final String BUNDLE_SHOW_CONTROLS = "showControls";

    public static final String RECORDING_TYPE_COMPLETED = "recordingTypeCompleted";
    public static final String RECORDING_TYPE_SCHEDULED = "recordingTypeScheduled";
    public static final String RECORDING_TYPE_FAILED = "recordingTypeFailed";
}
