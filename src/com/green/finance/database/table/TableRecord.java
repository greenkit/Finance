
package com.green.finance.database.table;

import android.database.sqlite.SQLiteDatabase;

public class TableRecord {

    // Table name:
    public static final String TABLE_NAME = "record";

    // Column names:
    public static final String COLUMN_ID = "record_id";
    public static final String COLUMN_NAME = "record_name";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TYPE = "type_ref";
    public static final String COLUMN_CONSUMER = "member_ref";
    public static final String COLUMN_PAYMENT = "payment_ref";
    public static final String COLUMN_IO = "io";
    public static final String COLUMN_REMARK = "remark";
    public static final String COLUMN_DATE = "date";

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_AMOUNT + " FLOAT,"
                + COLUMN_TYPE + " INTEGER,"
                + COLUMN_CONSUMER + " INTEGER,"
                + COLUMN_PAYMENT + " INTEGER,"
                + COLUMN_IO + " TEXT,"
                + COLUMN_REMARK + " TEXT,"
                + COLUMN_DATE + " LONG);");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
