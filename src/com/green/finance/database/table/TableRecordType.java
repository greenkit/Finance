
package com.green.finance.database.table;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.green.finance.R;

public class TableRecordType {

    public static final String TABLE_NAME = "type";

    public static final String DEFAULT_TYPE = "Other";

    // Column names:
    public static final String COLUMN_ID = "type_id";
    public static final String COLUMN_NAME = "type_name";

    public static void createTable (Context context, SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_NAME + " TEXT);");

        // initial table
        Resources resource = context.getResources();
        ContentValues values = new ContentValues(1);

        values.put(COLUMN_NAME, resource.getString(R.string.life));
        Log.d("TableType", "id=" + db.insert(TABLE_NAME, null, values));

        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.communication));
        db.insert(TABLE_NAME, null, values);

        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.traffic));
        db.insert(TABLE_NAME, null, values);

        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.medical));
        db.insert(TABLE_NAME, null, values);
        
        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.social));
        db.insert(TABLE_NAME, null, values);
        
        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.resident));
        db.insert(TABLE_NAME, null, values);
        
        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.study));
        db.insert(TABLE_NAME, null, values);
        
        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.travel));
        db.insert(TABLE_NAME, null, values);
        
        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.investment));
        db.insert(TABLE_NAME, null, values);
        
        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.wasteful));
        db.insert(TABLE_NAME, null, values);
        
        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.pet));
        db.insert(TABLE_NAME, null, values);
        
        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.amusement));
        db.insert(TABLE_NAME, null, values);
        
        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.lend));
        db.insert(TABLE_NAME, null, values);

        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.clothing));
        db.insert(TABLE_NAME, null, values);

        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.supplies));
        db.insert(TABLE_NAME, null, values);

        values.clear();
        values.put(COLUMN_NAME, resource.getString(R.string.salary));
        db.insert(TABLE_NAME, null, values);
    }

    public static void dropTable (SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
