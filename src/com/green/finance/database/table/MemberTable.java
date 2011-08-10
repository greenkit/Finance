package com.green.finance.database.table;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class MemberTable {

    public static final String TABLE_NAME = "member";

    public static final String COLUMN_ID = "member_id";
    public static final String COLUMN_NAME = "member_name";
    public static final String COLUMN_GENDER = "gender";
    public static final String COLUMN_BIRTH = "birth";
    public static final String COLUMN_PHOTO = "photo";

    public static void createTable (SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_GENDER + " TEXT,"
                + COLUMN_BIRTH + " TEXT,"
                + COLUMN_PHOTO + " BLOB);");

        // initial table
        ContentValues values = new ContentValues(3);
        values.put(COLUMN_NAME, "Green");
        values.put(COLUMN_GENDER, "male");
        values.put(COLUMN_BIRTH, "1984-7-16");
        db.insert(TABLE_NAME, null, values);

        values.clear();
        values.put(COLUMN_NAME, "Jasmine");
        values.put(COLUMN_GENDER, "female");
        values.put(COLUMN_BIRTH, "1985-7-19");
        db.insert(TABLE_NAME, null, values);

        values.clear();
        values.put(COLUMN_NAME, "Family");
        values.put(COLUMN_GENDER, "N/A");
        values.put(COLUMN_BIRTH, "2010-7-30");
        db.insert(TABLE_NAME, null, values);
    }

    public static void dropTable (SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
