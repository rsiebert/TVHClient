package org.tvheadend.tvhclient.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class DataContract {

    // The authority of the tvhclient provider.
    static final String AUTHORITY = "org.tvheadend.tvhclient.provider";
    // The content URI for the top-level tvhclient authority.
    static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    // A selection clause for ID based queries.
    static final String SELECTION_ID_BASED = BaseColumns._ID + " = ? ";

    /**
     * Constants for the connections table of the tvhclient provider.
     */
    public static final class Connections {
        // The database table name
        public static final String TABLE = "connections";
        // The content URI for this table.
        public static final Uri CONTENT_URI =  Uri.withAppendedPath(DataContract.CONTENT_URI, "connections");
        // The mime type of a directory of items.
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/connections";
        // The mime type of a single item.
        public static final String CONTENT_CONNECTION_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/connections";

        // Database column names for the connection table
        public static final String ID = BaseColumns._ID;
        public static final String NAME = "name";
        public static final String ADDRESS = "address";
        public static final String PORT = "port";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String SELECTED = "selected";
        public static final String CHANNEL_TAG = "channel_tag";
        public static final String STREAMING_PORT = "streaming_port";
        public static final String WOL_ADDRESS = "wol_address";
        public static final String WOL_PORT = "wol_port";
        public static final String WOL_BROADCAST = "wol_broadcast";
        public static final String PLAY_PROFILE_ID = "playback_profile_id";
        public static final String REC_PROFILE_ID = "recording_profile_id";
        public static final String CAST_PROFILE_ID = "cast_profile_id";

        // A projection of all columns in the items table.
        public static final String[] PROJECTION_ALL = {
                ID, NAME, ADDRESS, PORT, USERNAME, PASSWORD, SELECTED, CHANNEL_TAG, STREAMING_PORT,
                WOL_ADDRESS, WOL_PORT, WOL_BROADCAST, PLAY_PROFILE_ID, REC_PROFILE_ID, CAST_PROFILE_ID
        };
        // The default sort order for queries containing NAME fields.
        public static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }

    /**
     * Constants for the profiles table of the tvhclient provider.
     */
    public static final class Profiles {
        // The database table name
        public static final String TABLE = "profiles";
        // The content URI for this table.
        public static final Uri CONTENT_URI =  Uri.withAppendedPath(DataContract.CONTENT_URI, "profiles");
        // The mime type of a directory of items.
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/profiles";
        // The mime type of a single item.
        public static final String CONTENT_PROFILE_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/profiles";

        // Database column names for the profile table
        public static final String ID = BaseColumns._ID;
        public static final String ENABLED = "profile_enabled"; // use the new profile if htsp version > X
        public static final String UUID = "profile_uuid";       // The uuid of the profile
        public static final String NAME = "profile_name";       // The name of the profile
        public static final String CONTAINER = "container";
        public static final String TRANSCODE = "transcode";
        public static final String RESOLUTION = "resolution";
        public static final String VIDEO_CODEC = "video_codec";
        public static final String AUDIO_CODEC = "acode_codec"; // TODO rename this column
        public static final String SUBTITLE_CODEC = "subtitle_codec";

        // A projection of all columns in the items table.
        public static final String[] PROJECTION_ALL = {
                ID, ENABLED, UUID, NAME, CONTAINER, TRANSCODE,
                RESOLUTION, AUDIO_CODEC, VIDEO_CODEC, SUBTITLE_CODEC,
        };

        // The default sort order for queries containing NAME fields.
        public static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }

    /**
     * Constants for the profiles table of the tvhclient provider.
     */
    public static final class Channels {
        // The database table name
        public static final String TABLE = "channels";
        // The content URI for this table.
        public static final Uri CONTENT_URI =  Uri.withAppendedPath(DataContract.CONTENT_URI, "channels");
        // The mime type of a directory of items.
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/channels";
        // The mime type of a single item.
        public static final String CONTENT_CHANNEL_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/channels";

        // Database column names for the profile table
        public static final String ID = BaseColumns._ID;
        public static final String TITLE = "title";
        public static final String NAME = "name";
        public static final String UUID = "uuid";

        // A projection of all columns in the items table.
        public static final String[] PROJECTION_ALL = {
                ID, TITLE, NAME, UUID
        };

        // The default sort order for queries containing NAME fields.
        public static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }
}
