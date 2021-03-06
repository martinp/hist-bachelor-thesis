package no.kantega.android.afp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import no.kantega.android.afp.adapters.TransactionsAdapter;
import no.kantega.android.afp.controllers.Transactions;
import no.kantega.android.afp.models.Transaction;
import no.kantega.android.afp.utils.GsonUtil;
import no.kantega.android.afp.utils.HttpUtil;
import no.kantega.android.afp.utils.Prefs;
import no.kantega.android.afp.utils.Register;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This activity handles incoming notifications from C2DM and retrieval of new transactions
 */
public class NotificationsActivity extends ListActivity {

    private static final String TAG = NotificationsActivity.class.
            getSimpleName();
    private static final int PROGRESS_DIALOG_ID = 0;
    private static final int ALERT_DIALOG_ID = 1;
    private ProgressDialog progressDialog;
    private SharedPreferences preferences;
    private Cursor cursor;
    private Transactions db;
    private TransactionsAdapter adapter;
    private long latestTimestamp;
    private TextView transactionsCount;
    private final Runnable adapterHandler = new Runnable() {
        @Override
        public void run() {
            adapter.changeCursor(cursor);
            Log.d(TAG, "Changed to a new cursor");
            transactionsCount.setText(String.format(getResources().getString(R.string.transaction_count),
                    adapter.getCount()));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transactions);
        this.db = new Transactions(getApplicationContext());
        this.adapter = new TransactionsAdapter(this, cursor);
        this.latestTimestamp = getLatestExternalTimestamp();
        this.transactionsCount = (TextView) findViewById(R.id.tv_transactioncount);
        setListAdapter(adapter);
        String url = Prefs.getProperties(getApplicationContext()).
                getProperty("newTransactions");
        new TransactionsTask().execute(url);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Object o = l.getItemAtPosition(position);
        if (o instanceof Cursor) {
            Cursor cursor = (Cursor) o;
            int transaction_id = cursor.getInt(cursor.getColumnIndex("_id"));
            Transaction t = db.getById(transaction_id);
            Intent intent;
            intent = new Intent(getApplicationContext(), EditTransactionActivity.class);
            intent.putExtra("transaction", t);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                cursor = db.getCursorAfterTimestamp(latestTimestamp);
                runOnUiThread(adapterHandler);
            }
        }).start();
        this.transactionsCount.setText(String.valueOf(adapter.getCount()));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG_ID: {
                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(getResources().getString(R.string.fetching_transactions));
                progressDialog.setCancelable(false);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                return progressDialog;
            }
            case ALERT_DIALOG_ID: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.server_unavailable)
                        .setCancelable(false)
                        .setPositiveButton(R.string.confirm,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.dismiss();
                                    }
                                });
                return builder.create();
            }
            default: {
                return null;
            }
        }
    }

    /**
     * This task handles retrieval of new transactions
     */
    private class TransactionsTask
            extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            showDialog(PROGRESS_DIALOG_ID);
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            return getTransactions(urls[0]);
        }

        /**
         * Retrieve transactions from server
         *
         * @param url URL for new transactions
         * @return True if successful
         */
        private boolean getTransactions(final String url) {
            final InputStream in = post(String.format(url, latestTimestamp),
                    new ArrayList<NameValuePair>());
            if (in == null) {
                return false;
            }
            final List<Transaction> transactions = GsonUtil.
                    toList(in);
            if (transactions.isEmpty()) {
                return false;
            }
            progressDialog.setMax(transactions.size());
            int i = 0;
            for (Transaction t : transactions) {
                db.add(t);
                publishProgress(++i);
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            dismissDialog(PROGRESS_DIALOG_ID);
            if (!success) {
                showDialog(ALERT_DIALOG_ID);
            }
            onResume();
        }
    }

    /**
     * Post values to the given URL with registration ID
     *
     * @param url    The URL
     * @param values The values
     * @return Body of the response
     */
    private InputStream post(String url, List<NameValuePair> values) {
        if (preferences == null) {
            preferences = Prefs.get(getApplicationContext());
        }
        values.add(new BasicNameValuePair("username",
                preferences.getString(Register.USERNAME_KEY, null)));
        return HttpUtil.post(url, values);
    }

    /**
     * Retrieve the timestamp of the latest external transaction
     *
     * @return Timestamp of the transaction or 0 if none is exist
     */
    private long getLatestExternalTimestamp() {
        final Transaction transaction = db.getLatestExternal();
        return transaction != null ? transaction.getTimestamp() : 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.closeCursor(cursor);
        db.close();
    }
}
