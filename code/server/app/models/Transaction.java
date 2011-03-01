package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Transaction extends Model {

    public Date accountingDate;
    public Date fixedDate;
    public Double amountIn;
    public Double amountOut;
    public String text;
    public String archiveRef;
    @ManyToOne
    public TransactionType type;
    @ManyToMany
    public Set<TransactionTag> tags = new HashSet<TransactionTag>();
}
