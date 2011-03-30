package no.kantega.android.afp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import no.kantega.android.afp.controllers.Transactions;
import no.kantega.android.afp.utils.Register;

import java.util.Calendar;

public class OverviewActivity extends ListActivity {

    private static final String TAG = OverviewActivity.class.getSimpleName();
    private static final String SENDER_ID = "androidafp@gmail.com";
    private Transactions db;
    private CategoryAdapter adapter;
    private Cursor cursor;

    private static final int DATE_DIALOG_ID = 0;
    private int pickYear;
    private int pickMonth;
    private int pickDay;
    private Button pickDate;

    private static final String[] monthName = {"Januar", "Februar", "Mars", "April",
            "Mai", "Juni", "Juli", "August", "September", "Oktober",
            "November", "Desember"};

    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                    pickYear = year;
                    pickMonth = monthOfYear;
                    pickDay = dayOfMonth;
                    updateDisplay();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overview);

        final Calendar c = Calendar.getInstance();
        pickYear = c.get(Calendar.YEAR);
        pickMonth = c.get(Calendar.MONTH);
        pickDay = c.get(Calendar.DAY_OF_MONTH);

        this.db = new Transactions(getApplicationContext());
        this.cursor = db.getCursorTags(getMonth(), getYear());
        this.adapter = new CategoryAdapter(this, cursor);
        setListAdapter(adapter);

        pickDate = (Button) findViewById(R.id.button_overview_pickDate);
        pickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });
        Button newTransactionButton = (Button) findViewById(R.id.button_new_transaction);
        newTransactionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent().setClass(getApplicationContext(), AddTransactionActivity.class);
                startActivity(intent);

            }
        });

        updateDisplay();
        Register.handleRegistration(getApplicationContext());
    }

    private void updateDisplay() {
        //pickDate.setText("Test");
        pickDate.setText(monthName[pickMonth] + " " + pickYear);

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this,
                        mDateSetListener,
                        pickYear, pickMonth, pickDay);
        }
        return null;
    }

    private String getMonth() {
        String month = String.valueOf(pickMonth + 1);
        if (month.length() < 2) {
            month = "0" + month;
        }
        return month;
    }

    private String getYear() {
        String year = String.valueOf(pickYear);
        return year;
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                cursor = db.getCursorTags(getMonth(), getYear());
                runOnUiThread(handler);
            }
        }).start();
    }

    private final Runnable handler = new Runnable() {
        @Override
        public void run() {
            // Change to a fresh cursor, the old one will be automatically closed
            Log.d(TAG, "Changed to a new cursor");
            adapter.changeCursor(cursor);
        }
    };

    private class CategoryAdapter extends CursorAdapter {

        public CategoryAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View view = inflater.inflate(R.layout.overviewcategoryrow, parent, false);
            populateView(context, view, cursor);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            populateView(context, view, cursor);
        }

        private void populateView(Context context, View view, Cursor cursor) {
            String tag = cursor.getString(cursor.getColumnIndex("tag"));
            String consumption = cursor.getString(cursor.getColumnIndex("sum"));

            ImageView image = (ImageView) view.findViewById(R.id.overview_imageview_category);
            TextView tv_tag = (TextView) view.findViewById(R.id.overview_textview_tag);
            TextView tv_consumption = (TextView) view.findViewById(R.id.overview_textview_consumption);

            image.setImageDrawable(null);
            tv_tag.setText(null);
            tv_consumption.setText(null);

            if (tag != null) {
                tv_tag.setText(tag);
                image.setImageDrawable(getImageId(context, cursor));
            }

            if (consumption != null) {
                tv_consumption.setText(consumption);
            }
        }

        private Drawable getImageId(Context context, Cursor cursor) {
            final int imageId = cursor.getInt(cursor.getColumnIndex("imageId"));
            if (imageId > 0) {
                return context.getResources().getDrawable(imageId);
            }
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
    }
}
