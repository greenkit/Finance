
package com.green.finance;

import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.green.finance.database.DatabaseHelper;
import com.green.finance.database.table.MemberTable;
import com.green.finance.database.table.PaymentTable;
import com.green.finance.database.table.RecordTable;
import com.green.finance.database.table.TypeTable;
import com.green.finance.datatype.Record;
import com.green.finance.utils.Utils;

public class RecordEditorActivity extends BaseActivity {

    static final String INTENT_ACTION_RECORD_INSERT = "com.green.finance.intent.action.record.insert";
    static final String INTENT_ACTION_RECORD_UPDATE = "com.green.finance.intent.action.record.update";

    static final long HANDLE_MESSAGE_SEARCH_DELAY = 500L;

    private AutoCompleteTextView mName;
    private EditText mAmount;
    private Spinner mType;
    private Spinner mPayment;
    private Spinner mMember;
    private Spinner mIO;
    private EditText mRemark;
    private DatePicker mDate;
    private Button mSubmit;
    private DatabaseHelper mDatabaseHelper;
    private QueryHandler mQueryHandler;
    private HandlerThread mHandlerThread;
    private AutoCompleteCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onPause() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
            mQueryHandler = null;
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("query");
            mHandlerThread.start();
            mQueryHandler = new QueryHandler(mHandlerThread.getLooper());
        }

        super.onResume();
    }

    private void init() {
        initUi();
        loadData();
    }

    private void loadData() {
        Intent intent = getIntent();
        String action = intent.getAction();
        long id = intent.getLongExtra(INTENT_EXTRA_RECORD_ID, -1);

        if (INTENT_ACTION_RECORD_INSERT.equals(action)) {
            // Do nothing;
        } else if (INTENT_ACTION_RECORD_UPDATE.equals(action)) {
            if (id > -1) {
                Record record = mDatabaseHelper.queryRecordById(id);
                if (record != null) {
                    Utils.setPositionByName(mType, mDatabaseHelper.getTypeNames(), record.type);
                    Utils.setPositionByName(mPayment, mDatabaseHelper.getPaymentNames(), record.payment);
                    Utils.setPositionByName(mMember, mDatabaseHelper.getMemberNames(), record.member);
                    Utils.setPositionByName(mIO, mDatabaseHelper.getIoName(), record.io);

                    mName.setText(record.name);
                    mAmount.setText(String.valueOf(record.amount));
                    mRemark.setText(record.remark);

                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(record.date);
                    mDate.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                } else {
                    Utils.showToast(this, "Record not existed, id=" + id);
                    finish();
                }
            } else {
                Utils.showToast(this, "Record not existed, id=" + id);
                finish();
            }
        } else {
            finish();
        }
    }

    private void initUi () {
        setContentView(R.layout.record_editor);
        mDatabaseHelper = DatabaseHelper.getInstance();
        mName = (AutoCompleteTextView)findViewById(R.id.edite_name);
        mAdapter = new AutoCompleteCursorAdapter(this, null);
        mName.setAdapter(mAdapter);
        mName.addTextChangedListener(mTextWatcher);

        mAmount = (EditText)findViewById(R.id.edite_amount);

        mType = (Spinner)findViewById(R.id.spinner_type);
        SimpleCursorAdapter typeAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, mDatabaseHelper.queryType(), new String[] {
                    TypeTable.COLUMN_NAME
                }, new int[] {
                    android.R.id.text1
                });
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mType.setAdapter(typeAdapter);

        mPayment = (Spinner)findViewById(R.id.spinner_payment);
        SimpleCursorAdapter paymentAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, mDatabaseHelper.queryPayment(), new String[] {
                    PaymentTable.COLUMN_NAME
                }, new int[] {
                    android.R.id.text1
                });
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPayment.setAdapter(paymentAdapter);

        mMember = (Spinner)findViewById(R.id.spinner_member);
        SimpleCursorAdapter memberAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, mDatabaseHelper.queryMember(), new String[] {
                    MemberTable.COLUMN_NAME
                }, new int[] {
                    android.R.id.text1
                });
        memberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMember.setAdapter(memberAdapter);

        mIO = (Spinner)findViewById(R.id.spinner_io);
        mRemark = (EditText)findViewById(R.id.edite_remark);
        mDate = (DatePicker)findViewById(R.id.date_picker);
        mSubmit = (Button)findViewById(R.id.submit);
        mSubmit.setOnClickListener(mSubmitListener);
    }

    private OnClickListener mSubmitListener = new OnClickListener() {

        public void onClick(View v) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(mDate.getYear(), mDate.getMonth(), mDate.getDayOfMonth());

            Record record = new Record();
            record.name = mName.getText().toString();
            String amount = mAmount.getText().toString();
            record.amount = Float.valueOf(Utils.isEmpty(amount) ? "0" : amount);
            record.type = ((TextView)mType.getSelectedView()).getText().toString();
            record.remark = mRemark.getText().toString();
            record.date = calendar.getTimeInMillis();
            record.payment = ((TextView)mPayment.getSelectedView()).getText().toString();
            record.member = ((TextView)mMember.getSelectedView()).getText().toString();
            record.io = ((TextView)mIO.getSelectedView()).getText().toString();

            Intent intent = getIntent();
            String action = intent.getAction();
            long id = intent.getLongExtra(INTENT_EXTRA_RECORD_ID, -1);

            if (INTENT_ACTION_RECORD_INSERT.equals(action)) {
                if (mDatabaseHelper.insertRecord(record) > 0) {
                    Utils.showToast(RecordEditorActivity.this, "Add record succeeded!");
                    finish();
                } else {
                    Utils.showToast(RecordEditorActivity.this, "Add record failed!");
                }
            } else if (INTENT_ACTION_RECORD_UPDATE.equals(action) && id > -1) {
                if (mDatabaseHelper.updateRecordById(record, id) > 0) {
                    Utils.showToast(RecordEditorActivity.this, "Update record succeeded!");
                    finish();
                } else {
                    Utils.showToast(RecordEditorActivity.this, "Update record failed!");
                }
            }
        }
    };

    private static class AutoCompleteCursorAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public AutoCompleteCursorAdapter(Context context, Cursor c) {
            super(context, c);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_NAME));
            ((TextView) view).setText(name);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup view) {
            return mInflater.inflate(R.layout.auto_complete_list_item, null);
        }

        @Override
        public CharSequence convertToString(Cursor cursor) {
            if (cursor != null && cursor.getCount() > 0) {
                return cursor.getString(cursor.getColumnIndex(RecordTable.COLUMN_NAME));
            }

            return null;
        }
    }

    private TextWatcher mTextWatcher = new TextWatcher() {

        private String mCurrentText;
        private Message mLastMessage;

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            // Do nothing;
        }

        @Override
        public void beforeTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
            mCurrentText = text.toString().trim();
        }

        @Override
        public void afterTextChanged(Editable text) {
            String key = text.toString().trim();
            if (!Utils.isEmpty(key) && !key.equals(mCurrentText)) {
                // If the interval of user input is less than the default value,
                // the prior query will be canceled.
                // Otherwise, do query.
                if (mLastMessage != null && SystemClock.uptimeMillis() - mLastMessage.getWhen()
                        < HANDLE_MESSAGE_SEARCH_DELAY) {
                    mQueryHandler.removeMessages(QueryHandler.HANDLE_MESSAGE_QUERY_RECORD);
                }

                mLastMessage = mQueryHandler.sendQueryMessageDelay(key, HANDLE_MESSAGE_SEARCH_DELAY);
            }
        }
    };

    private class QueryHandler extends Handler {
        private static final int HANDLE_MESSAGE_QUERY_RECORD = 1;

        public Message sendQueryMessageDelay(String key, long delay) {
            Message message = obtainMessage(QueryHandler.HANDLE_MESSAGE_QUERY_RECORD, key);
            sendMessageDelayed(message, delay);
            return message;
        }

        public QueryHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case HANDLE_MESSAGE_QUERY_RECORD:
                final Cursor cursor = mDatabaseHelper.queryRecordNameByKey(msg.obj.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.changeCursor(cursor);
                    }
                });

                break;
            }
        }
    }
}