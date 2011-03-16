package no.kantega.android;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import no.kantega.android.controllers.Transactions;
import no.kantega.android.models.Transaction;
import no.kantega.android.utils.FmtUtil;

import java.util.ArrayList;

public class TransactionsActivity extends ListActivity {

    private static final String TAG = OverviewActivity.class.getSimpleName();
    private Transactions db;
    private ProgressDialog progressDialog;
    private ArrayList<Transaction> transactions;
    private OrderAdapter listAdapter;
    private Runnable viewOrders;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transactions);
        this.db = new Transactions(getApplicationContext());
        transactions = new ArrayList<Transaction>();
        listAdapter = new OrderAdapter(this, R.layout.transactionrow, transactions);
        setListAdapter(listAdapter);
        

    }

    private void refreshList() {
        viewOrders = new Runnable() {
            @Override
            public void run() {
                getTransactions();
            }
        };
        Thread thread = new Thread(null, viewOrders, "MagentoBackground");
        thread.start();
        progressDialog = ProgressDialog.show(TransactionsActivity.this, "Please wait...", "Retrieving data ...",
                true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        long transactionCount = db.getCount();
        if (transactions.size() < transactionCount) {
            refreshList();

        }
    }

    private void getTransactions() {
        try {
            transactions = new ArrayList<Transaction>(db.get(1000));
            //Thread.sleep(2000);
            Log.i("ARRAY", "" + transactions.size());
        } catch (Exception e) {
            Log.e("BACKGROUND_PROC", e.getMessage());
        }
        runOnUiThread(returnRes);
    }

    private Runnable returnRes = new Runnable() {
        @Override
        public void run() {
            if (transactions != null && transactions.size() > 0) {
                listAdapter.clear();
                listAdapter.notifyDataSetChanged();
                for (int i = 0; i < transactions.size(); i++) {
                    listAdapter.add(transactions.get(i));
                }
            }
            progressDialog.dismiss();
            listAdapter.notifyDataSetChanged();
        }
    };

    private class OrderAdapter extends ArrayAdapter<Transaction> {

        private ArrayList<Transaction> items;

        public OrderAdapter(Context context, int textViewResourceId,
                            ArrayList<Transaction> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.transactionrow, null);
            }
            Transaction t = items.get(position);
            if (t != null) {
                TextView date = (TextView) v.findViewById(R.id.trow_tv_date);
                TextView text = (TextView) v.findViewById(R.id.trow_tv_text);
                TextView category = (TextView) v.findViewById(R.id.trow_tv_category);
                TextView amount = (TextView) v.findViewById(R.id.trow_tv_amount);
                if (date != null) {
                    date.setText(FmtUtil.dateToString("yyyy-MM-dd", t.getAccountingDate()));
                }
                if (text != null) {
                    text.setText(FmtUtil.trimTransactionText(t.getText()));
                }
                if (category != null) {
                    category.setText(t.getTag().getName());
                }
                if (amount != null) {
                    amount.setText(t.getAmountOut().toString());
                }
            }
            return v;
        }
    }
}
