package com.green.finance.database.table;

import android.database.sqlite.SQLiteDatabase;

public class SnapshotTable {

    public static final String TABLE_NAME = "snapshot";

    public static final String COLUMN_ID = "snapshot_id";
    public static final String COLUMN_RECORD = "record_ref";
    public static final String COLUMN_SNAPSHOT = "snapshot";

    public static void createTable (SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_RECORD + " INTEGER,"
                + COLUMN_SNAPSHOT + " BLOB);");
    }

    public static void dropTable (SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
