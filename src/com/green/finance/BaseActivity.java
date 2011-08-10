package com.green.finance;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class BaseActivity extends Activity {

    protected static final String INTENT_EXTRA_RECORD_ID = "intent-extra-record-id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
	}
}
