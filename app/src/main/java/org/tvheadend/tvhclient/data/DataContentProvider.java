package org.tvheadend.tvhclient.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;

import java.util.ArrayList;
import java.util.List;

public class DataContentProvider extends ContentProvider {
    private final static String TAG = DataContentProvider.class.getSimpleName();

    // helper constants for use with the UriMatcher
    private static final int CONNECTION_LIST = 1;
    private static final int CONNECTION_ID = 2;
    private static final int PROFILE_LIST = 3;
    private static final int PROFILE_ID = 4;
    private static final UriMatcher mUriMatcher;

    // prepare the UriMatcher
    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(DataContract.AUTHORITY, "connections", CONNECTION_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, "connections/#", CONNECTION_ID);
        mUriMatcher.addURI(DataContract.AUTHORITY, "profiles", PROFILE_LIST);
        mUriMatcher.addURI(DataContract.AUTHORITY, "profiles/#", PROFILE_ID);
    }

    private DatabaseHelper mHelper;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate() called");
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
                // limit query to one row at most
                builder.appendWhere(DataContract.Connections.ID + " = " + uri.getLastPathSegment());
                break;

            case PROFILE_LIST:
                builder.setTables(DataContract.Profiles.TABLE);
                break;

            case PROFILE_ID:
                builder.setTables(DataContract.Connections.TABLE);
                // limit query to one row at most
                builder.appendWhere(DataContract.Profiles.ID + " = " + uri.getLastPathSegment());
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        // if you like you can log the query
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
        Log.d(TAG, "getType() called with: uri = [" + uri + "]");

        switch (mUriMatcher.match(uri)) {
            case CONNECTION_ID:
                return DataContract.Connections.CONTENT_CONNECTION_TYPE;
            case CONNECTION_LIST:
                return DataContract.Connections.CONTENT_TYPE;
            case PROFILE_ID:
                return DataContract.Profiles.CONTENT_PROFILE_TYPE;
            case PROFILE_LIST:
                return DataContract.Profiles.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        Log.d(TAG, "insert() called with: uri = [" + uri + "]");

        // Check if the uri is valid
        if (mUriMatcher.match(uri) != CONNECTION_LIST
                && mUriMatcher.match(uri) != PROFILE_LIST) {
            throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
        }

        SQLiteDatabase db = mHelper.getWritableDatabase();
        Uri newUri = null;
        long id;

        // Depending on the uri update the provides values
        switch (mUriMatcher.match(uri)) {
            case CONNECTION_LIST:
                id = db.insert(DataContract.Connections.TABLE, null, contentValues);
                newUri = getUriForId(id, uri);
                break;

            case PROFILE_LIST:
                id = db.insert(DataContract.Profiles.TABLE, null, contentValues);
                newUri = getUriForId(id, uri);
                break;
        }

        return newUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "delete() called with: uri = [" + uri + "]");

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
        Log.d(TAG, "update() called with: uri = [" + uri + "], contentValues = [" + contentValues + "], selection = [" + selection + "], selectionArgs = [" + selectionArgs[0] + "]");

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
