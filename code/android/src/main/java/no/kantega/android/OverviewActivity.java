package no.kantega.android;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import no.kantega.android.models.AggregatedTag;
import no.kantega.android.models.AverageConsumption;
import no.kantega.android.models.Transaction;
import no.kantega.android.utils.DatabaseHelper;
import no.kantega.android.utils.DatabaseOpenHelper;
import no.kantega.android.utils.FmtUtil;

import java.util.List;

public class OverviewActivity extends Activity {

    private static final String TAG = OverviewActivity.class.getSimpleName();
    private DatabaseHelper db;
    private AverageConsumption avg;
    private List<AggregatedTag> tags;
    private List<Transaction> transactions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overview);
        this.db = new DatabaseHelper(new DatabaseOpenHelper(
                getApplicationContext()).getReadableDatabase());
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                tags = db.getTags(3);
                transactions = db.getOrderedByDateDesc(1);
                avg = db.getAvg();
                runOnUiThread(populate);
            }
        }).start();
    }

    private Runnable populate = new Runnable() {
        @Override
        public void run() {
            populateCategories(tags);
            populateTransactions(transactions);
            populateAverageConsumption(avg);
        }
    };

    private void populateAverageConsumption(AverageConsumption avg) {
        TextView average_day = (TextView) findViewById(R.id.average_day);
        TextView average_week = (TextView) findViewById(R.id.average_week);
        average_day.setText(FmtUtil.currency(avg.getDay()));
        average_week.setText(FmtUtil.currency(avg.getWeek()));
    }

    private void clearTransactions() {
        TableLayout transactions = (TableLayout) findViewById(R.id.
                transactionTableLayout);
        int transaction_count = transactions.getChildCount() - 4;
        if (transactions.getChildAt(4) != null) {
            transactions.removeViews(4, transaction_count);
        }
    }

    private void populateTransactions(List<Transaction> transactions) {
        clearTransactions();
        for (Transaction t : transactions) {
            addTransaction(FmtUtil.date("yyyy-MM-dd",
                    t.getAccountingDate()), t.getType().getName(),
                    t.getTag().getName(),
                    FmtUtil.currency(t.getAmountOut()));
        }
    }

    private void addTransaction(String date, String text, String category,
                                String amount) {
        TableLayout transactions = (TableLayout) findViewById(R.id.
                transactionTableLayout);
        TableRow tr = new TableRow(this);
        TextView tv = new TextView(this);
        TableRow.LayoutParams tvParams = new TableRow.LayoutParams(0, TableRow.
                LayoutParams.WRAP_CONTENT,
                1f);
        tv.setText(date);
        tv.setLayoutParams(tvParams);
        tv.setTextColor(Color.WHITE);
        tr.addView(tv);
        tv = new TextView(this);
        tv.setText(text);
        tv.setLayoutParams(tvParams);
        tv.setTextColor(Color.WHITE);
        tr.addView(tv);
        tv = new TextView(this);
        tv.setText(category);
        tv.setLayoutParams(tvParams);
        tv.setTextColor(Color.WHITE);
        tr.addView(tv);
        tv = new TextView(this);
        tv.setText(amount);
        tv.setLayoutParams(tvParams);
        tv.setTextColor(Color.WHITE);
        tr.addView(tv);
        transactions.addView(tr, 4);
    }

    private void populateCategories(List<AggregatedTag> tags) {
        TextView category1 = (TextView) findViewById(R.id.top3_category_1);
        TextView category2 = (TextView) findViewById(R.id.top3_category_2);
        TextView category3 = (TextView) findViewById(R.id.top3_category_3);
        TextView amount1 = (TextView) findViewById(R.id.top3_amount_1);
        TextView amount2 = (TextView) findViewById(R.id.top3_amount_2);
        TextView amount3 = (TextView) findViewById(R.id.top3_amount_3);
        if (tags != null && tags.size() == 3) {
            category1.setText(tags.get(0).getName());
            amount1.setText(FmtUtil.currency(tags.get(0).getAmount()));
            category2.setText(tags.get(1).getName());
            amount2.setText(FmtUtil.currency(tags.get(1).getAmount()));
            category3.setText(tags.get(2).getName());
            amount3.setText(FmtUtil.currency(tags.get(2).getAmount()));
        }
    }
}
