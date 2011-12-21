package com.green.finance;

import android.app.Application;

import com.green.finance.database.DatabaseHelper;

public class FinanceApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DatabaseHelper.init(getApplicationContext());
    }
}
