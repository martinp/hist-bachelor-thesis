package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
public class Transaction extends Model {

    public int clientId;
    public Date accountingDate;
    public Date fixedDate;
    public double amountIn;
    public double amountOut;
    public String text;
    public String archiveRef;
    public boolean internal;
    public long timestamp;
    public boolean dirty;
    @ManyToOne
    public TransactionType type;
    @ManyToOne
    public TransactionTag tag;
}
