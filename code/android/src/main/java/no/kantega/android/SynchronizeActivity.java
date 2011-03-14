package no.kantega.android;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import no.kantega.android.controllers.Transactions;
import no.kantega.android.models.Transaction;
import no.kantega.android.utils.GsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class SynchronizeActivity extends Activity {

    private static final String TAG = SynchronizeActivity.class.getSimpleName();
    private static final int PROGRESS_DIALOG = 0;
    private Transactions db;
    private ProgressDialog progressDialog;

    /**
     * Called when the activity is starting. Attaches click listeners and
     * creates a database handle.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.synchronize);
        Button syncButton = (Button) findViewById(R.id.syncButton);
        syncButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(PROGRESS_DIALOG);
            }
        });
        db = new Transactions(getApplicationContext());
    }

    /**
     * Called when a dialog is created. Configures the progress dialog.
     *
     * @param id
     * @return The configured dialog
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG: {
                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(getResources().getString(
                        R.string.wait));
                progressDialog.setCancelable(false);
                progressDialog.setProgressStyle(ProgressDialog.
                        STYLE_HORIZONTAL);
                return progressDialog;
            }
            default: {
                return null;
            }
        }
    }

    /**
     * Called when preparing the dialog.
     *
     * @param id
     * @param dialog
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case PROGRESS_DIALOG: {
                progressDialog.setProgress(0);
                populateDatabase();
            }
        }
    }

    /**
     * Read URL from properties file and start a task that populates the
     * database
     */
    private void populateDatabase() {
        try {
            InputStream inputStream = getAssets().open("url.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            new TransactionsTask().execute(
                    properties.get("allTransactions").toString());
        } catch (IOException e) {
            Log.e(TAG, "Could not read properties file", e);
        }
    }

    /**
     * Handler that updates the progress dialog
     */
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            progressDialog.setProgress(msg.arg1);
        }
    };

    /**
     * Task that retrieves transactions, deserializes them from JSON and inserts
     * them into the local database
     */
    private class TransactionsTask
            extends AsyncTask<String, Integer, List<Transaction>> {

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected List<Transaction> doInBackground(String... urls) {
            List<Transaction> transactions = GsonUtil.parseTransactions(
                    GsonUtil.getBody(urls[0]));
            if (transactions != null && !transactions.isEmpty()) {
                progressDialog.setMax(transactions.size());
                db.emptyTables();
                int i = 0;
                for (Transaction t : transactions) {
                    db.add(t);
                    publishProgress(++i);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            final Message msg = handler.obtainMessage();
            msg.arg1 = values[0];
            handler.sendMessage(msg);
        }

        @Override
        protected void onPostExecute(List<Transaction> transactions) {
            progressDialog.dismiss();
        }
    }
}