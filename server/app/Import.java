import models.Transaction;
import models.TransactionTag;
import models.User;
import org.apache.commons.io.IOUtils;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;
import play.vfs.VirtualFile;
import utils.FmtUtil;
import utils.ModelHelper;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * This job handles the inital import of transactions from CSV
 */
@OnApplicationStart
public class Import extends Job {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private static final String FIXTURES = "fixtures-local.yml";
    private static final String FIXTURES_CSV = "/conf/fixture-transactions-full.csv";
    private static final String FIELD_SEPARATOR = "_";
    private static final int FIELD_IDX_DATE = 0;
    private static final int FIELD_IDX_TEXT = 4;
    private static final int FIELD_IDX_AMOUNT = 5;
    private static final int FIELD_IDX_TAG = 7;

    @Override
    public void doJob() {
        if (Play.mode.isProd()) {
            loadUsersFromFixture();
            loadTransactionsFromCsv();
        }
    }

    /**
     * Load users from fixture
     */
    private void loadUsersFromFixture() {
        if (User.count() == 0) {
            Fixtures.load(FIXTURES);
        }
    }

    /**
     * Parse and load transactions from CSV
     */
    private void loadTransactionsFromCsv() {
        if (Transaction.count() == 0) {
            File f = VirtualFile.fromRelativePath(FIXTURES_CSV).getRealFile();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                String line;
                final List<User> allUsers = User.all().fetch();
                while ((line = reader.readLine()) != null) {
                    for (User user : allUsers) {
                        Transaction t = saveTransaction(line);
                        if (t != null) {
                            t.user = user;
                            t.save();
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error(e, "IOException");
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
    }

    /**
     * Parse transaction from line
     *
     * @param line Line
     * @return Transaction or null if parsing error occurs
     */
    private Transaction saveTransaction(String line) {
        final String[] s = line.split(FIELD_SEPARATOR);
        final double amount = Double.parseDouble(s[FIELD_IDX_AMOUNT]);
        if (amount == 0) { // Skip incoming transactions
            return null;
        }
        final Transaction t = new Transaction();
        t.date = parseDate(s[FIELD_IDX_DATE]);
        t.text = FmtUtil.trimTransactionText(s[FIELD_IDX_TEXT]).trim();
        t.amount = amount;
        if (!"null".equals(s[FIELD_IDX_TAG])) {
            final TransactionTag tag = new TransactionTag();
            tag.name = s[FIELD_IDX_TAG];
            t.tag = ModelHelper.saveOrUpdate(tag);
        }
        t.internal = false;
        t.dirty = false;
        t.timestamp = t.date.getTime();
        return t;
    }

    /**
     * Parse date
     *
     * @param s String to parse
     * @return Parsed date or the current date on parse error
     */
    private Date parseDate(String s) {
        try {
            return dateFormat.parse(s);
        } catch (ParseException e) {
            Logger.warn(e, "Failed to parse date: %s", s);
        }
        return new Date();
    }
}
