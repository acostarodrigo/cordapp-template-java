package org.shield.fiat;

import com.google.gson.JsonObject;
import net.corda.core.contracts.Amount;
import net.corda.core.serialization.CordaSerializable;

import java.time.Instant;

@CordaSerializable
public class FiatTransaction {
    private long timestamp;
    private String description;
    private Type type;
    private Amount amount;
    private long balance;
    private Action action;

    @CordaSerializable
    public enum Action {IN,OUT}

    @CordaSerializable
    public enum Type {DEPOSIT, SETTLEMENT}

    public FiatTransaction(long timestamp, String description, Type type, Amount amount, long balance, Action action) {
        this.timestamp = timestamp;
        this.description = description;
        this.type = type;
        this.amount = amount;
        this.balance = balance;
        this.action = action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
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

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("timestamp", Instant.ofEpochSecond(timestamp).toString());
        jsonObject.addProperty("description", description);
        jsonObject.addProperty("type", type.toString());
        jsonObject.addProperty("amount", amount.getQuantity());
        jsonObject.addProperty("currency", amount.getToken().toString());
        jsonObject.addProperty("balance", String.valueOf(balance));
        jsonObject.addProperty("action", action.toString());

        return jsonObject;
    }
}
