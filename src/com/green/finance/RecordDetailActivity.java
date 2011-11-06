
package com.green.finance;

import java.util.Date;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Gallery;
import android.widget.TextView;

import com.green.finance.database.DatabaseHelper;
import com.green.finance.datatype.Record;

public class RecordDetailActivity extends BaseActivity {

    private TextView mName;

    private TextView mAmount;

    private TextView mType;

    private TextView mPayment;

    private TextView mIo;

    private TextView mMember;

    private TextView mRemark;

    private Gallery mSnapshot;

    private TextView mDate;

    private long mRecordId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUi();
    }

    @Override
    protected void onResume() {
        loadData();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, R.string.menu_update, 0, R.string.menu_update);
        menu.add(0, R.string.menu_delete, 0, R.string.menu_delete);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.string.menu_update:
                Intent intent = new Intent(this, RecordEditorActivity.class);
                intent.setAction(RecordEditorActivity.INTENT_ACTION_RECORD_UPDATE);
                intent.putExtra(INTENT_EXTRA_RECORD_ID, mRecordId);
                startActivity(intent);
                return true;
            case R.string.menu_delete:
                DatabaseHelper.getInstance().deleteRecordById(mRecordId);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initUi() {
        setContentView(R.layout.record_detail);
        mName = (TextView)findViewById(R.id.name);
        mAmount = (TextView)findViewById(R.id.amount);
        mType = (TextView)findViewById(R.id.type);
        mPayment = (TextView)findViewById(R.id.payment);
        mIo = (TextView)findViewById(R.id.io);
        mMember = (TextView)findViewById(R.id.member);
        mRemark = (TextView)findViewById(R.id.remark);
        mSnapshot = (Gallery)findViewById(R.id.gallery_snapshot);
        mDate = (TextView)findViewById(R.id.date);
    }

    private void loadData () {
        Intent intent = getIntent();
        long id = intent.getLongExtra(INTENT_EXTRA_RECORD_ID, -1);

        // The first time come;
        if (id > -1) {
            mRecordId = id;
        }
        // The second time come;
        else if (mRecordId < 0) {
            finish();
        }

        Record record = DatabaseHelper.getInstance().queryRecordById(mRecordId);

        if (record != null) {
            mName.setText(record.name);
            mAmount.setText(getResources().getString(R.string.total_money, record.amount));
            mType.setText(record.type);
            mRemark.setText(record.remark);
            mPayment.setText(record.payment);
            mIo.setText(record.io);
            mMember.setText(record.member);
            mDate.setText(new Date(record.date).toLocaleString());
//            mSnapshot.setAdapter(adapter);
        }
    }
}
