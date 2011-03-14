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

    /**
     * Check if a row exists in the given table with columnName = param
     *
     * @param table
     * @param columName
     * @param param
     * @return True if row exists
     */
    private boolean rowExists(String table, String columName, String param) {
        final Cursor cursor = db.query(table,
                new String[]{"1"}, columName + " = ?", new String[]{param},
                null, null, null, "1");
        cursor.moveToFirst();
        return cursor.getCount() > 0;
    }

    /**
     * Insert a new transaction type while respecting the unique constraint
     *
     * @param t
     * @return The id of the newly inserted row, or id of the existing row
     */
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
            typeId = Long.parseLong(getStringValue(cursor, "id"));
        } else {
            typeId = -1;
        }
        cursor.close();
        return typeId;
    }

    /**
     * Insert a new transaction tag while respecting the unique constraint
     *
     * @param t
     * @return The id of the newly inserted row, or id of the existing row
     */
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
            tagId = Long.parseLong(getStringValue(cursor, "id"));
        } else {
            tagId = -1;
        }
        cursor.close();
        return tagId;
    }

    /**
     * Insert a new transaction into the database
     *
     * @param t
     */
    public void insert(final Transaction t) {
        final long typeId = insertType(t.getType());
        final long tagId = insertTag(t.getTag());
        final ContentValues values = new ContentValues();
        values.put("accountingdate", FmtUtil.dateToString(SQLITE_DATE_FORMAT,
                t.getAccountingDate()));
        values.put("amountin", t.getAmountIn());
        values.put("amountout", t.getAmountOut());
        values.put("archiveref", t.getArchiveRef());
        values.put("fixeddate", FmtUtil.dateToString(SQLITE_DATE_FORMAT,
                t.getFixedDate()));
        values.put("text", t.getText());
        values.put("type_id", typeId);
        values.put("tag_id", tagId);
        values.put("timestamp", t.getTimestamp());
        values.put("internal", t.getInternal() ? 1 : 0);
        final long transactionId = db.insert("\"transaction\"", null, values);
        Log.d(TAG, "Inserted transaction with ID: " + transactionId);
    }

    /**
     * Helper method for retrieving the value of the given column
     *
     * @param cursor
     * @param columnName
     * @return The string value
     */
    private String getStringValue(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    /**
     * Helper method for retrieving the integer value of the given column
     *
     * @param cursor
     * @param columnName
     * @return The integer value
     */
    private Integer getIntValue(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    /**
     * Empty all tables
     */
    public void emptyTables() {
        db.execSQL("DELETE FROM \"transaction\"");
        db.execSQL("DELETE FROM \"transactiontag\"");
        db.execSQL("DELETE FROM \"transactiontype\"");
    }

    /**
     * Get total number of transactions
     *
     * @return Number of transactions
     */
    public long getTransactionCount() {
        return DatabaseUtils.queryNumEntries(db, "\"transaction\"");
    }

    /**
     * Retrieve an limited ordered list of transactions
     *
     * @param limit
     * @return List of transactions
     */
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
                        getStringValue(cursor, "accountingdate")));
                t.setAmountIn(Double.parseDouble(getStringValue(cursor,
                        "amountin")));
                t.setAmountOut(Double.parseDouble(getStringValue(cursor,
                        "amountout")));
                t.setArchiveRef(getStringValue(cursor, "archiveref"));
                t.setFixedDate(FmtUtil.stringToDate(SQLITE_DATE_FORMAT,
                        getStringValue(cursor, "fixeddate")));
                t.setText(getStringValue(cursor, "text"));
                TransactionTag tag = new TransactionTag();
                tag.setName(getStringValue(cursor, "tag"));
                t.setTag(tag);
                TransactionType type = new TransactionType();
                type.setName(getStringValue(cursor, "type"));
                t.setType(type);
                t.setInternal(
                        getIntValue(cursor, "internal") == 1 ? true : false);
                t.setTimestamp(Long.parseLong(getStringValue(cursor,
                        "timestamp")));
                transactions.add(t);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactions;
    }

    /**
     * Retrieve a limited list of aggregated tags
     *
     * @param limit
     * @return List of aggregated tags
     */
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
                at.setAmount(Double.parseDouble(getStringValue(cursor, "sum")));
                at.setName(getStringValue(cursor, "name"));
                aggregatedTags.add(at);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return aggregatedTags;
    }

    /**
     * Retrieve a list of all transaction tags ordered by most used
     *
     * @return List of transaction tags
     */
    public List<TransactionTag> getAllTags() {
        final Cursor cursor = db.query("\"transaction\" " +
                "INNER JOIN transactiontag ON " +
                "transactiontag.id = \"transaction\".tag_id",
                new String[]{"transactiontag.name AS tag", "COUNT(*) AS count"},
                null, null, "tag", null, "count DESC", null);
        final List<TransactionTag> tags = new ArrayList<TransactionTag>();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            do {
                TransactionTag tag = new TransactionTag();
                tag.setName(getStringValue(cursor, "tag"));
                tags.add(tag);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tags;
    }

    /**
     * Calculate the average amount spent per day
     *
     * @return Average amount spent per day
     */
    private Double getAvgDay() {
        if (DatabaseUtils.queryNumEntries(db, "\"transaction\"") == 0) {
            return 0D;
        }
        // Get start date
        Cursor cursor = db.query("\"transaction\"",
                new String[]{"accountingdate"}, null, null, null, null,
                "accountingDate ASC", "1");
        cursor.moveToFirst();
        final Date start = FmtUtil.stringToDate(SQLITE_DATE_FORMAT, getStringValue(
                cursor, "accountingdate"));
        cursor.close();
        // Get stop date
        cursor = db.query("\"transaction\"",
                new String[]{"accountingdate"}, null, null, null, null,
                "accountingDate DESC", "1");
        cursor.moveToFirst();
        final Date stop = FmtUtil.stringToDate(SQLITE_DATE_FORMAT, getStringValue(
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
        Double avg = Double.parseDouble(getStringValue(cursor, "sum")) / days;
        cursor.close();
        return avg;
    }

    /**
     * Calculate average consumpetion per day, week and month
     *
     * @return Average consumption
     */
    public AverageConsumption getAvg() {
        final double avgPerDay = getAvgDay();
        final AverageConsumption avg = new AverageConsumption();
        avg.setDay(avgPerDay);
        avg.setWeek(avgPerDay * 7);
        avg.setMonth(avgPerDay * 30.4368499);
        return avg;
    }
}
