package com.green.finance.database;

import android.database.Observable;
import android.util.Log;

public class DatabaseObservable extends Observable<DatabaseObserver> {

    private static final String TAG = DatabaseObservable.class.getSimpleName();

    public void notifyChange () {
        Log.d(TAG, "notify change, observers count = " + mObservers.size());
        for (DatabaseObserver o : mObservers) {
            o.update();
        }
    }

}
