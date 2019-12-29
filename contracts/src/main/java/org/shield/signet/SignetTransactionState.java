package org.shield.signet;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@CordaSerializable
@BelongsToContract(SignetTransactionContract.class)
public class SignetTransactionState implements ContractState, Serializable {
    private UUID transactionId;
    private Timestamp timestamp;
    private Currency value;
    private String source;
    private String destination;

    public SignetTransactionState(UUID transactionId, Timestamp timestamp, Currency value, String source, String destination) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.value = value;
        this.source = source;
        this.destination = destination;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public Currency getValue() {
        return value;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignetTransactionState that = (SignetTransactionState) o;
        return Objects.equals(getTransactionId(), that.getTransactionId()) &&
            Objects.equals(getTimestamp(), that.getTimestamp()) &&
            Objects.equals(getValue(), that.getValue()) &&
            Objects.equals(getSource(), that.getSource()) &&
            Objects.equals(getDestination(), that.getDestination());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTransactionId(), getTimestamp(), getValue(), getSource(), getDestination());
    }
}
