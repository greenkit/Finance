
package com.green.finance;

import java.util.Date;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.green.finance.database.DatabaseHelper;
import com.green.finance.database.DatabaseObserver;
import com.green.finance.database.table.TableRecord;
import com.green.finance.database.table.TableRecordType;
import com.green.finance.handler.DataHandlerService;
import com.green.finance.handler.DataHandlerService.Callback;

public class RecordListActivity extends BaseActivity
        implements DatabaseObserver, ServiceConnection {

    private static final String TAG = RecordListActivity.class.getSimpleName();

    private ListView mRecordList;

    private TextView mTextTotalIncome;

    private TextView mTextTotalOutcome;

    private RecordListAdapter mRecordListAdapter;

    private DataHandlerService mService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_list);
        bindService(new Intent(INTENT_ACTION_DATA_HANDLER_SERVICE), this, BIND_AUTO_CREATE);
        DatabaseHelper helper = DatabaseHelper.getInstance();
        helper.registerObserver(this);
        mRecordList = (ListView) findViewById(R.id.list);
        mTextTotalIncome = (TextView) findViewById(R.id.total_income);
        mTextTotalOutcome = (TextView) findViewById(R.id.total_outcome);
        mRecordList.setOnCreateContextMenuListener(this);
        mRecordList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchRecordDetailActivity(id);
            }
        });

        findViewById(R.id.add_record).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(RecordListActivity.this, RecordEditorActivity.class);
                intent.setAction(RecordEditorActivity.INTENT_ACTION_RECORD_INSERT);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        clear();
    }

    private void clear() {
        if (mRecordListAdapter != null) {
            Cursor cursor = mRecordListAdapter.getCursor();
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // menu.add(0, R.string.option_menu_statistics, 0,
        // R.string.option_menu_statistics);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // if (item.getItemId() == R.string.option_menu_statistics) {
        // Intent intent = new Intent(this, StatisticsActivity.class);
        // startActivity(intent);
        // return true;
        // }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, R.string.menu_view, 0, R.string.menu_view);
        menu.add(0, R.string.menu_update, 0, R.string.menu_update);
        menu.add(0, R.string.menu_delete, 1, R.string.menu_delete);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int itemId = item.getItemId();
        switch (itemId) {
            case R.string.menu_delete:
//                mDbHelper.deleteRecordById(info.id);
                return true;
            case R.string.menu_view:
                launchRecordDetailActivity(info.id);
                return true;
            case R.string.menu_update:
                Intent intent = new Intent(this, RecordEditorActivity.class);
                intent.setAction(RecordEditorActivity.INTENT_ACTION_RECORD_UPDATE);
                intent.putExtra(INTENT_EXTRA_RECORD_ID, info.id);
                startActivity(intent);
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void launchRecordDetailActivity(long id) {
        Intent intent = new Intent(this, RecordDetailActivity.class);
        intent.putExtra(INTENT_EXTRA_RECORD_ID, id);
        startActivity(intent);
    }

    private class RecordListAdapter extends CursorAdapter {

        private LayoutInflater inflater;

        public RecordListAdapter(Context context, Cursor c) {
            super(context, c);
            inflater = LayoutInflater.from(RecordListActivity.this);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView name = (TextView) view.findViewById(R.id.name);
            TextView amount = (TextView) view.findViewById(R.id.amount);
            TextView time = (TextView) view.findViewById(R.id.time);
            TextView type = (TextView) view.findViewById(R.id.type);

            name.setText(cursor.getString(cursor.getColumnIndexOrThrow(TableRecord.COLUMN_NAME)));
            amount.setText(getString(R.string.total_money, cursor.getFloat(
                    cursor.getColumnIndexOrThrow(TableRecord.COLUMN_AMOUNT))));
            time.setText(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(
                    TableRecord.COLUMN_DATE))).toLocaleString());
            type.setText(cursor.getString(cursor.getColumnIndexOrThrow(TableRecordType.COLUMN_NAME)));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(R.layout.record_list_item, parent, false);
        }
    }

    @Override
    public void update() {
        startUpdateRecordList();
    }

    private void updateRecordListUI(Cursor cursor) {
        if (mRecordListAdapter == null) {
            mRecordListAdapter = new RecordListAdapter(this, cursor);
            mRecordList.setAdapter(mRecordListAdapter);
        } else {
            mRecordListAdapter.changeCursor(cursor);
        }
    }

    private void startUpdateRecordList() {
        mService.startTask(DataHandlerService.QUERY_ALL_RECORD, mCallbackGetAllRecord);
    }

    private void startUpdateIncome() {
        mService.startTask(DataHandlerService.QUERY_INCOME, mCallbackGetTotalIncome);
    }

    private void startUpdateOutcome() {
        mService.startTask(DataHandlerService.QUERY_OUTCOME, mCallbackGetTotalOutcome);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        Log.d(TAG, "onServiceConnected()");
        mService = ((DataHandlerService.LocalBinder) binder).getService();
        // The first time service connect update the record list;
        startUpdateRecordList();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Do nothing;
    }

    private Callback<Cursor> mCallbackGetAllRecord = new Callback<Cursor>() {
        @Override
        public void onFinish(int callId, final Cursor cursor) {
            Log.d(TAG, "onFinish() : call id # " + callId);
            if(!isFinishing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateRecordListUI(cursor);
                        startUpdateIncome();
                        startUpdateOutcome();
                    }
                });
            }
        }
    };

    private final Callback<Float> mCallbackGetTotalOutcome = new Callback<Float>() {
        @Override
        public void onFinish(int callId, final Float value) {
            Log.d(TAG, "onFinish() : call id # " + callId);
            if(!isFinishing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mTextTotalOutcome != null) {
                            mTextTotalOutcome.setText(getString(R.string.total_money, value));
                        }
                    }
                });
            }
        }
    };

    private final Callback<Float> mCallbackGetTotalIncome = new Callback<Float>() {
        @Override
        public void onFinish(int callId, final Float value) {
            Log.d(TAG, "onFinish() : call id # " + callId);
            if(!isFinishing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mTextTotalIncome != null) {
                            mTextTotalIncome.setText(getString(R.string.total_money, value));
                        }
                    }
                });
            }
        }
    };
}
