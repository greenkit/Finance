
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
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.green.finance.database.DatabaseHelper;
import com.green.finance.database.table.TableMember;
import com.green.finance.database.table.TablePayment;
import com.green.finance.database.table.TableRecord;
import com.green.finance.database.table.TableRecordType;
import com.green.finance.datatype.Record;
import com.green.finance.utils.Utils;

public class RecordEditorActivity extends BaseActivity {

    private final static String TAG = "RecordEditorActivity";
    static final String INTENT_ACTION_RECORD_INSERT = "com.green.finance.intent.action.record.insert";
    static final String INTENT_ACTION_RECORD_UPDATE = "com.green.finance.intent.action.record.update";

    private static final long MESSAGE_SEARCH_DELAY = 500L;

    private AutoCompleteTextView mName;
    private EditText mAmount;
    private AutoCompleteTextView mRecordType;
    private Spinner mPayment;
    private Spinner mMember;
    private Spinner mIO;
    private EditText mRemark;
    private DatePicker mDate;
    private Button mSubmit;
    private DatabaseHelper mDatabaseHelper;
    private QueryHandler mQueryHandler;
    private HandlerThread mHandlerThread;
    private NameAdapter mNameAdapter;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
            mQueryHandler = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("query");
            mHandlerThread.start();
            mQueryHandler = new QueryHandler(mHandlerThread.getLooper());
        }
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
            return;
        } else if (INTENT_ACTION_RECORD_UPDATE.equals(action)) {
            if (id > -1) {
                Record record = mDatabaseHelper.queryRecordById(id);
                if (record != null) {
                    Utils.setPositionByName(mPayment, mDatabaseHelper.getPaymentNames(), record.payment);
                    Utils.setPositionByName(mMember, mDatabaseHelper.getMemberNames(), record.member);
                    Utils.setPositionByName(mIO, mDatabaseHelper.getIoName(), record.io);

                    mName.setText(record.name);
                    mAmount.setText(String.valueOf(record.amount));
                    mRemark.setText(record.remark);

                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(record.date);
                    mDate.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                    return;
                }
            }
        }

        Log.d(TAG, "Record is not existed, id = " + id);
        finish();
    }

    private boolean mRecordTypeDropdownListShow;

    private void initUi () {
        setContentView(R.layout.record_editor);
        mInflater = LayoutInflater.from(this);
        mDatabaseHelper = DatabaseHelper.getInstance();
        mName = (AutoCompleteTextView)findViewById(R.id.edite_name);
        mNameAdapter = new NameAdapter(this, null);
        mName.setAdapter(mNameAdapter);
        mName.addTextChangedListener(mTextWatcher);

        mAmount = (EditText)findViewById(R.id.edite_amount);

        mRecordType = (AutoCompleteTextView)findViewById(R.id.record_type);
        mRecordType.setAdapter(new RecordTypeCursorAdapter(this, mDatabaseHelper.queryRecordType()));
        mRecordType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRecordTypeDropdownListShow) {
                    mRecordType.showDropDown();
                    mRecordTypeDropdownListShow = true;
                } else {
                    mRecordType.dismissDropDown();
                    mRecordTypeDropdownListShow = false;
                }
            }
        });
        mRecordType.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mRecordTypeDropdownListShow = false;
            }
        });

        mPayment = (Spinner)findViewById(R.id.spinner_payment);
        SimpleCursorAdapter paymentAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, mDatabaseHelper.queryPayment(), new String[] {
                    TablePayment.COLUMN_NAME
                }, new int[] {
                    android.R.id.text1
                });
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPayment.setAdapter(paymentAdapter);

        mMember = (Spinner)findViewById(R.id.spinner_member);
        SimpleCursorAdapter memberAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, mDatabaseHelper.queryMember(), new String[] {
                    TableMember.COLUMN_NAME
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

    private final class RecordTypeCursorAdapter extends CursorAdapter {

        public RecordTypeCursorAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView text = (TextView) view.findViewById(R.id.text);
            view.findViewById(R.id.button_delete).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: Implement the record type delete function.
                }
            });

            text.setText(cursor.getString(cursor.getColumnIndexOrThrow(
                    TableRecordType.COLUMN_NAME)));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.record_type_list_item, null, false);
        }

        @Override
        public CharSequence convertToString(Cursor cursor) {
            return cursor.getString(cursor.getColumnIndexOrThrow(TableRecordType.COLUMN_NAME));
        }
    }

    private OnClickListener mSubmitListener = new OnClickListener() {

        public void onClick(View v) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(mDate.getYear(), mDate.getMonth(), mDate.getDayOfMonth());

            Record record = new Record();
            record.name = mName.getText().toString();
            String amount = mAmount.getText().toString();
            record.amount = Float.valueOf(Utils.isEmpty(amount) ? "0" : amount);
            record.type = mRecordType.getText().toString();
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
                    finish();
                } else {
                    Utils.showToast(RecordEditorActivity.this, "Add record failed!");
                }
            } else if (INTENT_ACTION_RECORD_UPDATE.equals(action) && id > -1) {
                if (mDatabaseHelper.updateRecordById(record, id) > 0) {
                    finish();
                } else {
                    Utils.showToast(RecordEditorActivity.this, "Update record failed!");
                }
            }
        }
    };

    private class NameAdapter extends CursorAdapter {

        public NameAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(TableRecord.COLUMN_NAME));
            ((TextView) view).setText(name);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup view) {
            return mInflater.inflate(R.layout.auto_complete_list_item, null);
        }

        @Override
        public CharSequence convertToString(Cursor cursor) {
            if (cursor != null && cursor.getCount() > 0) {
                return cursor.getString(cursor.getColumnIndex(TableRecord.COLUMN_NAME));
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
            if (mQueryHandler == null) {
                return;
            }

            String key = text.toString().trim();
            if (!Utils.isEmpty(key) && !key.equals(mCurrentText)) {
                // If the interval of user input is less than the default value,
                // the prior query will be canceled.
                // Otherwise, do query.
                if (mLastMessage != null && SystemClock.uptimeMillis() - mLastMessage.getWhen()
                        < MESSAGE_SEARCH_DELAY) {
                    mQueryHandler.removeMessages(QueryHandler.HANDLE_MESSAGE_QUERY_RECORD);
                }

                mLastMessage = mQueryHandler.sendQueryMessageDelay(key, MESSAGE_SEARCH_DELAY);
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
                        mNameAdapter.changeCursor(cursor);
                    }
                });

                break;
            }
        }
    }
}