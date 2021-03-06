package no.kantega.android.afp.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import no.kantega.android.afp.R;
import no.kantega.android.afp.utils.FmtUtil;
import no.kantega.android.afp.utils.ResourceHelper;

import java.util.Date;

/**
 * This class is the default adapter for various transaction lists
 */
public class TransactionsAdapter extends CursorAdapter {

    private final int layoutId;
    private final int[] colors;

    /**
     * Initialize the adapter
     *
     * @param context Application context
     * @param c       Cursor
     */
    public TransactionsAdapter(Context context, Cursor c) {
        super(context, c);
        this.layoutId = R.layout.transactionrow;
        this.colors = new int[]{context.getResources().getColor(R.color.row_bg_even),
                context.getResources().getColor(R.color.row_bg_odd)};
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(layoutId, parent, false);
        populateView(context, view, getCursor());
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        populateView(context, view, getCursor());
    }

    /**
     * Populate view
     *
     * @param context Application context
     * @param view    View
     * @param cursor  Cursor
     */
    private void populateView(Context context, View view, Cursor cursor) {
        view.setBackgroundColor(colors[cursor.getPosition() % colors.length]);
        String date = cursor.getString(cursor.getColumnIndex("date"));
        String text = cursor.getString(cursor.getColumnIndex("text"));
        String tag = cursor.getString(cursor.getColumnIndex("tag"));
        Double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
        ImageView image = (ImageView) view.findViewById(R.id.tag_icon);
        TextView tv_date = (TextView) view.findViewById(R.id.trow_tv_date);
        TextView tv_text = (TextView) view.findViewById(R.id.trow_tv_text);
        TextView tv_tag = (TextView) view.findViewById(R.id.trow_tv_category);
        TextView tv_amount = (TextView) view.findViewById(R.id.trow_tv_amount);
        tv_date.setText(null);
        tv_text.setText(null);
        tv_tag.setText(null);
        image.setImageDrawable(null);
        tv_amount.setText(null);
        if (date != null) {
            Date d = FmtUtil.stringToDate("yyyy-MM-dd HH:mm:ss", date);
            tv_date.setText(FmtUtil.dateToString("dd. MMM, yyyy", d));
        }
        if (text != null) {
            tv_text.setText(text);
        }
        if (tag != null) {
            tv_tag.setText(tag);
        } else {
            tv_tag.setText(R.string.not_tagged);
        }
        image.setImageDrawable(ResourceHelper.getImage(context, tag));
        if (amount != null) {
            tv_amount.setText(FmtUtil.currencyWithoutPrefix(amount));
        }
    }
}
