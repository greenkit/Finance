package com.green.finance.database.table;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;

import com.green.finance.R;

public class TablePayment {

    public static final String TABLE_NAME = "payment";

    public static final String COLUMN_ID = "payment_id";
    public static final String COLUMN_NAME = "payment_name";

    public static void createTable (Context context, SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_NAME + " TEXT);");

        // initial table
        Resources resource = context.getResources();
        ContentValues values = new ContentValues(1);
        values.put(COLUMN_NAME, resource.getString(R.string.payment_cash));
        db.insert(TABLE_NAME, null, values);

        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.payment_credit));
        db.insert(TABLE_NAME, null, values);

        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.payment_bank));
        db.insert(TABLE_NAME, null, values);
    }

    public static void dropTable (SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
