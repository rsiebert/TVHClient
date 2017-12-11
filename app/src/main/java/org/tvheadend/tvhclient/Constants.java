package org.tvheadend.tvhclient;

public class Constants {

    // Product id for the in-app billing item to unlock the application
    public static final String UNLOCKER = "unlocker";

    // Chromecast stuff
    public static final String CAST_APPLICATION_ID = "0531DF56";
    public static final double CAST_VOLUME_INCREMENT = 0.05;

    // Codes to identify the returning activity
    public static final int RESULT_CODE_SETTINGS = 101;
    public static final int RESULT_CODE_START_PLAYER = 102;

    // Amount of programs of a channel that shall be loaded from the server
    public static final int PREF_PROGRAMS_TO_LOAD = 15;

    // Amount of programs that must be visible before loading more programs
    public static final int PROGRAMS_VISIBLE_BEFORE_LOADING_MORE = 15;

    // Strings used to identify the information passed via a bundle
    public static final String BUNDLE_RECONNECT = "reconnect";
    public static final String BUNDLE_RESTART = "reload";
    public static final String BUNDLE_SHOW_CONTROLS = "showControls";
    public static final String BUNDLE_EPG_START_TIME = "epgStartTime";
    public static final String BUNDLE_EPG_END_TIME = "epgEndTime";
    public static final String BUNDLE_EPG_INDEX = "showControls";
    public static final String BUNDLE_MANAGE_CONNECTIONS = "manageConnections";
    public static final String BUNDLE_EPG_PIXELS_PER_MINUTE = "pixelsPerMinute";
    public static final String BUNDLE_EPG_DISPLAY_WIDTH = "displayWidth";
    public static final String BUNDLE_ACTION = "action";
    public static final String BUNDLE_NOTIFICATION_MSG = "notificationMsg";

    // Default values for the program guide
    public static final String EPG_DEFAULT_MAX_DAYS = "7";
    public static final String EPG_DEFAULT_HOURS_VISIBLE = "4";

    // Channel sorting order
    public static final int CHANNEL_SORT_DEFAULT = 0;
    public static final int CHANNEL_SORT_BY_NAME = 1;
    public static final int CHANNEL_SORT_BY_NUMBER = 2;

    // Recording sorting order
    public static final int RECORDING_SORT_ASCENDING = 0;
    public static final int RECORDING_SORT_DESCENDING = 1;

    // HTSP connection status
    public static final String ACTION_CONNECTION_STATE_LOST = "action_connection_state_lost";
    public static final String ACTION_CONNECTION_STATE_TIMEOUT = "action_connection_state_timeout";
    public static final String ACTION_CONNECTION_STATE_REFUSED = "action_connection_state_refused";
    public static final String ACTION_CONNECTION_STATE_AUTH = "action_connection_state_auth";
    public static final String ACTION_CONNECTION_STATE_OK = "action_connection_state_ok";
    public static final String ACTION_CONNECTION_STATE_SERVER_DOWN = "action_connection_state_server_down";
    public static final String ACTION_CONNECTION_STATE_UNKNOWN = "action_connection_state_unknown";
    public static final String ACTION_CONNECTION_STATE_NO_NETWORK = "action_connection_state_no_network";
    public static final String ACTION_CONNECTION_STATE_NO_CONNECTION = "action_connection_state_no_connection";

    // HTSP actions that indicate that the server has sent something
    public static final String ACTION_SUBSCRIPTION_ADD = "SUBSCRIPTION_ADD";
    public static final String ACTION_SUBSCRIPTION_DELETE = "SUBSCRIPTION_DELETE";
    public static final String ACTION_SUBSCRIPTION_UPDATE = "SUBSCRIPTION_UPDATE";
    public static final String ACTION_PLAYBACK_PACKET = "PLAYBACK_PACKET";
    public static final String ACTION_LOADING = "LOADING";
    public static final String ACTION_TICKET_ADD = "TICKET_ADD";
    public static final String ACTION_DISC_SPACE = "DISC_SPACE";
    public static final String ACTION_SYSTEM_TIME = "SYSTEM_TIME";
    public static final String ACTION_SHOW_MESSAGE = "SHOW_MESSAGE";

    // HTSP service actions that are called from the client to the server
    public static final String ACTION_CONNECT = "CONNECT";
    public static final String ACTION_DISCONNECT = "DISCONNECT";
    // The default names for the playback and recording profiles
    public static final String PROG_PROFILE_DEFAULT = "htsp";
    public static final String CAST_PROFILE_DEFAULT = "webtv-vp8-vorbis-webm";
    public static final String REC_PROFILE_DEFAULT = "(Default Profile)";

    // Identifier that can be passed to methods that support tags to differentiate the caller
    public static final String TAG_CHANNEL_ICON = "tag_channel_icon";
    public static final String TAG_PROGRAM_GUIDE = "tag_program_guide";

    // Minimum API version to enable or disable certain functionalities. The
    // features shall only be enabled if supported by the server. 
    public static final int MIN_API_VERSION_PROFILES = 16;
    public static final int MIN_API_VERSION_TIMER_RECORDINGS = 18;
    public static final int MIN_API_VERSION_SERIES_RECORDINGS = 13;
    public static final int MIN_API_VERSION_UPDATE_TIMER_RECORDINGS = 25;
    public static final int MIN_API_VERSION_UPDATE_SERIES_RECORDINGS = 25;

    // Minimum API versions to enable or disable certain fields in the HTSP API
    // methods. These fields shall only be visible and used if supported.
    public static final int MIN_API_VERSION_REC_FIELD_ENABLED = 19;
    public static final int MIN_API_VERSION_REC_FIELD_DIRECTORY = 19;
    public static final int MIN_API_VERSION_REC_FIELD_DUPDETECT = 20;

    public static final int MIN_API_VERSION_DVR_FIELD_ENABLED = 23;

    // These constants are required to check if the server contains certain bug fixes
    // Allow selecting 'All channels' for a series recording. (http://tvheadend.org/issues/2892)
    public static final int MIN_API_VERSION_REC_ALL_CHANNELS = 21;
    // Allow editing the title of a recording. (http://tvheadend.org/issues/2893)
    public static final int MIN_API_VERSION_REC_FIELD_TITLE = 21;
    public static final int MIN_API_VERSION_REC_FIELD_SUBTITLE = 21;
    public static final int MIN_API_VERSION_REC_FIELD_DESCRIPTION = 21;

    // Allow updating the channel in a recording
    public static final int MIN_API_VERSION_REC_FIELD_UPDATE_CHANNEL = 22;
}
