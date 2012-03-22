package com.green.finance.handler;

import java.lang.ref.WeakReference;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import com.green.finance.database.DatabaseHelper;

public class DataHandlerService extends Service implements Handler.Callback {

    private static final String TAG = DataHandlerService.class.getSimpleName();

    public static final int QUERY_ALL_RECORD = 1;
    public static final int QUERY_INCOME = 2;
    public static final int QUERY_IOCOME = 3;

    private final IBinder mBinder = new LocalBinder();
    private Handler mHandler;
    private DatabaseHelper mDatabaseHelper;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("data_handler");
        thread.start();
        mHandler = new Handler(thread.getLooper(), this);
        mDatabaseHelper = DatabaseHelper.getInstance();
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public DataHandlerService getService() {
            return DataHandlerService.this;
        }
    }

    /**
     * It is the main entry to launch the task, which is worked asynchronize. 
     * @param taskId The task which you will launch.
     * @param callback The callback which will return the result.
     */
    public void startTask(int taskId, Callback<?> callback) {
        // Because of the life-cycle of DataHandler is almost like the application, for prevent the
        // activity memory leak we just use the weak reference.
        WeakReference<Callback<?>> ref = new WeakReference<Callback<?>>(callback);
        mHandler.obtainMessage(taskId, ref).sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case QUERY_ALL_RECORD:
                // Get the data;
                final Cursor cursor = mDatabaseHelper.queryAllRecords();
                // Return the data result;
                returnResult(cursor, msg.obj);
                return true;
            case QUERY_IOCOME:
                // Get the income and outcome;
                final float income = mDatabaseHelper.queryTotalIncome();
                final float outcome = mDatabaseHelper.queryTotalOutcome();
                // Return the data result;
                Pair<Float, Float> io = new Pair<Float, Float>(income, outcome);
                returnResult(io, msg.obj);
                return true;
            default:
                Log.w(TAG, "Unsupport task, id = " + msg.what);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> void returnResult(T result, Object obj) {
        WeakReference<Callback<T>> refCallback = (WeakReference<Callback<T>>) obj;
        Callback<T> callback = refCallback.get();
        if (callback != null) {
            callback.onComplete(QUERY_ALL_RECORD, result);
        }
    }

    /**
     * The callback is used to return the result from callee to caller; 
     */
    public interface Callback <T> {

        /**
         * The result will be return to caller when call finish;
         * @param callId The call id the caller set;
         * @param o The returned value;
         */
        public void onComplete(int callId, T value);
    }
}
