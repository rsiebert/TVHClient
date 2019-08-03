package org.tvheadend.tvhclient.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Helper class to get access to the old SQLite database.
 * Required to move the old connection information to the new room database
 */
class DatabaseHelperForMigration private constructor(context: Context) : SQLiteOpenHelper(context, "tvhclient", null, 9) {

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        // NOP
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // NOP
    }

    companion object {

        private var instance: DatabaseHelperForMigration? = null

        fun getInstance(ctx: Context): DatabaseHelperForMigration? {
            if (instance == null) {
                instance = DatabaseHelperForMigration(ctx)
            }
            return instance
        }
    }
}
