
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
import android.util.Pair;
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
    protected void onPause() {
        super.onPause();
        DatabaseHelper helper = DatabaseHelper.peekInstance();
        if (helper != null) {
            helper.unregisterObserver(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseHelper helper = DatabaseHelper.getInstance();
        helper.registerObserver(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        startUpdateAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind the data handler service;
        unbindService(this);
        clear();
    }

    private void clear() {
        // Close the opened cursors;
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

        private class ViewHolder {
            TextView mName;
            TextView mAmount;
            TextView mTime;
            TextView mType;
        }

        public RecordListAdapter(Context context, Cursor c) {
            super(context, c);
            inflater = LayoutInflater.from(RecordListActivity.this);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder;
            if (view.getTag() == null) {
                holder = new ViewHolder();
                holder.mName = (TextView) view.findViewById(R.id.name);
                holder.mAmount = (TextView) view.findViewById(R.id.amount);
                holder.mTime = (TextView) view.findViewById(R.id.time);
                holder.mType = (TextView) view.findViewById(R.id.type);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.mName.setText(cursor.getString(cursor.getColumnIndexOrThrow(TableRecord.COLUMN_NAME)));
            holder.mAmount.setText(getString(R.string.total_money, cursor.getFloat(
                    cursor.getColumnIndexOrThrow(TableRecord.COLUMN_AMOUNT))));
            holder.mTime.setText(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(
                    TableRecord.COLUMN_DATE))).toLocaleString());
            holder.mType.setText(cursor.getString(cursor.getColumnIndexOrThrow(TableRecordType.COLUMN_NAME)));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(R.layout.record_list_item, parent, false);
        }
    }

    @Override
    public void update() {
        Log.d(TAG, "database changed, update UI");
        startUpdateAll();
    }

    /**
     * Update the record list UI;
     */
    private void updateRecordList(Cursor cursor) {
        if (mRecordListAdapter == null) {
            mRecordListAdapter = new RecordListAdapter(this, cursor);
            mRecordList.setAdapter(mRecordListAdapter);
        } else {
            mRecordListAdapter.changeCursor(cursor);
        }
    }

    /**
     * Update the income and outcome UI;
     */
    private void updateIOcome(final Pair<Float, Float> io) {
        // Update income UI;
        final float income = io.first;
        if (mTextTotalIncome != null) {
            mTextTotalIncome.setText(getString(R.string.total_money, income));
        }

        // Update outcome UI;
        final float outcome = io.second;
        if (mTextTotalOutcome != null) {
            mTextTotalOutcome.setText(getString(R.string.total_money, outcome));
        }
    }

    private void startUpdateRecordList() {
        mService.startTask(DataHandlerService.QUERY_ALL_RECORD, mCallbackGetAllRecord);
    }

    private void startUpdateIOcome() {
        mService.startTask(DataHandlerService.QUERY_IOCOME, mCallbackGetTotalIOcome);
    }

    private void startUpdateAll() {
        startUpdateRecordList();
        startUpdateIOcome();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        Log.d(TAG, "onServiceConnected()");
        mService = ((DataHandlerService.LocalBinder) binder).getService();
        // The first time service connect then update the all UI;
        startUpdateAll();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Do nothing;
    }

    private Callback<Pair<Float, Float>> mCallbackGetTotalIOcome = new Callback<Pair<Float, Float>>() {
        @Override
        public void onComplete(int callId, final Pair<Float, Float> io) {
            Log.d(TAG, "onComplete() : call id # " + callId);
            if(!isFinishing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateIOcome(io);
                    }
                });
            }
        }
    };

    private Callback<Cursor> mCallbackGetAllRecord = new Callback<Cursor>() {
        @Override
        public void onComplete(int callId, final Cursor cursor) {
            Log.d(TAG, "onComplete() : call id # " + callId);
            if(!isFinishing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateRecordList(cursor);
                    }
                });
            }
        }
    };
}
