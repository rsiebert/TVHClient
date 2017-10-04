package org.tvheadend.tvhclient.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class DataContract {

    // The authority of the tvhclient provider.
    public static final String AUTHORITY = "org.tvheadend.tvhclient.provider";
    // The content URI for the top-level tvhclient authority.
    private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Constants for the connections table of the tvhclient provider.
     */
    public static final class Connections {
        // The database table name
        public static final String TABLE = "connections";
        // The content URI for this table.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(DataContract.CONTENT_URI, "connections");
        // The mime type of a directory of items.
        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/connections";
        // The mime type of a single item.
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/connections";

        // Database column names
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
        public static final String TIME = "time";                           // s64   required   UNIX time.
        public static final String GMT_OFFSET = "gmt_offset";               // s32   optional   Minutes east of GMT.
        public static final String FREE_DISC_SPACE = "free_disc_space";     // s64   required   Bytes available.
        public static final String TOTAL_DISC_SPACE = "total_disc_space";   // s64   required   Total capacity.
        public static final String HTSP_VERSION = "htsp_version";           // u32   required   The server supports all versions of the protocol up to and including this number.
        public static final String SERVER_NAME = "server_name";             // str   required   Server software name.
        public static final String SERVER_VERSION = "server_version";       // str   required   Server software version
        public static final String WEB_ROOT = "web_root";                   // str   optional   Server HTTP webroot (Added in version 8) Note: any access to TVH webserver should include this at start of URL path

        // A projection of all columns in the items table.
        public static final String[] PROJECTION_ALL = {
                ID, NAME, ADDRESS, PORT, USERNAME, PASSWORD, SELECTED, CHANNEL_TAG, STREAMING_PORT,
                WOL_ADDRESS, WOL_PORT, WOL_BROADCAST, PLAY_PROFILE_ID, REC_PROFILE_ID, CAST_PROFILE_ID,
                TIME, GMT_OFFSET, FREE_DISC_SPACE, TOTAL_DISC_SPACE,
                HTSP_VERSION, SERVER_NAME, SERVER_VERSION, WEB_ROOT
        };
        // The default sort order for queries
        static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }

    /**
     * Constants for the profiles table of the tvhclient provider.
     */
    public static final class Profiles {
        // The database table name
        public static final String TABLE = "profiles";
        // The content URI for this table.
        static final Uri CONTENT_URI = Uri.withAppendedPath(DataContract.CONTENT_URI, "profiles");
        // The mime type of a directory of items.
        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/profiles";
        // The mime type of a single item.
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/profiles";

        // Database column names
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
        static final String[] PROJECTION_ALL = {
                ID, ENABLED, UUID, NAME, CONTAINER, TRANSCODE,
                RESOLUTION, AUDIO_CODEC, VIDEO_CODEC, SUBTITLE_CODEC,
        };

        // The default sort order for queries
        static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }

    /**
     * Constants for the channels table of the tvhclient provider.
     */
    public static final class Channels {
        // The database table name
        public static final String TABLE = "channels";
        // The content URI for this table.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(DataContract.CONTENT_URI, "channels");
        public static final Uri CONTENT_URI_ICON = Uri.withAppendedPath(DataContract.CONTENT_URI, "channel_icons");
        // The mime type of a directory of items.
        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/channels";
        // The mime type of a single item.
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/channels";

        // Database column names
        public static final String ID = BaseColumns._ID;                // u32 required   ID of channel
        public static final String NUMBER = "channelNumber";            // u32 required   Channel number, 0 means unconfigured.
        public static final String NUMBER_MINOR = "channelNumberMinor"; // u32 optional   Minor channel number (Added in version 13).
        public static final String NAME = "channelName";                // str required   Name of channel.
        public static final String ICON = "channelIcon";                // str optional   URL to an icon representative for the channel
        public static final String EVENT_ID = "eventId";                // u32 optional   ID of the current event on this channel.
        public static final String NEXT_EVENT_ID = "nextEventId";       // u32 optional   ID of the next event on the channel.
        // TODO this relation requires a channel id to tag id table?
        public static final String TAGS = "tags";                       // u32[] optional   Tags this channel is mapped to.
        // TODO this relation requires a channel id to services table?
        public static final String SERVICES = "services";               // msg[] optional   List of available services (Added in version 5)

        // A projection of all columns in the items table.
        public static final String[] PROJECTION_ALL = {
                ID, NUMBER, NUMBER_MINOR, NAME, ICON, EVENT_ID, NEXT_EVENT_ID
        };

        // The default sort order for queries
        static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }

    /**
     * Constants for the tags table of the tvhclient provider.
     */
    public static final class Tags {
        // The database table name
        public static final String TABLE = "tags";
        // The content URI for this table.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(DataContract.CONTENT_URI, "tags");
        // The mime type of a directory of items.
        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/tags";
        // The mime type of a single item.
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/tags";

        public static final String ID = BaseColumns._ID;            // u32   required   ID of tag.
        public static final String NAME = "tagName";                // str   required   Name of tag.
        public static final String INDEX = "tagIndex";              // u32   optional   Index value for sorting (default by from min to max) (Added in version 18).
        public static final String ICON = "tagIcon";                // str   optional   URL to an icon representative for the channel.
        public static final String TITLED_ICON = "tagTitledIcon";   // u32   optional   Icon includes a title
        // TODO this relation requires a channel id to tag id table?
        public static final String MEMBERS = "members";             // u32[] optional   Channel IDs of those that belong to the tag

        // A projection of all columns in the items table.
        static final String[] PROJECTION_ALL = {
                ID, NAME, INDEX, ICON, TITLED_ICON
        };

        // The default sort order for queries
        static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }

    /**
     * Constants for the programs table of the tvhclient provider.
     */
    public static final class Programs {
        // The database table name
        public static final String TABLE = "programs";
        // The content URI for this table.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(DataContract.CONTENT_URI, "programs");
        // The mime type of a directory of items.
        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/programs";
        // The mime type of a single item.
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/programs";

        public static final String ID = BaseColumns._ID;                // u32   required   Event ID
        public static final String CHANNEL_ID = "channelId";            // u32   required   The channel this event is related to.
        public static final String START = "start";                     // u64   required   Start time of event, UNIX time.
        public static final String STOP = "stop";                       // u64   required   Ending time of event, UNIX time.
        public static final String TITLE = "title";                     // str   optional   Title of event.
        public static final String SUMMARY = "summary";                 // str   optional   Short description of the event (Added in version 6).
        public static final String DESCRIPTION = "description";         // str   optional   Long description of the event.
        public static final String SERIES_LINK_ID = "serieslinkId";     // u32   optional   Series Link ID (Added in version 6).
        public static final String EPISODE_ID = "episodeId";            // u32   optional   Episode ID (Added in version 6).
        public static final String SEASON_ID = "seasonId";              // u32   optional   Season ID (Added in version 6).
        public static final String BRAND_ID = "brandId";                // u32   optional   Brand ID (Added in version 6).
        public static final String TYPE_OF_CONTENT = "contentType";     // u32   optional   DVB content code (Added in version 4, Modified in version 6*).
        public static final String AGE_RATING = "ageRating";            // u32   optional   Minimum age rating (Added in version 6).
        public static final String STAR_RATING = "starRating";          // u32   optional   Star rating (1-5) (Added in version 6).
        public static final String FIRST_AIRED = "firstAired";          // s64   optional   Original broadcast time, UNIX time (Added in version 6).
        public static final String SEASON_NUMBER = "seasonNumber";      // u32   optional   Season number (Added in version 6).
        public static final String SEASON_COUNT = "seasonCount";        // u32   optional   Show season count (Added in version 6).
        public static final String EPISODE_NUMBER = "episodeNumber";    // u32   optional   Episode number (Added in version 6).
        public static final String EPISODE_COUNT = "episodeCount";      // u32   optional   Season episode count (Added in version 6).
        public static final String PART_NUMBER = "partNumber";          // u32   optional   Multi-part episode part number (Added in version 6).
        public static final String PART_COUNT = "partCount";            // u32   optional   Multi-part episode part count (Added in version 6).
        public static final String EPISODE_ON_SCREEN = "episodeOnscreen"; // str   optional   Textual representation of episode number (Added in version 6).
        public static final String IMAGE = "image";                     // str   optional   URL to a still capture from the episode (Added in version 6).
        public static final String DVR_ID = "dvrId";                    // u32   optional   ID of a recording (Added in version 5).
        public static final String NEXT_EVENT_ID = "nextEventId";       // u32   optional   ID of next event on the same channel.

        // A projection of all columns in the items table.
        static final String[] PROJECTION_ALL = {
                ID, CHANNEL_ID, START, STOP, TITLE, SUMMARY, DESCRIPTION,
                SERIES_LINK_ID, EPISODE_ID, SEASON_ID, BRAND_ID, TYPE_OF_CONTENT, AGE_RATING, STAR_RATING,
                FIRST_AIRED, SEASON_NUMBER, SEASON_COUNT, EPISODE_NUMBER, EPISODE_COUNT, PART_NUMBER,
                PART_COUNT, EPISODE_ON_SCREEN, IMAGE, DVR_ID, NEXT_EVENT_ID
        };

        // The default sort order for queries
        static final String SORT_ORDER_DEFAULT = CHANNEL_ID + " ASC, " + START + " ASC";
    }

    /**
     * Constants for the recordings table of the tvhclient provider.
     */
    public static final class Recordings {
        // The database table name
        public static final String TABLE = "recordings";
        // The content URI for this table.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(DataContract.CONTENT_URI, "recordings");
        // The mime type of a directory of items.
        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/recordings";
        // The mime type of a single item.
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/recordings";

        public static final String ID = BaseColumns._ID;            // u32   required   ID of dvrEntry.
        public static final String CHANNEL = "channel";             // u32   optional   Channel of dvrEntry.
        public static final String START = "start";                 // s64   required   Time of when this entry was scheduled to start recording.
        public static final String STOP = "stop";                   // s64   required   Time of when this entry was scheduled to stop recording.
        public static final String START_EXTRA = "startExtra";      // s64   required   Extra start time (pre-time) in minutes (Added in version 13).
        public static final String STOP_EXTRA = "stopExtra";        // s64   required   Extra stop time (post-time) in minutes (Added in version 13).
        public static final String RETENTION = "retention";         // s64   required   DVR Entry retention time in days (Added in version 13).
        public static final String PRIORITY = "priority";           // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set) (Added in version 13).
        public static final String EVENT_ID = "eventId";            // u32   optional   Associated EPG Event ID (Added in version 13).
        public static final String AUTOREC_ID = "autorecId";        // str   optional   Associated Autorec UUID (Added in version 13).
        public static final String TIMEREC_ID = "timerecId";        // str   optional   Associated Timerec UUID (Added in version 18).
        public static final String TYPE_OF_CONTENT = "contentType"; // u32   optional   Content Type (like in the DVB standard) (Added in version 13).
        public static final String TITLE = "title";                 // str   optional   Title of recording
        public static final String SUBTITLE = "subtitle";           // str   optional   Subtitle of recording (Added in version 20).
        public static final String SUMMARY = "summary";             // str   optional   Short description of the recording (Added in version 6).
        public static final String DESCRIPTION = "description";     // str   optional   Long description of the recording.
        public static final String STATE = "state";                 // str   required   Recording state
        public static final String ERROR = "error";                 // str   optional   Plain english error description (e.g. "Aborted by user").
        public static final String OWNER = "owner";                 // str   optional   Name of the entry owner (Added in version 18).
        public static final String CREATOR = "creator";             // str   optional   Name of the entry creator (Added in version 18).
        public static final String SUBSCRIPTION_ERROR = "subscriptionError";    // str   optional   Subscription error string (Added in version 20).
        public static final String STREAM_ERRORS = "streamErrors";  // str   optional   Number of recording errors (Added in version 20).
        public static final String DATA_ERRORS = "dataErrors";      // str   optional   Number of stream data errors (Added in version 20).
        public static final String PATH = "path";                   // str   optional   Recording path for playback.
        // TODO this requires a separate table or just a comma separated string in this table?
        public static final String FILES = "files";                 // msg   optional   All recorded files for playback (Added in version 21).
        public static final String DATA_SIZE = "dataSize";          // s64   optional   Actual file size of the last recordings (Added in version 21).
        public static final String ENABLED = "enabled";             // u32   optional   Enabled flag (Added in version 23).

        // A projection of all columns in the items table.
        public static final String[] PROJECTION_ALL = {
                ID, CHANNEL, START, STOP, START_EXTRA, STOP_EXTRA, RETENTION, PRIORITY, EVENT_ID,
                AUTOREC_ID, TIMEREC_ID, TYPE_OF_CONTENT, TITLE, SUBTITLE, SUMMARY, DESCRIPTION,
                STATE, ERROR, OWNER, CREATOR, SUBSCRIPTION_ERROR, STREAM_ERRORS, DATA_ERRORS, PATH,
                DATA_SIZE, ENABLED
        };

        public static final String SELECTION_COMPLETED =
                ERROR + " IS NULL AND (" +
                        DataContract.Recordings.STATE + "=? OR " +
                        DataContract.Recordings.STATE + "=?)";

        public static final String SELECTION_SCHEDULED =
                DataContract.Recordings.ERROR + " IS NULL AND ("
                        + DataContract.Recordings.STATE + "=? OR "
                        + DataContract.Recordings.STATE + "=?)";

        // A recording is failed if its either failed, missed or aborted
        // failed: error is set AND (state == missed or state == invalid)
        // missed: no error and state == missed
        // aborted: error == "Aborted by user" and state == "completed"
        public static final String SELECTION_FAILED =
                "(" + DataContract.Recordings.ERROR + " IS NOT NULL AND "
                        + "(" + DataContract.Recordings.STATE + "=? OR " + DataContract.Recordings.STATE + "=?)) "
                        + " OR (" + DataContract.Recordings.ERROR + " IS NULL AND " + DataContract.Recordings.STATE + "=?)"
                        + " OR (" + DataContract.Recordings.ERROR + "=? AND " + DataContract.Recordings.STATE + "=?)";

        public static final String SELECTION_REMOVED =
                DataContract.Recordings.ERROR + "=? AND " + DataContract.Recordings.STATE + "=?";

        public static final String[] SELECTION_ARGS_COMPLETED = {"completed"};
        public static final String[] SELECTION_ARGS_SCHEDULED = {"recording", "scheduled"};
        public static final String[] SELECTION_ARGS_FAILED = {"missed", "invalid", "missed", "Aborted by user", "completed"};
        public static final String[] SELECTION_ARGS_REMOVED = {"File missing", "completed"};

        // The default sort order for queries
        static final String SORT_ORDER_DEFAULT = CHANNEL + " ASC, " + START + " ASC";
    }

    /**
     * Constants for the series recordings table of the tvhclient provider.
     */
    public static final class SeriesRecordings {
        // The database table name
        public static final String TABLE = "series_recordings";
        // The content URI for this table.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(DataContract.CONTENT_URI, "series_recordings");
        // The mime type of a directory of items.
        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/series_recordings";
        // The mime type of a single item.
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/series_recordings";

        public static final String ID = BaseColumns._ID;            // str   required   ID (string!) of dvrAutorecEntry.
        public static final String ENABLED = "enabled";             // u32   required   If autorec entry is enabled (activated).
        public static final String NAME = "name";                   // str   required   Name of the autorec entry (Added in version 18).
        public static final String MIN_DURATION = "minDuration";    // u32   required   Minimal duration in seconds (0 = Any).
        public static final String MAX_DURATION = "maxDuration";    // u32   required   Maximal duration in seconds (0 = Any).
        public static final String RETENTION = "retention";         // u32   required   Retention time (in days).
        public static final String DAYS_OF_WEEK = "daysOfWeek";     // u32   required   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
        public static final String PRIORITY = "priority";           // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
        public static final String APPROX_TIME = "approxTime";      // u32   required   Minutes from midnight (up to 24*60).
        public static final String START = "start";                 // s32   required   Exact start time (minutes from midnight) (Added in version 18).
        public static final String START_WINDOW = "startWindow";    // s32   required   Exact stop time (minutes from midnight) (Added in version 18).
        public static final String START_EXTRA = "startExtra";      // s64   required   Extra start minutes (pre-time).
        public static final String STOP_EXTRA = "stopExtra";        // s64   required   Extra stop minutes (post-time).
        public static final String TITLE = "title";                 // str   optional   Title.
        public static final String FULLTEXT = "fulltext";           // u32   optional   Fulltext flag (Added in version 20).
        public static final String DIRECTORY = "directory";         // str   optional   Forced directory name (Added in version 19).
        public static final String CHANNEL = "channel";             // u32   optional   Channel ID.
        public static final String OWNER = "owner";                 // str   optional   Owner of this autorec entry (Added in version 18).
        public static final String CREATOR = "creator";             // str   optional   Creator of this autorec entry (Added in version 18).
        public static final String DUP_DETECT = "dupDetect";        // u32   optional   Duplicate detection (see addAutorecEntry) (Added in version 20).

        // A projection of all columns in the items table.
        static final String[] PROJECTION_ALL = {
                ID, ENABLED, NAME, MIN_DURATION, MAX_DURATION, RETENTION, DAYS_OF_WEEK, PRIORITY,
                APPROX_TIME, START, START_WINDOW, START_EXTRA, STOP_EXTRA, TITLE, FULLTEXT,
                DIRECTORY, CHANNEL, OWNER, CREATOR, DUP_DETECT
        };

        // The default sort order for queries
        static final String SORT_ORDER_DEFAULT = NAME + " ASC, " + START + " ASC";
    }

    /**
     * Constants for the timer recordings table of the tvhclient provider.
     */
    public static final class TimerRecordings {
        // The database table name
        public static final String TABLE = "timer_recordings";
        // The content URI for this table.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(DataContract.CONTENT_URI, "timer_recordings");
        // The mime type of a directory of items.
        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/timer_recordings";
        // The mime type of a single item.
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/timer_recordings";

        public static final String ID = BaseColumns._ID;        // str   required   ID (string!) of timerecEntry.
        public static final String TITLE = "title";             // str   required   Title for the recordings.
        public static final String DIRECTORY = "directory";     // str   optional   Forced directory name (Added in version 19).
        public static final String ENABLED = "enabled";         // u32   required   Title for the recordings.
        public static final String NAME = "name";               // str   required   Name for this timerec entry.
        public static final String CONFIG_NAME = "configName";  // str   required   DVR Configuration Name / UUID.
        public static final String CHANNEL = "channel";         // u32   required   Channel ID.
        public static final String DAYS_OF_WEEK = "daysOfWeek"; // u32   optional   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
        public static final String PRIORITY = "priority";       // u32   optional   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
        public static final String START = "start";             // u32   required   Minutes from midnight (up to 24*60) for the start of the time window (including)
        public static final String STOP = "stop";               // u32   required   Minutes from modnight (up to 24*60) for the end of the time window (including, cross-noon allowed)
        public static final String RETENTION = "retention";     // u32   optional   Retention in days.
        public static final String OWNER = "owner";             // str   optional   Owner of this timerec entry.
        public static final String CREATOR = "creator";         // str   optional   Creator of this timerec entry.

        // A projection of all columns in the items table.
        static final String[] PROJECTION_ALL = {
                ID, TITLE, DIRECTORY, ENABLED, NAME, CONFIG_NAME, CHANNEL, DAYS_OF_WEEK,
                PRIORITY, START, STOP, RETENTION, OWNER, CREATOR
        };

        // The default sort order for queries
        static final String SORT_ORDER_DEFAULT = NAME + " ASC, " + START + " ASC";
    }
}
