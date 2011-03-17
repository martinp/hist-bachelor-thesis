package controllers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import models.*;
import play.db.jpa.JPA;
import play.mvc.Controller;
import utils.GsonUtil;

import javax.persistence.Query;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Transactions extends Controller {

    @SuppressWarnings("unchecked")
    public static void topTags(int count) {
        Query query = JPA.em().createQuery(
                "select tag.name, sum(t.amountOut) from Transaction t" +
                        " join t.tag as tag" +
                        " group by tag.name order by sum(t.amountOut) desc").
                setMaxResults(count);
        List<Object[]> result = query.getResultList();
        List<AggregatedTag> tags = new ArrayList<AggregatedTag>();
        for (Object[] o : result) {
            if (o.length != 2) {
                throw new IllegalArgumentException(
                        "Incorrect number of fields fetched");
            }
            AggregatedTag tag = new AggregatedTag();
            tag.setName(o[0].toString());
            tag.setAmount(Double.parseDouble(o[1].toString()));
            tags.add(tag);
        }
        renderJSON(tags);
    }

    private static Double avgDay() {
        Transaction t;
        t = Transaction.find("order by accountingDate asc").first();
        Date start = t.accountingDate;
        t = Transaction.find("order by accountingDate desc").first();
        Date stop = t.accountingDate;
        final int days =
                (int) ((stop.getTime() - start.getTime()) / 1000) / 86400;
        Query query = JPA.em().createQuery(
                "select sum(t.amountOut) from Transaction t");
        Object o = query.getSingleResult();
        return Double.parseDouble(o.toString()) / days;
    }

    public static void avg() {
        AverageConsumption ac = new AverageConsumption();
        ac.setDay(avgDay());
        ac.setWeek(avgDay() * 7);
        ac.setMonth(avgDay() * 30.4368499); // Google approves
        renderJSON(ac);
    }

    public static void transactions() {
        List<Transaction> transactions = Transaction.
                find("order by accountingDate desc, timestamp desc").fetch();
        String json = GsonUtil.renderJSONWithDateFmt("yyyy-MM-dd HH:mm:ss",
                transactions);
        renderJSON(json);
    }

    @SuppressWarnings("unchecked")
    public static void freshTransactions(Long timestamp) {
        Query query = JPA.em().createQuery("select t from Transaction t " +
                "where accountingDate > :date " +
                "and internal = :internal " +
                "order by accountingDate desc");
        query.setParameter("date", new Date(timestamp));
        query.setParameter("internal", false);
        List<Transaction> transactions = query.getResultList();
        String json = GsonUtil.renderJSONWithDateFmt("yyyy-MM-dd HH:mm:ss",
                transactions);
        renderJSON(json);
    }

    public static void save(JsonArray body) {
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonElement json, Type typeOfT,
                                    JsonDeserializationContext context)
                    throws JsonParseException {
                SimpleDateFormat format = new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss");
                try {
                    return format.parse(json.getAsJsonPrimitive().
                            getAsString());
                } catch (ParseException e) {
                    return null;
                }
            }
        });
        final Type listType = new TypeToken<List<Transaction>>() {
        }.getType();
        final List<Transaction> transactions = builder.create().fromJson(body, listType);
        List<Transaction> updated = new ArrayList<Transaction>();
        for (Transaction t : transactions) {
            if (t.dirty) {
                Transaction existing = Transaction.findById(t.id);
                if (existing != null) {
                    existing.accountingDate = t.accountingDate;
                    existing.fixedDate = t.fixedDate;
                    existing.amountIn = t.amountIn;
                    existing.amountOut = t.amountOut;
                    existing.text = t.text;
                    existing.archiveRef = t.archiveRef;
                    existing.internal = t.internal;
                    existing.timestamp = t.timestamp;
                    existing.dirty = false;
                    existing.tag = addOrSaveTag(t.tag.name);
                    existing.type = addOrSaveType(t.type.name);
                    existing.save();
                    updated.add(existing);
                } else {
                    t.id = null;
                    t.tag = addOrSaveTag(t.tag.name);
                    t.type = addOrSaveType(t.type.name);
                    t.dirty = false;
                    t.save();
                    updated.add(t);
                }
            }
        }
        renderJSON(GsonUtil.renderJSONWithDateFmt("yyyy-MM-dd HH:mm:ss",
                updated));
    }

    private static TransactionTag addOrSaveTag(String name) {
        TransactionTag tag = TransactionTag.find("name", name).first();
        if (tag != null) {
            return tag;
        } else {
            tag = new TransactionTag();
            tag.name = name;
            tag.save();
            return tag;
        }
    }

    private static TransactionType addOrSaveType(String name) {
        TransactionType type = TransactionType.find("name", name).first();
        if (type != null) {
            return type;
        } else {
            type = new TransactionType();
            type.name = name;
            type.save();
            return type;
        }
    }
}