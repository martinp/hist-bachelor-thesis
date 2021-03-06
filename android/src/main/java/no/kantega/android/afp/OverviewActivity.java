package no.kantega.android.afp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import no.kantega.android.afp.controllers.Transactions;
import no.kantega.android.afp.models.TransactionTag;
import no.kantega.android.afp.utils.FmtUtil;
import no.kantega.android.afp.utils.Register;
import no.kantega.android.afp.utils.ResourceHelper;

import java.util.Calendar;

/**
 * This activity displays a general overview of the current transactions
 */
public class OverviewActivity extends ListActivity {

    private static final String TAG = OverviewActivity.class.getSimpleName();
    private Transactions db;
    private CategoryAdapter adapter;
    private Cursor cursor;
    private static final int DATE_DIALOG_ID = 0;
    private int pickYear;
    private int pickMonth;
    private int pickDay;
    private Button pickDate;
    private String[] monthName;
    private final DatePickerDialog.OnDateSetListener mDateSetListener =
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
        this.pickYear = c.get(Calendar.YEAR);
        this.pickMonth = c.get(Calendar.MONTH);
        this.pickDay = c.get(Calendar.DAY_OF_MONTH);
        this.db = new Transactions(getApplicationContext());
        this.adapter = new CategoryAdapter(this, cursor);
        this.monthName = getResources().getStringArray(R.array.months);
        setListAdapter(adapter);
        this.pickDate = (Button) findViewById(R.id.button_overview_pickDate);
        this.pickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });
        Button minusButton = (Button) findViewById(R.id.button_overview_minus);
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pickMonth > 0) {
                    pickMonth -= 1;
                } else if (pickMonth == 0) {
                    pickYear -= 1;
                    pickMonth = 11;
                }
                updateDisplay();
            }
        });
        Button plusButton = (Button) findViewById(R.id.button_overview_plus);
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pickMonth < 11) {
                    pickMonth += 1;
                } else if (pickMonth == 11) {
                    pickYear += 1;
                    pickMonth = 0;
                }
                updateDisplay();
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
        // Add default tags
        if (db.getTagCount() == 0) {
            for (TransactionTag tag : ResourceHelper.getDefaultTags()) {
                db.add(tag);
            }
        }
    }

    /**
     * Update the selected datef
     */
    private void updateDisplay() {
        pickDate.setText(monthName[pickMonth] + " " + pickYear);
        onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.overviewmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menu_pie_chart:
                Intent i = new Intent(getApplicationContext(), PieChartActivity.class);
                i.putExtra("month", getMonth());
                i.putExtra("year", getYear());
                startActivity(i);
                break;
        }
        return true;
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

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DATE_DIALOG_ID:
                ((DatePickerDialog) dialog).updateDate(pickYear, pickMonth, pickDay);
                break;
        }
    }

    /**
     * The currently selected month
     *
     * @return Month
     */
    private String getMonth() {
        String month = String.valueOf(pickMonth + 1);
        if (month.length() < 2) {
            month = "0" + month;
        }
        return month;
    }

    /**
     * The currently selected year
     *
     * @return Year
     */
    private String getYear() {
        return String.valueOf(pickYear);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                cursor = db.getMergeCursorTags(getMonth(), getYear());
                runOnUiThread(handler);
            }
        }).start();
    }

    private final Runnable handler = new Runnable() {
        @Override
        public void run() {
            adapter.changeCursor(cursor);
            Log.d(TAG, "Changed to a new cursor");
        }
    };

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Object o = l.getItemAtPosition(position);
        if (o instanceof Cursor) {
            Cursor cursor = (Cursor) o;
            Intent intent;
            String tag;
            final int tagColumnIndex = cursor.getColumnIndex("tag");
            if (tagColumnIndex == -1) {
                tag = getResources().getString(R.string.total);
                intent = new Intent(getApplicationContext(), PieChartActivity.class);
            } else {
                tag = cursor.getString(cursor.getColumnIndex("tag"));
                intent = new Intent(getApplicationContext(), TransactionsPerTagActivity.class);
            }
            intent.putExtra("tag", tag);
            intent.putExtra("year", getYear());
            intent.putExtra("month", getMonth());
            startActivity(intent);
        }
    }

    /**
     * This adapter handles binding of views from the given cursor
     */
    private class CategoryAdapter extends CursorAdapter {

        private final int[] colors;

        /**
         * Construct a new adapter in the given application context
         *
         * @param context The application context
         * @param cursor  The cursor
         */
        public CategoryAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            this.colors = new int[]{context.getResources().getColor(R.color.row_bg_even),
                    context.getResources().getColor(R.color.row_bg_odd)};
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

        /**
         * Populate the view
         *
         * @param context Application context
         * @param view    The view
         * @param cursor  The cursor
         */
        private void populateView(Context context, View view, Cursor cursor) {
            view.setBackgroundColor(colors[cursor.getPosition() % colors.length]);
            final int tagColumnIndex = cursor.getColumnIndex("tag");
            String tag;
            Double consumption = cursor.getDouble(cursor.getColumnIndex("sum"));
            ImageView image = (ImageView) view.findViewById(R.id.overview_imageview_category);
            TextView tv_tag = (TextView) view.findViewById(R.id.overview_textview_tag);
            TextView tv_consumption = (TextView) view.findViewById(R.id.overview_textview_consumption);
            if (tagColumnIndex > -1) {
                tag = cursor.getString(tagColumnIndex);
            } else {
                tag = getResources().getString(R.string.total);
            }
            image.setImageDrawable(null);
            tv_tag.setText(null);
            tv_consumption.setText(null);
            if (tag != null) {
                tv_tag.setText(tag);
            } else {
                tv_tag.setText(R.string.not_tagged);
            }
            image.setImageDrawable(ResourceHelper.getImage(context, tag));
            if (consumption != null) {
                tv_consumption.setText(FmtUtil.currencyWithoutPrefix(consumption));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.closeCursor(cursor);
        db.close();
    }
}
