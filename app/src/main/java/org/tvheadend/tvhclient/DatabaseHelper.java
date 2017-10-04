package org.tvheadend.tvhclient;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.tvheadend.tvhclient.data.DataContract;

public class DatabaseHelper extends SQLiteOpenHelper {
    private final static String TAG = DatabaseHelper.class.getSimpleName();

    // Database version and name declarations
    private static final int DATABASE_VERSION = 13;
    private static final String DATABASE_NAME = "tvhclient";

    private static DatabaseHelper mInstance = null;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (mInstance == null)
            mInstance = new DatabaseHelper(context);
        return mInstance;
    }

    /**
     * Called when the database is created for the very first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Connections.TABLE + " ("
                + DataContract.Connections.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DataContract.Connections.NAME + " TEXT NOT NULL,"
                + DataContract.Connections.ADDRESS + " TEXT NOT NULL, "
                + DataContract.Connections.PORT + " INT DEFAULT 9982, "
                + DataContract.Connections.USERNAME + " TEXT NULL, "
                + DataContract.Connections.PASSWORD + " TEXT NULL, "
                + DataContract.Connections.SELECTED + " INT NOT NULL, "
                + DataContract.Connections.CHANNEL_TAG + " INT DEFAULT 0, "
                + DataContract.Connections.STREAMING_PORT + " INT DEFAULT 9981, "
                + DataContract.Connections.WOL_ADDRESS + " TEXT NULL, "
                + DataContract.Connections.WOL_PORT + " INT DEFAULT 9, "
                + DataContract.Connections.WOL_BROADCAST + " INT DEFAULT 0, "
                + DataContract.Connections.PLAY_PROFILE_ID + " INT DEFAULT 0, "
                + DataContract.Connections.REC_PROFILE_ID + " INT DEFAULT 0,"
                + DataContract.Connections.CAST_PROFILE_ID + " INT DEFAULT 0);";
        db.execSQL(query);

        query = "CREATE TABLE IF NOT EXISTS " + DataContract.Profiles.TABLE + " ("
                + DataContract.Profiles.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DataContract.Profiles.ENABLED + " INT DEFAULT 0, "
                + DataContract.Profiles.UUID + " TEXT NULL, "
                + DataContract.Profiles.NAME + " TEXT NULL, "
                + DataContract.Profiles.CONTAINER + " TEXT NULL, "
                + DataContract.Profiles.TRANSCODE + " INT DEFAULT 0, "
                + DataContract.Profiles.RESOLUTION + " TEXT NULL, "
                + DataContract.Profiles.AUDIO_CODEC + " TEXT NULL, "
                + DataContract.Profiles.VIDEO_CODEC + " TEXT NULL, "
                + DataContract.Profiles.SUBTITLE_CODEC + " TEXT NULL);";
        db.execSQL(query);

        db.execSQL(getChannelsQuery());
        db.execSQL(getTagsQuery());
        db.execSQL(getProgramsQuery());
        db.execSQL(getRecordingsQuery());
        db.execSQL(getSeriesRecordingsQuery());
        db.execSQL(getTimerRecordingsQuery());
        db.execSQL(getServerInfoQuery());
    }

    /**
     * Called when the database version has changed.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade() called with: db = [" + db + "], oldVersion = [" + oldVersion + "], newVersion = [" + newVersion + "]");

        if (oldVersion < newVersion && newVersion == 2) {
            // Add the channel tag column in database version 2 
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.CHANNEL_TAG
                    + " INT DEFAULT 0;");
        }
        if (oldVersion < newVersion && newVersion == 3) {
            // Add the streaming port column in database version 3 
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.STREAMING_PORT
                    + " INT DEFAULT 9981;");
        }
        if (oldVersion < newVersion && newVersion == 4) {
            // Add the required columns for WOL. sqlite does only support single
            // alterations of the table
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.WOL_ADDRESS
                    + " TEXT NULL;");
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.WOL_PORT
                    + " INT DEFAULT 9;");
        }
        if (oldVersion < newVersion && newVersion == 5) {
            // Add the broadcast column for WOL.
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.WOL_BROADCAST
                    + " INT DEFAULT 0;");
        }
        if (oldVersion < newVersion && newVersion == 6) {
            // Add the id columns for the profiles.
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.PLAY_PROFILE_ID
                    + " INT DEFAULT 0;");
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.REC_PROFILE_ID
                    + " INT DEFAULT 0;");
            // Add the new profile table
            final String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Profiles.TABLE + " ("
                    + DataContract.Profiles.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DataContract.Profiles.ENABLED + " INT DEFAULT 0, "
                    + DataContract.Profiles.UUID + " TEXT NULL, "
                    + DataContract.Profiles.CONTAINER + " TEXT NULL, "
                    + DataContract.Profiles.TRANSCODE + " INT DEFAULT 0, "
                    + DataContract.Profiles.RESOLUTION + " TEXT NULL, "
                    + DataContract.Profiles.AUDIO_CODEC + " TEXT NULL, "
                    + DataContract.Profiles.VIDEO_CODEC + " TEXT NULL, "
                    + DataContract.Profiles.SUBTITLE_CODEC + " TEXT NULL);";
            db.execSQL(query);
        }
        if (oldVersion < newVersion && newVersion == 7) {
            db.execSQL("ALTER TABLE " + DataContract.Profiles.TABLE + " ADD COLUMN " + DataContract.Profiles.NAME
                    + " TEXT NULL;");
        }
        if (oldVersion < newVersion && newVersion == 8) {
            // Add the id columns for the profiles but check if they exist
            Cursor cursor = db.rawQuery("SELECT * FROM " + DataContract.Connections.TABLE, null);
            int colIndex = cursor.getColumnIndex(DataContract.Connections.PLAY_PROFILE_ID);
            if (colIndex < 0) {
                db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN "
                        + DataContract.Connections.PLAY_PROFILE_ID + " INT DEFAULT 0;");
            }

            cursor = db.rawQuery("SELECT * FROM " + DataContract.Connections.TABLE, null);
            colIndex = cursor.getColumnIndex(DataContract.Connections.REC_PROFILE_ID);
            if (colIndex < 0) {
                db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN "
                        + DataContract.Connections.REC_PROFILE_ID + " INT DEFAULT 0;");
            }

            // Add the new profile table
            final String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Profiles.TABLE + " ("
                    + DataContract.Profiles.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DataContract.Profiles.ENABLED + " INT DEFAULT 0, "
                    + DataContract.Profiles.UUID + " TEXT NULL, "
                    + DataContract.Profiles.NAME + " TEXT NULL, "
                    + DataContract.Profiles.CONTAINER + " TEXT NULL, "
                    + DataContract.Profiles.TRANSCODE + " INT DEFAULT 0, "
                    + DataContract.Profiles.RESOLUTION + " TEXT NULL, "
                    + DataContract.Profiles.AUDIO_CODEC + " TEXT NULL, "
                    + DataContract.Profiles.VIDEO_CODEC + " TEXT NULL, "
                    + DataContract.Profiles.SUBTITLE_CODEC + " TEXT NULL);";
            db.execSQL(query);
            cursor.close();
        }
        if (oldVersion < newVersion && newVersion == 9) {
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.CAST_PROFILE_ID
                    + " INT DEFAULT 0;");
        }
        if (oldVersion < newVersion && newVersion == 10) {
            db.execSQL(getChannelsQuery());
            db.execSQL(getTagsQuery());
            db.execSQL(getProgramsQuery());
        }
        if (oldVersion < newVersion && newVersion == 11) {
            db.execSQL(getRecordingsQuery());
        }
        if (oldVersion < newVersion && newVersion == 12) {
            db.execSQL(getSeriesRecordingsQuery());
            db.execSQL(getTimerRecordingsQuery());
        }
        if (oldVersion < newVersion && newVersion == 13) {
            db.execSQL(getServerInfoQuery());

            // TODO get all defined connection names and add new servers with this name

            // TODO drop the name from the connections table

        }
    }

    private String getChannelsQuery() {
        String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Channels.TABLE + " ("
                + DataContract.Channels.ID + " INTEGER PRIMARY KEY,"                // u32 ID of channel
                + DataContract.Channels.NUMBER + " INT DEFAULT 0,"                  // u32 Channel number, 0 means unconfigured.
                + DataContract.Channels.NUMBER_MINOR + " INT DEFAULT 0,"            // u32 Minor channel number (Added in version 13).
                + DataContract.Channels.NAME + " TEXT NULL,"                        // str Name of channel.
                + DataContract.Channels.ICON + " TEXT NULL,"                        // str URL to an icon representative for the channel
                + DataContract.Channels.EVENT_ID + " INT DEFAULT 0,"                // u32 ID of the current event on this channel.
                + DataContract.Channels.NEXT_EVENT_ID + " INT DEFAULT 0);";         // u32 ID of the next event on the channel.
        return query;
    }

    private String getTagsQuery() {
        String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Tags.TABLE + " ("
                + DataContract.Tags.ID + " INTEGER PRIMARY KEY,"                    // u32 ID of tag.
                + DataContract.Tags.NAME + " TEXT NULL,"                            // str Name of tag.
                + DataContract.Tags.INDEX + " INT DEFAULT 0,"                       // u32 Index value for sorting (default by from min to max) (Added in version 18).
                + DataContract.Tags.ICON + " TEXT NULL,"                            // str URL to an icon representative for the channel.
                + DataContract.Tags.TITLED_ICON + " INT DEFAULT 0);";             // u32 Icon includes a title" +
        return query;
    }

    private String getProgramsQuery() {
        String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Programs.TABLE + " ("
                + DataContract.Programs.ID + " INTEGER PRIMARY KEY,"                // u32 Event ID
                + DataContract.Programs.CHANNEL_ID + " INT DEFAULT 0,"              // u32 The channel this event is related to.
                + DataContract.Programs.START + " INT DEFAULT 0,"                   // u64 Start time of event, UNIX time.
                + DataContract.Programs.STOP + " INT DEFAULT 0,"                    // u64 Ending time of event, UNIX time.
                + DataContract.Programs.TITLE + " TEXT NULL,"                       // str Title of event.
                + DataContract.Programs.SUMMARY + " TEXT NULL,"                     // str Short description of the event (Added in version 6).
                + DataContract.Programs.DESCRIPTION + " TEXT NULL,"                 // str Long description of the event.
                + DataContract.Programs.SERIES_LINK_ID + " INT DEFAULT 0,"          // u32 Series Link ID (Added in version 6).
                + DataContract.Programs.EPISODE_ID + " INT DEFAULT 0,"              // u32 Episode ID (Added in version 6).
                + DataContract.Programs.SEASON_ID + " INT DEFAULT 0,"               // u32 Season ID (Added in version 6).
                + DataContract.Programs.BRAND_ID + " INT DEFAULT 0,"                // u32 Brand ID (Added in version 6).
                + DataContract.Programs.TYPE_OF_CONTENT + " INT DEFAULT 0,"         // u32 DVB content code (Added in version 4, Modified in version 6*).
                + DataContract.Programs.AGE_RATING + " INT DEFAULT 0,"              // u32 Minimum age rating (Added in version 6).
                + DataContract.Programs.STAR_RATING + " INT DEFAULT 0,"             // u32 Star rating (1-5) (Added in version 6).
                + DataContract.Programs.FIRST_AIRED + " INT DEFAULT 0,"             // s64 Original broadcast time, UNIX time (Added in version 6).
                + DataContract.Programs.SEASON_NUMBER + " INT DEFAULT 0,"           // u32 Season number (Added in version 6).
                + DataContract.Programs.SEASON_COUNT + " INT DEFAULT 0,"            // u32 Show season count (Added in version 6).
                + DataContract.Programs.EPISODE_NUMBER + " INT DEFAULT 0,"          // u32 Episode number (Added in version 6).
                + DataContract.Programs.EPISODE_COUNT + " INT DEFAULT 0,"           // u32 Season episode count (Added in version 6).
                + DataContract.Programs.PART_NUMBER + " INT DEFAULT 0,"             // u32 Multi-part episode part number (Added in version 6).
                + DataContract.Programs.PART_COUNT + " INT DEFAULT 0,"              // u32 Multi-part episode part count (Added in version 6).
                + DataContract.Programs.EPISODE_ON_SCREEN + " TEXT NULL,"           // str Textual representation of episode number (Added in version 6).
                + DataContract.Programs.IMAGE + " TEXT NULL,"                       // str URL to a still capture from the episode (Added in version 6).
                + DataContract.Programs.DVR_ID + " INT DEFAULT 0,"                  // u32 ID of a recording (Added in version 5).
                + DataContract.Programs.NEXT_EVENT_ID + " INT DEFAULT 0);";         // u32 ID of next event on the same channel.
        return query;
    }

    private String getRecordingsQuery() {
        String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Recordings.TABLE + " ("
                + DataContract.Recordings.ID + " INTEGER PRIMARY KEY,"      // u32   required   ID of dvrEntry.
                + DataContract.Recordings.CHANNEL + " INT DEFAULT 0,"       // u32   optional   Channel of dvrEntry.
                + DataContract.Recordings.START + " INT DEFAULT 0,"         // s64   required   Time of when this entry was scheduled to start recording.
                + DataContract.Recordings.STOP + " INT DEFAULT 0,"          // s64   required   Time of when this entry was scheduled to stop recording.
                + DataContract.Recordings.START_EXTRA + " INT DEFAULT 0,"   // s64   required   Extra start time (pre-time) in minutes (Added in version 13).
                + DataContract.Recordings.STOP_EXTRA + " INT DEFAULT 0,"    // s64   required   Extra stop time (post-time) in minutes (Added in version 13).
                + DataContract.Recordings.RETENTION + " INT DEFAULT 0,"     // s64   required   DVR Entry retention time in days (Added in version 13).
                + DataContract.Recordings.PRIORITY + " INT DEFAULT 0,"      // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set) (Added in version 13).
                + DataContract.Recordings.EVENT_ID + " INT DEFAULT 0,"      // u32   optional   Associated EPG Event ID (Added in version 13).
                + DataContract.Recordings.AUTOREC_ID + " TEXT NULL,"        // str   optional   Associated Autorec UUID (Added in version 13).
                + DataContract.Recordings.TIMEREC_ID + " TEXT NULL,"        // str   optional   Associated Timerec UUID (Added in version 18).
                + DataContract.Recordings.TYPE_OF_CONTENT + " INT DEFAULT 0," // u32   optional   Content Type (like in the DVB standard) (Added in version 13).
                + DataContract.Recordings.TITLE + " TEXT NULL,"             // str   optional   Title of recording
                + DataContract.Recordings.SUBTITLE + " TEXT NULL,"          // str   optional   Subtitle of recording (Added in version 20).
                + DataContract.Recordings.SUMMARY + " TEXT NULL,"           // str   optional   Short description of the recording (Added in version 6).
                + DataContract.Recordings.DESCRIPTION + " TEXT NULL,"       // str   optional   Long description of the recording.
                + DataContract.Recordings.STATE + " TEXT NULL,"             // str   required   Recording state
                + DataContract.Recordings.ERROR + " TEXT NULL,"             // str   optional   Plain english error description (e.g. "Aborted by user").
                + DataContract.Recordings.OWNER + " TEXT NULL,"             // str   optional   Name of the entry owner (Added in version 18).
                + DataContract.Recordings.CREATOR + " TEXT NULL,"           // str   optional   Name of the entry creator (Added in version 18).
                + DataContract.Recordings.SUBSCRIPTION_ERROR + " TEXT NULL," // str   optional   Subscription error string (Added in version 20).
                + DataContract.Recordings.STREAM_ERRORS + " TEXT NULL,"     // str   optional   Number of recording errors (Added in version 20).
                + DataContract.Recordings.DATA_ERRORS + " TEXT NULL,"       // str   optional   Number of stream data errors (Added in version 20).
                + DataContract.Recordings.PATH + " TEXT NULL,"              // str   optional   Recording path for playback.
                + DataContract.Recordings.DATA_SIZE + " INT DEFAULT 0,"     // s64   optional   Actual file size of the last recordings (Added in version 21).
                + DataContract.Recordings.ENABLED + " INT DEFAULT 0);";     // u32   optional   Enabled flag (Added in version 23).
        return query;
    }

    private String getSeriesRecordingsQuery() {
        String query = "CREATE TABLE IF NOT EXISTS " + DataContract.SeriesRecordings.TABLE + " ("
                + DataContract.SeriesRecordings.ID + " TEXT PRIMARY KEY,"            // str   required   ID (string!) of dvrAutorecEntry.
                + DataContract.SeriesRecordings.ENABLED + " INT DEFAULT 0,"          // u32   required   If autorec entry is enabled (activated).
                + DataContract.SeriesRecordings.NAME + " TEXT NULL,"                 // str   required   Name of the autorec entry (Added in version 18).
                + DataContract.SeriesRecordings.MIN_DURATION + " INT DEFAULT 0,"     // u32   required   Minimal duration in seconds (0 = Any).
                + DataContract.SeriesRecordings.MAX_DURATION + " INT DEFAULT 0,"     // u32   required   Maximal duration in seconds (0 = Any).
                + DataContract.SeriesRecordings.RETENTION + " INT DEFAULT 0,"        // u32   required   Retention time (in days).
                + DataContract.SeriesRecordings.DAYS_OF_WEEK + " INT DEFAULT 0,"     // u32   required   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
                + DataContract.SeriesRecordings.PRIORITY + " INT DEFAULT 0,"         // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
                + DataContract.SeriesRecordings.APPROX_TIME + " INT DEFAULT 0,"      // u32   required   Minutes from midnight (up to 24*60).
                + DataContract.SeriesRecordings.START + " INT DEFAULT 0,"            // s32   required   Exact start time (minutes from midnight) (Added in version 18).
                + DataContract.SeriesRecordings.START_WINDOW + " INT DEFAULT 0,"     // s32   required   Exact stop time (minutes from midnight) (Added in version 18).
                + DataContract.SeriesRecordings.START_EXTRA + " INT DEFAULT 0,"      // s64   required   Extra start minutes (pre-time).
                + DataContract.SeriesRecordings.STOP_EXTRA + " INT DEFAULT 0,"       // s64   required   Extra stop minutes (post-time).
                + DataContract.SeriesRecordings.TITLE + " TEXT NULL,"                // str   optional   Title.
                + DataContract.SeriesRecordings.FULLTEXT + " INT DEFAULT 0,"         // u32   optional   Fulltext flag (Added in version 20).
                + DataContract.SeriesRecordings.DIRECTORY + " TEXT NULL,"            // str   optional   Forced directory name (Added in version 19).
                + DataContract.SeriesRecordings.CHANNEL + " INT DEFAULT 0,"          // u32   optional   Channel ID.
                + DataContract.SeriesRecordings.OWNER + " TEXT NULL,"                // str   optional   Owner of this autorec entry (Added in version 18).
                + DataContract.SeriesRecordings.CREATOR + " TEXT NULL,"              // str   optional   Creator of this autorec entry (Added in version 18).
                + DataContract.SeriesRecordings.DUP_DETECT + " INT DEFAULT 0);";     // u32   optional   Duplicate detection (see addAutorecEntry) (Added in version 20).
        return query;
    }

    private String getTimerRecordingsQuery() {
        String query = "CREATE TABLE IF NOT EXISTS " + DataContract.TimerRecordings.TABLE + " ("
                + DataContract.TimerRecordings.ID + " TEXT PRIMARY KEY,"        // str   required   ID (string!) of timerecEntry.
                + DataContract.TimerRecordings.TITLE + " TEXT NULL,"            // str   required   Title for the recordings.
                + DataContract.TimerRecordings.DIRECTORY + " TEXT NULL,"        // str   optional   Forced directory name (Added in version 19).
                + DataContract.TimerRecordings.ENABLED + " INT DEFAULT 0,"      // u32   required   Title for the recordings.
                + DataContract.TimerRecordings.NAME + " TEXT NULL,"             // str   required   Name for this timerec entry.
                + DataContract.TimerRecordings.CONFIG_NAME + " TEXT NULL,"      // str   required   DVR Configuration Name / UUID.
                + DataContract.TimerRecordings.CHANNEL + " INT DEFAULT 0,"      // u32   required   Channel ID.
                + DataContract.TimerRecordings.DAYS_OF_WEEK + " INT DEFAULT 0," // u32   optional   Bitmask - Days of week (0x01 = Monday, 0x40 = Sunday, 0x7f = Whole Week, 0 = Not set).
                + DataContract.TimerRecordings.PRIORITY + " INT DEFAULT 0,"     // u32   optional   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set).
                + DataContract.TimerRecordings.START + " INT DEFAULT 0,"        // u32   required   Minutes from midnight (up to 24*60) for the start of the time window (including)
                + DataContract.TimerRecordings.STOP + " INT DEFAULT 0,"         // u32   required   Minutes from modnight (up to 24*60) for the end of the time window (including, cross-noon allowed)
                + DataContract.TimerRecordings.RETENTION + " INT DEFAULT 0,"    // u32   optional   Retention in days.
                + DataContract.TimerRecordings.OWNER + " TEXT NULL,"            // str   optional   Owner of this timerec entry.
                + DataContract.TimerRecordings.CREATOR + " TEXT NULL);";        // str   optional   Creator of this timerec entry.
        return query;
    }

    private String getServerInfoQuery() {
        String query = "CREATE TABLE IF NOT EXISTS " + DataContract.ServerInfo.TABLE + " ("
                + DataContract.ServerInfo.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DataContract.ServerInfo.ACCOUNT_ID + " INT DEFAULT 0,"           // The connection id where the credentials to the server are stored
                + DataContract.ServerInfo.TIME + " INT DEFAULT 0,"                 // s64   required   UNIX time.
                + DataContract.ServerInfo.GMT_OFFSET + " INT DEFAULT 0,"           // s32   optional   Minutes east of GMT.
                + DataContract.ServerInfo.FREE_DISC_SPACE + " INT DEFAULT 0,"      // s64   required   Bytes available.
                + DataContract.ServerInfo.TOTAL_DISC_SPACE + " INT DEFAULT 0,"     // s64   required   Total capacity.
                + DataContract.ServerInfo.HTSP_VERSION + " INT DEFAULT 0,"         // u32   required   The server supports all versions of the protocol up to and including this number.
                + DataContract.ServerInfo.SERVER_NAME + " TEXT NULL,"              // str   required   Server software name.
                + DataContract.ServerInfo.SERVER_VERSION + " TEXT NULL,"           // str   required   Server software version
                + DataContract.ServerInfo.WEB_ROOT + " TEXT NULL,"                 // str   optional   Server HTTP webroot
                + DataContract.ServerInfo.CHANNEL_COUNT + " INT DEFAULT 0,"
                + DataContract.ServerInfo.TAG_COUNT + " INT DEFAULT 0,"
                + DataContract.ServerInfo.RECORDING_COUNT + " INT DEFAULT 0,"
                + DataContract.ServerInfo.COMPLETED_RECORDING_COUNT + " INT DEFAULT 0,"
                + DataContract.ServerInfo.SCHEDULED_RECORDING_COUNT + " INT DEFAULT 0,"
                + DataContract.ServerInfo.FAILED_RECORDING_COUNT + " INT DEFAULT 0,"
                + DataContract.ServerInfo.REMOVED_RECORDING_COUNT + " INT DEFAULT 0,"
                + DataContract.ServerInfo.SERIES_RECORDING_COUNT + " INT DEFAULT 0,"
                + DataContract.ServerInfo.TIMER_RECORDING_COUNT + " INT DEFAULT 0);";
        return query;
    }
}
