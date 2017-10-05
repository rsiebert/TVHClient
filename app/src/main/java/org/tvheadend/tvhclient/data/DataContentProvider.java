package org.tvheadend.tvhclient.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.tvheadend.tvhclient.DatabaseHelper;

public class DataContentProvider extends ContentProvider {
    private final static String TAG = DataContentProvider.class.getSimpleName();

    // helper constants for use with the UriMatcher
    private static final int CONNECTION_LIST = 1;
    private static final int CONNECTION_ID = 2;
    private static final int PROFILE_LIST = 3;
    private static final int PROFILE_ID = 4;
    private static final int CHANNEL_LIST = 5;
    private static final int CHANNEL_ID = 6;
    private static final int TAG_LIST = 7;
    private static final int TAG_ID = 8;
    private static final int PROGRAM_LIST = 9;
    private static final int PROGRAM_ID = 10;
    private static final int RECORDING_LIST = 11;
    private static final int RECORDING_ID = 12;
    private static final int SERIES_RECORDING_LIST = 13;
    private static final int SERIES_RECORDING_ID = 14;
    private static final int TIMER_RECORDING_LIST = 15;
    private static final int TIMER_RECORDING_ID = 16;

    public static final int SERVER_INFO_ID = 25;

    private static final UriMatcher mUriMatcher;

