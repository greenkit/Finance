
package com.green.finance.database;

import com.green.finance.R;
import com.green.finance.database.table.TableMember;
import com.green.finance.database.table.TablePayment;
import com.green.finance.database.table.TableRecord;
import com.green.finance.database.table.TableSnapshot;
import com.green.finance.database.table.TableRecordType;
import com.green.finance.datatype.Record;
import com.green.finance.utils.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "/sdcard/finance/finance.db";

    private static final int DATABASE_VERSION = 1;

    private static DatabaseHelper sHelper;
    private static DatabaseObservable sObservable;
    private static Context mContext;
    private static Resources mResources;

    public static void init(Context context) {
        sHelper = new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
        sObservable = new DatabaseObservable();
        mContext = context;
        mResources = context.getResources();
    }

    public static DatabaseHelper getInstance() {
        if (sHelper == null) {
            throw new IllegalStateException("DatabaseHelper has NOT been initialized!");
        }

        return sHelper;
    }

    private DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        TableMember.createTable(db);
        TablePayment.createTable(mContext, db);
        TableRecordType.createTable(mContext, db);
        TableRecord.createTable(db);
        TableSnapshot.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TableRecord.dropTable(db);
        TableRecordType.dropTable(db);
        TablePayment.dropTable(db);
        TableMember.dropTable(db);
        TableSnapshot.dropTable(db);
        onCreate(db);
    }

    private Cursor queryRecord (String where, String[] args) {
        if (Utils.isEmpty(where)) { where = "1=1";}

        String sql = "SELECT " + TableRecord.COLUMN_ID + " AS _id "
                    + ", " + TableRecord.COLUMN_NAME
                    + ", " + TableRecord.COLUMN_AMOUNT
                    + ", " + TableRecord.COLUMN_TYPE
                    + ", " + TableRecord.COLUMN_CONSUMER
                    + ", " + TableRecord.COLUMN_PAYMENT
                    + ", " + TableRecord.COLUMN_IO
                    + ", " + TableRecord.COLUMN_REMARK
                    + ", " + TableRecord.COLUMN_DATE
                    + ", " + TableMember.COLUMN_NAME
                    + ", " + TablePayment.COLUMN_NAME
                    + ", " + TableSnapshot.COLUMN_SNAPSHOT
                    + ", " + TableRecordType.COLUMN_NAME
                    + " FROM (((" + TableRecord.TABLE_NAME
                    + " LEFT JOIN " + TableRecordType .TABLE_NAME + " ON " + TableRecord.COLUMN_TYPE
                    + "=" + TableRecordType.COLUMN_ID + ") T1" + " LEFT JOIN " + TableMember.TABLE_NAME
                    + " ON " + "T1." + TableRecord.COLUMN_CONSUMER + "=" + TableMember.COLUMN_ID + ") T2"
                    + " LEFT JOIN " + TablePayment.TABLE_NAME + " ON " + "T2." + TableRecord.COLUMN_PAYMENT
                    + "=" + TablePayment.COLUMN_ID + ") T3 " + " LEFT JOIN " + TableSnapshot.TABLE_NAME + " ON "
                    + " T3." + TableRecord.COLUMN_ID + "=" + TableSnapshot.COLUMN_RECORD + " WHERE " + where
                    + " ORDER BY " + TableRecord.COLUMN_DATE + " DESC ";

        return getReadableDatabase().rawQuery(sql, args);
    }

    private float queryTotal (String where, String[] args) {
        if (Utils.isEmpty(where)) { where = " 1=1 "; }
        SQLiteDatabase db = getReadableDatabase();
        String columeTotal = "total";
        String sql = "SELECT TOTAL(" + TableRecord.COLUMN_AMOUNT + ") AS " + columeTotal + " FROM "
        + TableRecord.TABLE_NAME + " WHERE " + where;
        Cursor cursor = db.rawQuery(sql, args);
        if (cursor != null && cursor.moveToFirst()) {
            float total = cursor.getFloat(cursor.getColumnIndexOrThrow("total"));
            cursor.close();
            return total;
        }
        
        return 0;
    }

    private float queryTotalByIo (String io) {
        String where = TableRecord.COLUMN_IO + "=?";
        String[] args = new String[] { io };
        
        return queryTotal(where, args);
    }

    public void deleteRecordById (long id) {
        SQLiteDatabase db = getWritableDatabase();
        int count = db.delete(TableRecord.TABLE_NAME, TableRecord.COLUMN_ID + "=?",
                new String[] { String.valueOf(id)});
        if (count > 0) {
            sObservable.notifyChange();
        }
    }

    public Cursor queryRecordsByCurrentMonth() {
        String where = TableRecord.COLUMN_DATE + " BETWEEN ? AND ? ";

        String[] args = {
                String.valueOf(Utils.getMinTimeInMillisByCurrentMonth()),
                String.valueOf(Utils.getMaxTimeInMillisByCurrentMonth())
        };

        return queryRecord(where, args);
    }

    public Cursor queryAllRecords() {
        return queryRecord(null, null);
    }

    public Record queryRecordById(long id) {
        String where = TableRecord.COLUMN_ID + "=? ";
        String[] args = { String.valueOf(id)};

        Cursor c = queryRecord(where, args);
        Record record = getRecord(c);
        c.close();

        return record;
    }

    public int updateRecordById(Record record, long id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(9);
        values.put(TableRecord.COLUMN_AMOUNT, record.amount);
        values.put(TableRecord.COLUMN_DATE, record.date);
        values.put(TableRecord.COLUMN_IO, record.io);
        values.put(TableRecord.COLUMN_CONSUMER, queryMemberIdByName(record.member));
        values.put(TableRecord.COLUMN_NAME, record.name);
        values.put(TableRecord.COLUMN_PAYMENT, queryPaymentIdByName(record.payment));
        values.put(TableRecord.COLUMN_REMARK, record.remark);
        values.put(TableRecord.COLUMN_TYPE, queryTypeIdByName(record.type));
        return db.update(TableRecord.TABLE_NAME, values, TableRecord.COLUMN_ID + "=?",
                new String[] {
                    String.valueOf(id)
                });
    }

    public float queryTotalIncome () {
        return queryTotalByIo(mResources.getString(R.string.income));
    }

    public float queryTotalOutcome () {
        return queryTotalByIo(mResources.getString(R.string.outcome));
    }

    public long insertRecord(Record record) {
        float amount = record.amount;
        long date = record.date;
        String io = record.io;
        int member = queryMemberIdByName(record.member);
        String name = record.name;
        int payment = queryPaymentIdByName(record.payment);
        String remark = record.remark;
        int type = queryTypeIdByName(record.type);

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(9);
        values.put(TableRecord.COLUMN_AMOUNT, amount);
        values.put(TableRecord.COLUMN_DATE, date);
        values.put(TableRecord.COLUMN_IO, io);
        values.put(TableRecord.COLUMN_CONSUMER, member);
        values.put(TableRecord.COLUMN_NAME, name);
        values.put(TableRecord.COLUMN_PAYMENT, payment);
        values.put(TableRecord.COLUMN_REMARK, remark);
        values.put(TableRecord.COLUMN_TYPE, type);
        return db.insert(TableRecord.TABLE_NAME, null, values);
    }

    public int queryPaymentIdByName(String payment) {
        if (Utils.isEmpty(payment)) {
            return -1;
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TablePayment.TABLE_NAME, new String[] {
            TablePayment.COLUMN_ID
        }, TablePayment.COLUMN_NAME + "=?", new String[] {
            payment
        }, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndexOrThrow(TablePayment.COLUMN_ID));
        } else {
            return -1;
        }
    }

    public int queryTypeIdByName(String type) {
        if (Utils.isEmpty(type)) {
            return -1;
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TableRecordType.TABLE_NAME, new String[] {
            TableRecordType.COLUMN_ID
        }, TableRecordType.COLUMN_NAME + "=?", new String[] {
            type
        }, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndexOrThrow(TableRecordType.COLUMN_ID));
        } else {
            return -1;
        }
    }

    public int queryMemberIdByName(String member) {
        if (Utils.isEmpty(member)) {
            return -1;
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TableMember.TABLE_NAME, new String[] {
            TableMember.COLUMN_ID
        }, TableMember.COLUMN_NAME + "=?", new String[] {
            member
        }, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndexOrThrow(TableMember.COLUMN_ID));
        } else {
            return -1;
        }
    }

    public Cursor queryPayment() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TablePayment.TABLE_NAME, new String[] {
                TablePayment.COLUMN_ID + " AS _id ", TablePayment.COLUMN_NAME
        }, null, null, null, null, null);
    }

    public Cursor queryRecordName() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TableRecord.TABLE_NAME, new String[] {
                TableRecord.COLUMN_ID + " AS _id ", TableRecord.COLUMN_NAME
        }, null, null, null, null, null);
    }

    public Cursor queryRecordNameByKey(String key) {
        SQLiteDatabase db = getReadableDatabase();

        return db.query(TableRecord.TABLE_NAME, new String[] {
                TableRecord.COLUMN_ID + " AS _id ", TableRecord.COLUMN_NAME
        }, TableRecord.COLUMN_NAME + " LIKE '%" + key + "%'", null, TableRecord.COLUMN_NAME, null, null);
    }

    public String[] getPaymentNames() {
        Cursor cursor = queryPayment();
        String[] payment = null;
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getCount();
            payment = new String[count];
            int index = cursor.getColumnIndexOrThrow(TablePayment.COLUMN_NAME);
            for (int i = 0; i < count; i++, cursor.moveToNext()) {
                payment[i] = cursor.getString(index);
            }
        }

        return payment;
    }

    public Cursor queryRecordType() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TableRecordType.TABLE_NAME, new String[] {
                TableRecordType.COLUMN_ID + " AS _id ", TableRecordType.COLUMN_NAME
        }, null, null, null, null, null);
    }

    public String[] getTypeNames() {
        Cursor cursor = queryRecordType();
        String[] type = null;
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getCount();
            type = new String[count];
            int index = cursor.getColumnIndexOrThrow(TableRecordType.COLUMN_NAME);
            for (int i = 0; i < count; i++, cursor.moveToNext()) {
                type[i] = cursor.getString(index);
            }
        }

        return type;
    }

    public Cursor queryMember() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TableMember.TABLE_NAME, new String[] {
                TableMember.COLUMN_ID + " AS _id ", TableMember.COLUMN_NAME
        }, null, null, null, null, null);
    }

    public String[] getMemberNames() {
        Cursor cursor = queryMember();
        String[] member = null;
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getCount();
            member = new String[count];
            int index = cursor.getColumnIndexOrThrow(TableMember.COLUMN_NAME);
            for (int i = 0; i < count; i++, cursor.moveToNext()) {
                member[i] = cursor.getString(index);
            }
        }

        return member;
    }

    public String[] getIoName() {
        return mResources.getStringArray(R.array.array_io);
    }

    public void registerObserver(DatabaseObserver observer) {
        sObservable.registerObserver(observer);
    }

    private Record getRecord(Cursor c) {
        Record record = null;

        if (c != null && c.moveToFirst()) {
            record = new Record();
            record.name = c.getString(c.getColumnIndexOrThrow(TableRecord.COLUMN_NAME));
            record.amount = c.getFloat(c.getColumnIndexOrThrow(TableRecord.COLUMN_AMOUNT));
            record.io = c.getString(c.getColumnIndexOrThrow(TableRecord.COLUMN_IO));
            record.remark = c.getString(c.getColumnIndexOrThrow(TableRecord.COLUMN_REMARK));
            record.date = c.getLong(c.getColumnIndexOrThrow(TableRecord.COLUMN_DATE));
            record.type = c.getString(c.getColumnIndexOrThrow(TableRecordType.COLUMN_NAME));
            record.payment = c.getString(c.getColumnIndexOrThrow(TablePayment.COLUMN_NAME));
            record.member = c.getString(c.getColumnIndexOrThrow(TableMember.COLUMN_NAME));
        }

        return record;
    }

}
