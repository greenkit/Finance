package com.green.finance;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class BaseActivity extends Activity {

    static final String INTENT_EXTRA_RECORD_ID = "intent-extra-record-id";
    static final String INTENT_ACTION_DATA_HANDLER_SERVICE = "android.intent.action.data.handler";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    }
}
