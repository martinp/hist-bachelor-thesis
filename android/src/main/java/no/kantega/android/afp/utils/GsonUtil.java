package no.kantega.android.afp.utils;

import android.util.Log;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import no.kantega.android.afp.models.Transaction;
import no.kantega.android.afp.models.TransactionTag;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class handles parsing of JSON data to native Java types
 */
public class GsonUtil {

    private static final String TAG = GsonUtil.class.getSimpleName();
    private static final String PORTABLE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final Gson gson;

    static {
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonElement json, Type typeOfT,
                                    JsonDeserializationContext context)
                    throws JsonParseException {
                SimpleDateFormat format = new SimpleDateFormat(
                        PORTABLE_DATE_FORMAT);
                try {
                    return format.parse(json.getAsJsonPrimitive().
                            getAsString());
                } catch (ParseException e) {
                    return null;
                }
            }
        });
        gson = builder.create();
    }

    /**
     * Parse transactions from an InputStream
     *
     * @param in Stream to read from
     * @return List of transactions
     */
    public static List<Transaction> toList(final InputStream in) {
        List<Transaction> transactions = new ArrayList<Transaction>();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(
                    in, "UTF-8"));
            reader.beginArray();
            while (reader.hasNext()) {
                Transaction t = gson.fromJson(reader,
                        Transaction.class);
                transactions.add(t);
            }
            reader.endArray();
            reader.close();
            return transactions;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "JsonSyntaxException", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Parse transactions from the given JSON
     *
     * @param json JSON value
     * @return List of transactions
     */
    public static List<Transaction> toList(final String json) {
        final Type type = new TypeToken<List<Transaction>>() {
        }.getType();
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "JsonSyntaxException", e);
        } catch (JsonParseException e) {
            Log.e(TAG, "JsonParseException", e);
        }
        return Collections.emptyList();
    }

    /**
     * Parse transaction tags from the given JSON
     *
     * @param json JSON value
     * @return Map of TransactionTags keyed on _id of the Transaction
     */
    public static Map<Integer, TransactionTag> toMap(final String json) {
        final Type type = new TypeToken<Map<Integer, TransactionTag>>() {
        }.getType();
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "JsonSyntaxException", e);
        } catch (JsonParseException e) {
            Log.e(TAG, "JsonParseException", e);
        }
        return Collections.emptyMap();
    }

    /**
     * Parse a generic map from JSON
     *
     * @param json JSON
     * @return Native data
     */
    public static List<Map<String, String>> toListOfMap(final String json) {
        final Type type = new TypeToken<List<Map<String, String>>>() {
        }.getType();
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "JsonSyntaxException", e);
        } catch (JsonParseException e) {
            Log.e(TAG, "JsonParseException", e);
        }
        return Collections.emptyList();
    }

    /**
     * Serialize to JSON using a portable date format
     *
     * @param o Object which should be serialized
     * @return JSON representation
     */
    public static String toJson(final Object o) {
        return new GsonBuilder().setDateFormat(PORTABLE_DATE_FORMAT).create().
                toJson(o);
    }
}
