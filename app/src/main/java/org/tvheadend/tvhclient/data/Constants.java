package org.tvheadend.tvhclient.data;

public class Constants {

    // Product id for the in-app billing item to unlock the application
    public static final String UNLOCKER = "unlocker";

    // Codes to identify the returning activity
    public static final int RESULT_CODE_START_PLAYER = 102;

    // Strings used to identify the information passed via a bundle
    public static final String BUNDLE_ACTION = "action";
    public static final String BUNDLE_NOTIFICATION_MSG = "notificationMsg";

    // Default values for the program guide
    public static final String EPG_DEFAULT_MAX_DAYS = "7";
    public static final String EPG_DEFAULT_HOURS_VISIBLE = "4";

    // Channel sorting order
    public static final int CHANNEL_SORT_DEFAULT = 0;
    public static final int CHANNEL_SORT_BY_NAME = 1;
    public static final int CHANNEL_SORT_BY_NUMBER = 2;

    public static final String ACTION_TICKET_ADD = "TICKET_ADD";

    public static final String CAST_PROFILE_DEFAULT = "webtv-vp8-vorbis-webm";

}
