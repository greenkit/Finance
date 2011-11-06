
package com.green.finance.database;

import com.green.finance.R;
import com.green.finance.database.table.MemberTable;
import com.green.finance.database.table.PaymentTable;
import com.green.finance.database.table.RecordTable;
import com.green.finance.database.table.SnapshotTable;
import com.green.finance.database.table.TypeTable;
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
        MemberTable.createTable(db);
        PaymentTable.createTable(mContext, db);
        TypeTable.createTable(mContext, db);
        RecordTable.createTable(db);
        SnapshotTable.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        RecordTable.dropTable(db);
        TypeTable.dropTable(db);
        PaymentTable.dropTable(db);
        MemberTable.dropTable(db);
        SnapshotTable.dropTable(db);
        onCreate(db);
    }

    private Cursor queryRecord (String where, String[] args) {
        if (Utils.isEmpty(where)) { where = "1=1";}

        String sql = "SELECT " + RecordTable.COLUMN_ID + " AS _id "
                    + ", " + RecordTable.COLUMN_NAME
                    + ", " + RecordTable.COLUMN_AMOUNT
                    + ", " + RecordTable.COLUMN_TYPE
                    + ", " + RecordTable.COLUMN_CONSUMER
                    + ", " + RecordTable.COLUMN_PAYMENT
                    + ", " + RecordTable.COLUMN_IO
                    + ", " + RecordTable.COLUMN_REMARK
                    + ", " + RecordTable.COLUMN_DATE
                    + ", " + MemberTable.COLUMN_NAME
                    + ", " + PaymentTable.COLUMN_NAME
                    + ", " + SnapshotTable.COLUMN_SNAPSHOT
                    + ", " + TypeTable.COLUMN_NAME
                    + " FROM (((" + RecordTable.TABLE_NAME
                    + " LEFT JOIN " + TypeTable .TABLE_NAME + " ON " + RecordTable.COLUMN_TYPE
                    + "=" + TypeTable.COLUMN_ID + ") T1" + " LEFT JOIN " + MemberTable.TABLE_NAME
                    + " ON " + "T1." + RecordTable.COLUMN_CONSUMER + "=" + MemberTable.COLUMN_ID + ") T2"
                    + " LEFT JOIN " + PaymentTable.TABLE_NAME + " ON " + "T2." + RecordTable.COLUMN_PAYMENT
                    + "=" + PaymentTable.COLUMN_ID + ") T3 " + " LEFT JOIN " + SnapshotTable.TABLE_NAME + " ON "
                    + " T3." + RecordTable.COLUMN_ID + "=" + SnapshotTable.COLUMN_RECORD + " WHERE " + where
                    + " ORDER BY " + RecordTable.COLUMN_DATE + " DESC ";

        return getReadableDatabase().rawQuery(sql, args);
    }

    private float queryTotal (String where, String[] args) {
        if (Utils.isEmpty(where)) { where = " 1=1 "; }
        SQLiteDatabase db = getReadableDatabase();
        String columeTotal = "total";
        String sql = "SELECT TOTAL(" + RecordTable.COLUMN_AMOUNT + ") AS " + columeTotal + " FROM "
        + RecordTable.TABLE_NAME + " WHERE " + where;
        Cursor cursor = db.rawQuery(sql, args);
        if (cursor != null && cursor.moveToFirst()) {
            float total = cursor.getFloat(cursor.getColumnIndexOrThrow("total"));
            cursor.close();
            return total;
        }
        
        return 0;
    }

    private float queryTotalByIo (String io) {
        String where = RecordTable.COLUMN_IO + "=?";
        String[] args = new String[] { io };
        
        return queryTotal(where, args);
    }

    public void deleteRecordById (long id) {
        SQLiteDatabase db = getWritableDatabase();
        int count = db.delete(RecordTable.TABLE_NAME, RecordTable.COLUMN_ID + "=?",
                new String[] { String.valueOf(id)});
        if (count > 0) {
            sObservable.notifyChange();
        }
    }

    public Cursor queryRecordsByCurrentMonth() {
        String where = RecordTable.COLUMN_DATE + " BETWEEN ? AND ? ";

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
        String where = RecordTable.COLUMN_ID + "=? ";
        String[] args = { String.valueOf(id)};

        Cursor c = queryRecord(where, args);
        Record record = getRecord(c);
        c.close();

        return record;
    }

    public int updateRecordById(Record record, long id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(9);
        values.put(RecordTable.COLUMN_AMOUNT, record.amount);
        values.put(RecordTable.COLUMN_DATE, record.date);
        values.put(RecordTable.COLUMN_IO, record.io);
        values.put(RecordTable.COLUMN_CONSUMER, queryMemberIdByName(record.member));
        values.put(RecordTable.COLUMN_NAME, record.name);
        values.put(RecordTable.COLUMN_PAYMENT, queryPaymentIdByName(record.payment));
        values.put(RecordTable.COLUMN_REMARK, record.remark);
        values.put(RecordTable.COLUMN_TYPE, queryTypeIdByName(record.type));
        return db.update(RecordTable.TABLE_NAME, values, RecordTable.COLUMN_ID + "=?",
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
        values.put(RecordTable.COLUMN_AMOUNT, amount);
        values.put(RecordTable.COLUMN_DATE, date);
        values.put(RecordTable.COLUMN_IO, io);
        values.put(RecordTable.COLUMN_CONSUMER, member);
        values.put(RecordTable.COLUMN_NAME, name);
        values.put(RecordTable.COLUMN_PAYMENT, payment);
        values.put(RecordTable.COLUMN_REMARK, remark);
        values.put(RecordTable.COLUMN_TYPE, type);
        return db.insert(RecordTable.TABLE_NAME, null, values);
    }

    public int queryPaymentIdByName(String payment) {
        if (Utils.isEmpty(payment)) {
            return -1;
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(PaymentTable.TABLE_NAME, new String[] {
            PaymentTable.COLUMN_ID
        }, PaymentTable.COLUMN_NAME + "=?", new String[] {
            payment
        }, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndexOrThrow(PaymentTable.COLUMN_ID));
        } else {
            return -1;
        }
    }

    public int queryTypeIdByName(String type) {
        if (Utils.isEmpty(type)) {
            return -1;
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TypeTable.TABLE_NAME, new String[] {
            TypeTable.COLUMN_ID
        }, TypeTable.COLUMN_NAME + "=?", new String[] {
            type
        }, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndexOrThrow(TypeTable.COLUMN_ID));
        } else {
            return -1;
        }
    }

    public int queryMemberIdByName(String member) {
        if (Utils.isEmpty(member)) {
            return -1;
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(MemberTable.TABLE_NAME, new String[] {
            MemberTable.COLUMN_ID
        }, MemberTable.COLUMN_NAME + "=?", new String[] {
            member
        }, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndexOrThrow(MemberTable.COLUMN_ID));
        } else {
            return -1;
        }
    }

    public Cursor queryPayment() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(PaymentTable.TABLE_NAME, new String[] {
                PaymentTable.COLUMN_ID + " AS _id ", PaymentTable.COLUMN_NAME
        }, null, null, null, null, null);
    }

    public Cursor queryRecordName() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(RecordTable.TABLE_NAME, new String[] {
                RecordTable.COLUMN_ID + " AS _id ", RecordTable.COLUMN_NAME
        }, null, null, null, null, null);
    }

    public String[] getPaymentNames() {
        Cursor cursor = queryPayment();
        String[] payment = null;
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getCount();
            payment = new String[count];
            int index = cursor.getColumnIndexOrThrow(PaymentTable.COLUMN_NAME);
            for (int i = 0; i < count; i++, cursor.moveToNext()) {
                payment[i] = cursor.getString(index);
            }
        }

        return payment;
    }

    public Cursor queryType() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TypeTable.TABLE_NAME, new String[] {
                TypeTable.COLUMN_ID + " AS _id ", TypeTable.COLUMN_NAME
        }, null, null, null, null, null);
    }

    public String[] getTypeNames() {
        Cursor cursor = queryType();
        String[] type = null;
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getCount();
            type = new String[count];
            int index = cursor.getColumnIndexOrThrow(TypeTable.COLUMN_NAME);
            for (int i = 0; i < count; i++, cursor.moveToNext()) {
                type[i] = cursor.getString(index);
            }
        }

        return type;
    }

    public Cursor queryMember() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(MemberTable.TABLE_NAME, new String[] {
                MemberTable.COLUMN_ID + " AS _id ", MemberTable.COLUMN_NAME
        }, null, null, null, null, null);
    }

    public String[] getMemberNames() {
        Cursor cursor = queryMember();
        String[] member = null;
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getCount();
            member = new String[count];
            int index = cursor.getColumnIndexOrThrow(MemberTable.COLUMN_NAME);
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
            record.name = c.getString(c.getColumnIndexOrThrow(RecordTable.COLUMN_NAME));
            record.amount = c.getFloat(c.getColumnIndexOrThrow(RecordTable.COLUMN_AMOUNT));
            record.io = c.getString(c.getColumnIndexOrThrow(RecordTable.COLUMN_IO));
            record.remark = c.getString(c.getColumnIndexOrThrow(RecordTable.COLUMN_REMARK));
            record.date = c.getLong(c.getColumnIndexOrThrow(RecordTable.COLUMN_DATE));
            record.type = c.getString(c.getColumnIndexOrThrow(TypeTable.COLUMN_NAME));
            record.payment = c.getString(c.getColumnIndexOrThrow(PaymentTable.COLUMN_NAME));
            record.member = c.getString(c.getColumnIndexOrThrow(MemberTable.COLUMN_NAME));
        }

        return record;
    }

}
