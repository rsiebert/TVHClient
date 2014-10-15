package org.tvheadend.tvhclient;

public class Constants {

    // Codes to identify the returning activity
    public static final int RESULT_CODE_SETTINGS = 101;
    public static final int RESULT_CODE_START_PLAYER = 102;

    // Amount of programs of a channel that shall be loaded from the server
    public static final int PREF_PROGRAMS_TO_LOAD = 15;

    // Strings used to identify the information passed via a bundle
    public static final String BUNDLE_SHOWS_ONLY_CHANNELS = "showOnlyChannels";
    public static final String BUNDLE_RECORDING_TYPE = "recordingType";
    public static final String BUNDLE_DUAL_PANE = "isDualPane";
    public static final String BUNDLE_CHANNEL_ID = "channelId";
    public static final String BUNDLE_PROGRAM_ID = "eventId";
    public static final String BUNDLE_RECORDING_ID = "dvrId";
    public static final String BUNDLE_RECONNECT = "reconnect";
    public static final String BUNDLE_RESTART = "reload";
    public static final String BUNDLE_COUNT = "count";
    public static final String BUNDLE_MENU_POSITION = "menuPosition";
    public static final String BUNDLE_SHOW_CONTROLS = "showControls";
    public static final String BUNDLE_EPG_START_TIME = "epgStartTime";
    public static final String BUNDLE_EPG_END_TIME = "epgEndTime";
    public static final String BUNDLE_EPG_HOURS_TO_SHOW = "epgHoursToShow";
    public static final String BUNDLE_EPG_INDEX = "showControls";
    public static final String BUNDLE_CONNECTION_STATUS = "connectionStatus";
    public static final String BUNDLE_CONNECTION_ID = "connectionId";
    public static final String BUNDLE_MANAGE_CONNECTIONS = "manageConnections";
    public static final String BUNDLE_SETTINGS_MODE = "settingsMode";

    // Differentiates the different types of recordings
    public static final int RECORDING_TYPE_COMPLETED = 1;
    public static final int RECORDING_TYPE_SCHEDULED = 2;
    public static final int RECORDING_TYPE_SERIES = 3;
    public static final int RECORDING_TYPE_FAILED = 4;

    // Strings that determine the navigation drawer menu position and the list
    // positions so it can be reselected after an orientation change.
    public static final String MENU_STACK = "menu_stack";
    public static final String MENU_POSITION = "menu_position";
    public static final String CHANNEL_LIST_POSITION = "channel_list_position";
    public static final String PROGRAM_LIST_POSITION = "program_list_position";
    public static final String COMPLETED_RECORDING_LIST_POSITION = "completed_recording_list_position";
    public static final String SCHEDULED_RECORDING_LIST_POSITION = "scheduled_recording_list_position";
    public static final String SERIES_RECORDING_LIST_POSITION = "series_recording_list_position";
    public static final String FAILED_RECORDING_LIST_POSITION = "failed_recording_list_position";
    public static final String LAST_CONNECTION_STATE = "last_connection_state";

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
    
    // HTSP actions
    public static final String ACTION_CHANNEL_ADD = "CHANNEL_ADD";
    public static final String ACTION_CHANNEL_DELETE = "CHANNEL_DELETE";
    public static final String ACTION_CHANNEL_UPDATE = "CHANNEL_UPDATE";
    public static final String ACTION_TAG_ADD = "TAG_ADD";
    public static final String ACTION_TAG_DELETE = "TAG_DELETE";
    public static final String ACTION_TAG_UPDATE = "TAG_UPDATE";
    public static final String ACTION_DVR_ADD = "DVR_ADD";
    public static final String ACTION_DVR_DELETE = "DVR_DELETE";
    public static final String ACTION_DVR_UPDATE = "DVR_UPDATE";
    public static final String ACTION_DVR_CANCEL = "DVR_CANCEL";
    public static final String ACTION_PROGRAM_ADD = "PROGRAM_ADD";
    public static final String ACTION_PROGRAM_DELETE = "PROGRAM_DELETE";
    public static final String ACTION_PROGRAM_UPDATE = "PROGRAM_UPDATE";
    public static final String ACTION_SUBSCRIPTION_ADD = "SUBSCRIPTION_ADD";
    public static final String ACTION_SUBSCRIPTION_DELETE = "SUBSCRIPTION_DELETE";
    public static final String ACTION_SUBSCRIPTION_UPDATE = "SUBSCRIPTION_UPDATE";
    public static final String ACTION_SIGNAL_STATUS = "SIGNAL_STATUS";
    public static final String ACTION_PLAYBACK_PACKET = "PLAYBACK_PACKET";
    public static final String ACTION_LOADING = "LOADING";
    public static final String ACTION_TICKET_ADD = "TICKET";
    public static final String ACTION_ERROR = "ERROR";
    public static final String ACTION_DISC_SPACE = "DISC_SPACE";

    // Additional HTSP service actions
    public static final String ACTION_CONNECT = "CONNECT";
    public static final String ACTION_DISCONNECT = "DISCONNECT";
    public static final String ACTION_EPG_QUERY = "EPG_QUERY";
    public static final String ACTION_GET_EVENT = "GET_EVENT";
    public static final String ACTION_GET_EVENTS = "GET_EVENTS";
    public static final String ACTION_SUBSCRIBE = "SUBSCRIBE";
    public static final String ACTION_UNSUBSCRIBE = "UNSUBSCRIBE";
    public static final String ACTION_FEEDBACK = "FEEDBACK";
    public static final String ACTION_GET_TICKET = "GET_TICKET";
    public static final String ACTION_GET_DISC_STATUS = "GET_DISC_STATUS";
}
