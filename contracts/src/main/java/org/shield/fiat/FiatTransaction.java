package org.shield.fiat;

import com.google.gson.JsonObject;
import net.corda.core.contracts.Amount;
import net.corda.core.serialization.CordaSerializable;

import java.util.Date;

@CordaSerializable
public class FiatTransaction {
    private Date date;
    private String description;
    private String type;
    private Amount amount;
    private long balance;

    public FiatTransaction(Date date, String description, String type, Amount amount, long balance) {
        this.date = date;
        this.description = description;
        this.type = type;
        this.amount = amount;
        this.balance = balance;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "FiatTransaction{" +
            "date=" + date +
            ", description='" + description + '\'' +
            ", type='" + type + '\'' +
            ", amount=" + amount +
            ", balance=" + balance +
            '}';
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("date", date.toString());
        jsonObject.addProperty("description", description);
        jsonObject.addProperty("type", type);
        jsonObject.addProperty("amount", amount.getQuantity());
        jsonObject.addProperty("currency", amount.getToken().toString());
        jsonObject.addProperty("balance", String.valueOf(balance));

        return jsonObject;
    }
}