    // prepare the UriMatcher
    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Connections.TABLE, CONNECTION_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Connections.TABLE + "/#", CONNECTION_ID);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Profiles.TABLE, PROFILE_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Profiles.TABLE + "/#", PROFILE_ID);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Channels.TABLE, CHANNEL_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Channels.TABLE + "/#", CHANNEL_ID);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Tags.TABLE, TAG_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Tags.TABLE + "/#", TAG_ID);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Programs.TABLE, PROGRAM_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Programs.TABLE + "/#", PROGRAM_ID);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Recordings.TABLE, RECORDING_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Recordings.TABLE + "/#", RECORDING_ID);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.SeriesRecordings.TABLE, SERIES_RECORDING_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.SeriesRecordings.TABLE + "/#", SERIES_RECORDING_ID);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.TimerRecordings.TABLE, TIMER_RECORDING_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.TimerRecordings.TABLE + "/#", TIMER_RECORDING_ID);
        mUriMatcher.addURI(DataContract.AUTHORITY, DataContract.Connections.TABLE + "/#", SERVER_INFO_ID);
    }

    private DatabaseHelper mHelper;

    @Override
    public boolean onCreate() {
        mHelper = DatabaseHelper.getInstance(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query() called with: uri = [" + uri + "]");

        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        switch (mUriMatcher.match(uri)) {
            case CONNECTION_LIST:
                builder.setTables(DataContract.Connections.TABLE);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DataContract.Connections.SORT_ORDER_DEFAULT;
                }
                break;
            case CONNECTION_ID:
                builder.setTables(DataContract.Connections.TABLE);
                builder.appendWhere(DataContract.Connections.ID + " = " + uri.getLastPathSegment());
                break;
            case PROFILE_LIST:
                builder.setTables(DataContract.Profiles.TABLE);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DataContract.Profiles.SORT_ORDER_DEFAULT;
                }
                break;
            case PROFILE_ID:
                builder.setTables(DataContract.Profiles.TABLE);
                builder.appendWhere(DataContract.Profiles.ID + " = " + uri.getLastPathSegment());
                break;
            case CHANNEL_LIST:
                builder.setTables(DataContract.Channels.TABLE);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DataContract.Channels.SORT_ORDER_DEFAULT;
                }
                break;
            case CHANNEL_ID:
                builder.setTables(DataContract.Channels.TABLE);
                builder.appendWhere(DataContract.Channels.ID + " = " + uri.getLastPathSegment());
                break;
            case TAG_LIST:
                builder.setTables(DataContract.Tags.TABLE);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DataContract.Tags.SORT_ORDER_DEFAULT;
                }
                break;
            case TAG_ID:
                builder.setTables(DataContract.Tags.TABLE);
                builder.appendWhere(DataContract.Tags.ID + " = " + uri.getLastPathSegment());
                break;
            case PROGRAM_LIST:
                builder.setTables(DataContract.Programs.TABLE);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DataContract.Programs.SORT_ORDER_DEFAULT;
                }
                break;
            case PROGRAM_ID:
                builder.setTables(DataContract.Programs.TABLE);
                builder.appendWhere(DataContract.Programs.ID + " = " + uri.getLastPathSegment());
                break;
            case RECORDING_LIST:
                builder.setTables(DataContract.Recordings.TABLE);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DataContract.Recordings.SORT_ORDER_DEFAULT;
                }
                break;
            case RECORDING_ID:
                builder.setTables(DataContract.Recordings.TABLE);
                builder.appendWhere(DataContract.Recordings.ID + " = " + uri.getLastPathSegment());
                break;
            case SERIES_RECORDING_LIST:
                builder.setTables(DataContract.SeriesRecordings.TABLE);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DataContract.SeriesRecordings.SORT_ORDER_DEFAULT;
                }
                break;
            case SERIES_RECORDING_ID:
                builder.setTables(DataContract.SeriesRecordings.TABLE);
                builder.appendWhere(DataContract.SeriesRecordings.ID + " = " + uri.getLastPathSegment());
                break;
            case TIMER_RECORDING_LIST:
                builder.setTables(DataContract.TimerRecordings.TABLE);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DataContract.TimerRecordings.SORT_ORDER_DEFAULT;
                }
                break;
            case TIMER_RECORDING_ID:
                builder.setTables(DataContract.TimerRecordings.TABLE);
                builder.appendWhere(DataContract.TimerRecordings.ID + " = " + uri.getLastPathSegment());
                break;
            case SERVER_INFO_ID:
                builder.setTables(DataContract.Connections.TABLE);
                builder.appendWhere(DataContract.Connections.SELECTED + " =1");
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        Log.d(TAG, "SQL query: " + builder.buildQuery(projection, selection, null, null, sortOrder, null));

        Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        // if we want to be notified of any changes:
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case CONNECTION_ID:
                return DataContract.Connections.CONTENT_ITEM_TYPE;
            case CONNECTION_LIST:
                return DataContract.Connections.CONTENT_TYPE;
            case PROFILE_ID:
                return DataContract.Profiles.CONTENT_ITEM_TYPE;
            case PROFILE_LIST:
                return DataContract.Profiles.CONTENT_TYPE;
            case CHANNEL_ID:
                return DataContract.Channels.CONTENT_ITEM_TYPE;
            case CHANNEL_LIST:
                return DataContract.Channels.CONTENT_TYPE;
            case TAG_ID:
                return DataContract.Tags.CONTENT_ITEM_TYPE;
            case TAG_LIST:
                return DataContract.Tags.CONTENT_TYPE;
            case PROGRAM_ID:
                return DataContract.Programs.CONTENT_ITEM_TYPE;
            case PROGRAM_LIST:
                return DataContract.Programs.CONTENT_TYPE;
            case RECORDING_ID:
                return DataContract.Recordings.CONTENT_ITEM_TYPE;
            case RECORDING_LIST:
                return DataContract.Recordings.CONTENT_TYPE;
            case SERIES_RECORDING_ID:
                return DataContract.SeriesRecordings.CONTENT_ITEM_TYPE;
            case SERIES_RECORDING_LIST:
                return DataContract.SeriesRecordings.CONTENT_TYPE;
            case TIMER_RECORDING_ID:
                return DataContract.TimerRecordings.CONTENT_ITEM_TYPE;
            case TIMER_RECORDING_LIST:
                return DataContract.TimerRecordings.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        // Check if the uri is valid
        if (mUriMatcher.match(uri) != CONNECTION_LIST
                && mUriMatcher.match(uri) != PROFILE_LIST
                && mUriMatcher.match(uri) != CHANNEL_LIST
                && mUriMatcher.match(uri) != TAG_LIST
                && mUriMatcher.match(uri) != PROGRAM_LIST
                && mUriMatcher.match(uri) != RECORDING_LIST
                && mUriMatcher.match(uri) != SERIES_RECORDING_LIST
                && mUriMatcher.match(uri) != TIMER_RECORDING_LIST) {
            throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
        }

        SQLiteDatabase db = mHelper.getWritableDatabase();
        Uri newUri = null;
        long id = 0;

        // Depending on the uri update the provides values
        switch (mUriMatcher.match(uri)) {
            case CONNECTION_LIST:
                id = db.insert(DataContract.Connections.TABLE, null, contentValues);
                break;
            case PROFILE_LIST:
                id = db.insert(DataContract.Profiles.TABLE, null, contentValues);
                break;
            case CHANNEL_LIST:
                id = db.insertWithOnConflict(DataContract.Channels.TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
                break;
            case TAG_LIST:
                id = db.insertWithOnConflict(DataContract.Tags.TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
                break;
            case PROGRAM_LIST:
                id = db.insertWithOnConflict(DataContract.Programs.TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
                break;
            case RECORDING_LIST:
                id = db.insertWithOnConflict(DataContract.Recordings.TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
                break;
            case SERIES_RECORDING_LIST:
                id = db.insertWithOnConflict(DataContract.SeriesRecordings.TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
                 break;
            case TIMER_RECORDING_LIST:
                id = db.insertWithOnConflict(DataContract.TimerRecordings.TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
                break;
        }

        return getUriForId(id, uri);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int deletedRows;
        String where;

        switch (mUriMatcher.match(uri)) {
            case CONNECTION_LIST:
                deletedRows = db.delete(DataContract.Connections.TABLE, selection, selectionArgs);
                break;

            case CONNECTION_ID:
                where = DataContract.Connections.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                deletedRows = db.delete(DataContract.Connections.TABLE, where, selectionArgs);
                break;

            case PROFILE_LIST:
                deletedRows = db.delete(DataContract.Profiles.TABLE, selection, selectionArgs);
                break;

            case PROFILE_ID:
                where = DataContract.Profiles.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                deletedRows = db.delete(DataContract.Profiles.TABLE, where, selectionArgs);
                break;

            case CHANNEL_LIST:
                deletedRows = db.delete(DataContract.Channels.TABLE, selection, selectionArgs);
                break;

            case CHANNEL_ID:
                where = DataContract.Channels.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                deletedRows = db.delete(DataContract.Channels.TABLE, where, selectionArgs);
                break;

            case TAG_LIST:
                deletedRows = db.delete(DataContract.Tags.TABLE, selection, selectionArgs);
                break;

            case TAG_ID:
                where = DataContract.Tags.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                deletedRows = db.delete(DataContract.Tags.TABLE, where, selectionArgs);
                break;

            case PROGRAM_LIST:
                deletedRows = db.delete(DataContract.Programs.TABLE, selection, selectionArgs);
                break;

            case PROGRAM_ID:
                where = DataContract.Programs.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                deletedRows = db.delete(DataContract.Programs.TABLE, where, selectionArgs);
                break;

            case RECORDING_LIST:
                deletedRows = db.delete(DataContract.Recordings.TABLE, selection, selectionArgs);
                break;

            case RECORDING_ID:
                where = DataContract.Recordings.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                deletedRows = db.delete(DataContract.Recordings.TABLE, where, selectionArgs);
                break;

            case SERIES_RECORDING_LIST:
                deletedRows = db.delete(DataContract.SeriesRecordings.TABLE, selection, selectionArgs);
                break;

            case SERIES_RECORDING_ID:
                where = DataContract.SeriesRecordings.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                deletedRows = db.delete(DataContract.SeriesRecordings.TABLE, where, selectionArgs);
                break;

            case TIMER_RECORDING_LIST:
                deletedRows = db.delete(DataContract.TimerRecordings.TABLE, selection, selectionArgs);
                break;

            case TIMER_RECORDING_ID:
                where = DataContract.TimerRecordings.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                deletedRows = db.delete(DataContract.TimerRecordings.TABLE, where, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Invalid URI: " + uri);
        }

        // notify all listeners of the changes
        if (getContext() != null && deletedRows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return deletedRows;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int updateCount;
        String where;

        switch (mUriMatcher.match(uri)) {
            case CONNECTION_LIST:
                updateCount = db.update(DataContract.Connections.TABLE, contentValues, selection, selectionArgs);
                break;

            case CONNECTION_ID:
                where = DataContract.Connections.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                updateCount = db.update(DataContract.Connections.TABLE, contentValues, where, selectionArgs);
                break;

            case PROFILE_LIST:
                updateCount = db.update(DataContract.Profiles.TABLE, contentValues, selection, selectionArgs);
                break;

            case PROFILE_ID:
                where = DataContract.Profiles.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                updateCount = db.update(DataContract.Profiles.TABLE, contentValues, where, selectionArgs);
                break;

            case CHANNEL_LIST:
                updateCount = db.update(DataContract.Channels.TABLE, contentValues, selection, selectionArgs);
                break;

            case CHANNEL_ID:
                where = DataContract.Channels.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                updateCount = db.update(DataContract.Channels.TABLE, contentValues, where, selectionArgs);
                break;

            case TAG_LIST:
                updateCount = db.update(DataContract.Tags.TABLE, contentValues, selection, selectionArgs);
                break;

            case TAG_ID:
                where = DataContract.Tags.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                updateCount = db.update(DataContract.Tags.TABLE, contentValues, where, selectionArgs);
                break;

            case PROGRAM_LIST:
                updateCount = db.update(DataContract.Programs.TABLE, contentValues, selection, selectionArgs);
                break;

            case PROGRAM_ID:
                where = DataContract.Programs.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                updateCount = db.update(DataContract.Programs.TABLE, contentValues, where, selectionArgs);
                break;

            case RECORDING_LIST:
                updateCount = db.update(DataContract.Recordings.TABLE, contentValues, selection, selectionArgs);
                break;

            case RECORDING_ID:
                where = DataContract.Recordings.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                updateCount = db.update(DataContract.Recordings.TABLE, contentValues, where, selectionArgs);
                break;

            case SERIES_RECORDING_LIST:
                updateCount = db.update(DataContract.SeriesRecordings.TABLE, contentValues, selection, selectionArgs);
                break;

            case SERIES_RECORDING_ID:
                where = DataContract.SeriesRecordings.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                updateCount = db.update(DataContract.SeriesRecordings.TABLE, contentValues, where, selectionArgs);
                break;

            case TIMER_RECORDING_LIST:
                updateCount = db.update(DataContract.TimerRecordings.TABLE, contentValues, selection, selectionArgs);
                break;

            case TIMER_RECORDING_ID:
                where = DataContract.TimerRecordings.ID + " = " + uri.getLastPathSegment();
                where += TextUtils.isEmpty(selection) ? "" : " AND " + selection;
                updateCount = db.update(DataContract.TimerRecordings.TABLE, contentValues, where, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        // notify all listeners of changes:
        if (getContext() != null && updateCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return updateCount;
    }

    private Uri getUriForId(long id, Uri uri) {
        if (id > 0) {
            Uri itemUri = ContentUris.withAppendedId(uri, id);
            if (getContext() != null) {
                // notify all listeners of changes and return itemUri:
                getContext().getContentResolver().notifyChange(itemUri, null);
            }
            return itemUri;
        }
        throw new SQLException("Problem while inserting into uri: " + uri);
    }
}
