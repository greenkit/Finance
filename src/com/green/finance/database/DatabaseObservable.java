package com.green.finance.database;

import android.database.Observable;

public class DatabaseObservable extends Observable<DatabaseObserver> {

    public void notifyChange () {
        for (DatabaseObserver o : mObservers) {
            o.update();
        }
    }

}
