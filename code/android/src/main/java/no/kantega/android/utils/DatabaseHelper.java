package no.kantega.android.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import no.kantega.android.models.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();
    private static final String SQLITE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private SQLiteDatabase db;

    public DatabaseHelper(SQLiteDatabase db) {
        this.db = db;
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    public void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    private boolean rowExists(String table, String columName, String param) {
        final Cursor cursor = db.query(table,
                new String[]{"1"}, columName + " = ?", new String[]{param},
                null, null, null, "1");
        cursor.moveToFirst();
        return cursor.getCount() > 0;
    }

    private long insertType(final TransactionType t) {
        if (!rowExists("transactiontype", "name", t.getName())) {
            final ContentValues values = new ContentValues();
            values.put("name", t.getName());
            db.insert("transactiontype", null, values);
        }
        Cursor cursor = db.query("transactiontype", new String[]{"id"},
                " name = ?", new String[]{t.getName()}, null, null, null, "1");
        cursor.moveToFirst();
        long typeId;
        if (cursor.getCount() > 0) {
            typeId = Long.parseLong(getValue(cursor, "id"));
        } else {
            typeId = -1;
        }
        return typeId;
    }

    private long insertTag(final TransactionTag t) {
        if (!rowExists("transactiontag", "name", t.getName())) {
            final ContentValues values = new ContentValues();
            values.put("name", t.getName());
            db.insert("transactiontag", null, values);
        }
        Cursor cursor = db.query("transactiontag", new String[]{"id"},
                " name = ?", new String[]{t.getName()}, null, null, null, "1");
        cursor.moveToFirst();
        long tagId;
        if (cursor.getCount() > 0) {
            tagId = Long.parseLong(getValue(cursor, "id"));
        } else {
            tagId = -1;
        }
        return tagId;
    }

    public void insert(final Transaction t) {
        final long typeId = insertType(t.getType());
        final long tagId = insertTag(t.getTag());
        final ContentValues values = new ContentValues();
        values.put("accountingdate", FmtUtil.date(SQLITE_DATE_FORMAT,
                t.getAccountingDate()));
        values.put("amountin", t.getAmountIn());
        values.put("amountout", t.getAmountOut());
        values.put("archiveref", t.getArchiveRef());
        values.put("fixeddate", FmtUtil.date(SQLITE_DATE_FORMAT,
                t.getFixedDate()));
        values.put("text", t.getText());
        values.put("type_id", typeId);
        values.put("tag_id", tagId);
        final long transactionId = db.insert("\"transaction\"", null, values);
        Log.d(TAG, "Inserted transaction with ID: " + transactionId);
    }

    private String getValue(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    public void emptyTables() {
        db.execSQL("DELETE FROM \"transaction\"");
        db.execSQL("DELETE FROM \"transactiontag\"");
        db.execSQL("DELETE FROM \"transactiontype\"");
    }

    public List<Transaction> getOrderedByDateDesc(int limit) {
        final Cursor cursor = db.query(
                "\"transaction\" " +
                        "INNER JOIN \"transactiontype\" " +
                        "ON transactiontype.id = \"transaction\".type_id " +
                        "INNER JOIN transactiontag " +
                        "ON transactiontag.id = \"transaction\".tag_id"
                , new String[]{"*", "transactiontype.name as type",
                        "transactiontag.name as tag"}, null, null,
                null, null, "accountingdate DESC", String.valueOf(limit));
        final List<Transaction> transactions = new ArrayList<Transaction>();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            do {
                Transaction t = new Transaction();
                t.setAccountingDate(FmtUtil.stringToDate(SQLITE_DATE_FORMAT,
                        getValue(cursor, "accountingdate")));
                t.setAmountIn(Double.parseDouble(getValue(cursor, "amountin")));
                t.setAmountOut(Double.parseDouble(getValue(cursor, "amountout")));
                t.setArchiveRef(getValue(cursor, "archiveref"));
                t.setFixedDate(FmtUtil.stringToDate(SQLITE_DATE_FORMAT,
                        getValue(cursor, "fixeddate")));
                t.setText(getValue(cursor, "text"));
                TransactionTag tag = new TransactionTag();
                tag.setName(getValue(cursor, "tag"));
                t.setTag(tag);
                TransactionType type = new TransactionType();
                type.setName(getValue(cursor, "type"));
                t.setType(type);
                transactions.add(t);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactions;
    }

    public List<AggregatedTag> getTags(final int limit) {
        final Cursor cursor = db.query("\"transaction\" " +
                "INNER JOIN transactiontag " +
                "ON transactiontag.id = \"transaction\".tag_id ",
                new String[]{"transactiontag.name",
                        "SUM(\"transaction\".amountout) AS sum"},
                null, null, "transactiontag.name", null, "sum DESC",
                String.valueOf(limit));
        final List<AggregatedTag> aggregatedTags =
                new ArrayList<AggregatedTag>();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            do {
                AggregatedTag at = new AggregatedTag();
                at.setAmount(Double.parseDouble(getValue(cursor, "sum")));
                at.setName(getValue(cursor, "name"));
                aggregatedTags.add(at);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return aggregatedTags;
    }

    public List<TransactionTag> getAllTags() {
        final Cursor cursor = db.query("\"transaction\"",
                new String[]{"transactiontag.name", "COUNT(*) AS count"},
                null, null, null, "count DESC", null);
        final List<TransactionTag> tags = new ArrayList<TransactionTag>();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            do {
                TransactionTag tag = new TransactionTag();
                tag.setName(getValue(cursor, "transactiontag.name"));
                tags.add(tag);
            } while (cursor.moveToNext());
        }
        return tags;
    }

    private Double getAvgDay() {
        if (DatabaseUtils.queryNumEntries(db, "\"transaction\"") == 0) {
            return 0D;
        }
        // Get start date
        Cursor cursor = db.query("\"transaction\"",
                new String[]{"accountingdate"}, null, null, null, null,
                "accountingDate ASC", "1");
        cursor.moveToFirst();
        final Date start = FmtUtil.stringToDate(SQLITE_DATE_FORMAT, getValue(
                cursor, "accountingdate"));
        cursor.close();
        // Get stop date
        cursor = db.query("\"transaction\"",
                new String[]{"accountingdate"}, null, null, null, null,
                "accountingDate DESC", "1");
        cursor.moveToFirst();
        final Date stop = FmtUtil.stringToDate(SQLITE_DATE_FORMAT, getValue(
                cursor, "accountingdate"));
        // Calculate number of days
        final int days =
                (int) ((stop.getTime() - start.getTime()) / 1000) / 86400;
        cursor.close();
        // Sum
        cursor = db.query("\"transaction\"",
                new String[]{"SUM(amountout) AS sum"}, null, null, null, null,
                null, "1");
        cursor.moveToFirst();
        return Double.parseDouble(getValue(cursor, "sum")) / days;
    }

    public AverageConsumption getAvg() {
        final double avgPerDay = getAvgDay();
        final AverageConsumption avg = new AverageConsumption();
        avg.setDay(avgPerDay);
        avg.setWeek(avgPerDay * 7);
        avg.setMonth(avgPerDay * 30.4368499);
        return avg;
    }
}
