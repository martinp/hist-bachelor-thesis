import models.Transaction;
import models.TransactionTag;
import org.junit.Test;
import play.test.UnitTest;
import utils.GsonUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Test case for GsonUtil
 */
public class GsonUtilTest extends UnitTest {

    /**
     * Test makeJSON
     */
    @Test
    public void testMakeJSON() {
        String expected = "\"2010-01-01 00:00:00\"";
        String actual = GsonUtil.makeJSON(new Date(1262300400000L));
        assertEquals(expected, actual);
    }

    /**
     * Test parseTransactions
     */
    @Test
    public void testParseTransactions() {
        final String json = "[{\"date\":\"2009-04-15 00:00:00\"," +
                "\"amount\":1272.56," +
                "\"text\":\"456997107150**** 09.04 SEK 1550,00 CLAS OHLSON AB (49)\"," +
                "\"internal\":false," +
                "\"timestamp\":1239746400000," +
                "\"tag\":{\"name\":\"Datautstyr\",\"id\":4},\"id\":7}]";
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final Transaction t = new Transaction();
        try {
            t.date = sdf.parse("2009-04-15 00:00:00");
            t.amount = 1272.56;
            t.text = "456997107150**** 09.04 SEK 1550,00 CLAS OHLSON AB (49)";
            final TransactionTag tag = new TransactionTag();
            tag.id = 4L;
            tag.name = "Datautstyr";
            t.tag = tag;
            t.timestamp = 1239746400000L;
            t.internal = false;
            t.id = 7L;
        } catch (ParseException e) {
            assertTrue(false);
        }
        List<Transaction> expected = new ArrayList<Transaction>() {{
            add(t);
        }};
        List<Transaction> actual = GsonUtil.parseTransactions(json);
        assertEquals(expected, actual);
    }
}
