package org.tvheadend.tvhclient.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class DataContract {

    // The authority of the tvhclient provider.
    static final String AUTHORITY = "org.tvheadend.tvhclient.provider";
    // The content URI for the top-level tvhclient authority.
    static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

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
        public static final String[] PROJECTION_ALL = {
                ID, ENABLED, UUID, NAME, CONTAINER, TRANSCODE,
                RESOLUTION, AUDIO_CODEC, VIDEO_CODEC, SUBTITLE_CODEC,
        };

        // The default sort order for queries containing NAME fields.
        public static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }

    /**
     * Constants for the channels table of the tvhclient provider.
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

        // Database column names
        public static final String ID = BaseColumns._ID;                // u32 ID of channel
        public static final String NUMBER = "channelNumber";            // u32 Channel number, 0 means unconfigured.
        public static final String NUMBER_MINOR = "channelNumberMinor"; // u32 Minor channel number (Added in version 13).
        public static final String NAME = "channelName";                // str Name of channel.
        public static final String ICON = "channelIcon";                // str URL to an icon representative for the channel
        public static final String EVENT_ID = "eventId";                // u32 ID of the current event on this channel.
        public static final String NEXT_EVENT_ID = "nextEventId";       // u32 ID of the next event on the channel.
        // TODO this relation requires a channel id to tag id table?
        public static final String TAGS = "tags";                       // u32[] Tags this channel is mapped to.
        // TODO this relation requires a channel id to services table?
        public static final String SERVICES = "services";               // msg[] List of available services (Added in version 5)

        // A projection of all columns in the items table.
        public static final String[] PROJECTION_ALL = {
                ID, NUMBER, NUMBER_MINOR, NAME, ICON, EVENT_ID, NEXT_EVENT_ID
        };

        // The default sort order for queries containing NAME fields.
        public static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }

    /**
     * Constants for the tags table of the tvhclient provider.
     */
    public static final class Tags {
        // The database table name
        public static final String TABLE = "tags";
        // The content URI for this table.
        public static final Uri CONTENT_URI =  Uri.withAppendedPath(DataContract.CONTENT_URI, "tags");
        // The mime type of a directory of items.
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/tags";
        // The mime type of a single item.
        public static final String CONTENT_TAG_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/tags";

        public static final String ID = BaseColumns._ID;            // u32   required   ID of tag.
        public static final String NAME = "tagName";                // str   required   Name of tag.
        public static final String INDEX = "tagIndex";              // u32   optional   Index value for sorting (default by from min to max) (Added in version 18).
        public static final String ICON = "tagIcon";                // str   optional   URL to an icon representative for the channel.
        public static final String TITLED_ICON = "tagTitledIcon";   // u32   optional   Icon includes a title" +
        // TODO this relation requires a channel id to tag id table?
        public static final String MEMBERS = "members";             // u32[] optional   Channel IDs of those that belong to the tag

        // A projection of all columns in the items table.
        public static final String[] PROJECTION_ALL = {
                ID, NAME, INDEX, ICON, TITLED_ICON
        };

        // The default sort order for queries containing NAME fields.
        public static final String SORT_ORDER_DEFAULT = NAME + " ASC";
    }

    /**
     * Constants for the programs table of the tvhclient provider.
     */
    public static final class Programs {
        // The database table name
        public static final String TABLE = "programs";
        // The content URI for this table.
        public static final Uri CONTENT_URI =  Uri.withAppendedPath(DataContract.CONTENT_URI, "programs");
        // The mime type of a directory of items.
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/programs";
        // The mime type of a single item.
        public static final String CONTENT_PROGRAM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/programs";

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
        public static final String TYPE_OF_CONTENT = "contentType";        // u32   optional   DVB content code (Added in version 4, Modified in version 6*).
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
        public static final String[] PROJECTION_ALL = {
                ID, CHANNEL_ID, START, STOP, TITLE, SUMMARY, DESCRIPTION,
                SERIES_LINK_ID, EPISODE_ID, SEASON_ID, BRAND_ID, TYPE_OF_CONTENT, AGE_RATING, STAR_RATING,
                FIRST_AIRED, SEASON_NUMBER, SEASON_COUNT, EPISODE_NUMBER, EPISODE_COUNT, PART_NUMBER,
                PART_COUNT, EPISODE_ON_SCREEN, IMAGE, DVR_ID, NEXT_EVENT_ID
        };

        // The default sort order for queries containing NAME fields.
        public static final String SORT_ORDER_DEFAULT = CHANNEL_ID + " ASC, " + START + " ASC";
    }
}
