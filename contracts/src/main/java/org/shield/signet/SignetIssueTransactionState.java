package org.shield.signet;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;


@CordaSerializable
@BelongsToContract(SignetIssueTransactionContract.class)
public class SignetIssueTransactionState implements ContractState, Serializable {
    private UUID transactionId;
    private int timestamp;
    private Amount amount;
    private SignetAccountState source;
    private SignetAccountState escrow;
    private String signetConfirmationId;
    private IssueState state;

    @ConstructorForDeserialization
    public SignetIssueTransactionState(){
        // default constructor for deserialization
    }

    public SignetIssueTransactionState(UUID transactionId, int timestamp, Amount amount, SignetAccountState source, SignetAccountState escrow, String signetConfirmationId, IssueState state) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.amount = amount;
        this.source = source;
        this.escrow = escrow;
        this.signetConfirmationId = signetConfirmationId;
        this.state = state;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public SignetAccountState getSource() {
        return source;
    }

    public void setSource(SignetAccountState source) {
        this.source = source;
    }

    public SignetAccountState getEscrow() {
        return escrow;
    }

    public void setEscrow(SignetAccountState escrow) {
        this.escrow = escrow;
    }

    public String getSignetConfirmationId() {
        return signetConfirmationId;
    }

    public void setSignetConfirmationId(String signetConfirmationId) {
        this.signetConfirmationId = signetConfirmationId;
    }

    public IssueState getState() {
        return state;
    }

    public void setState(IssueState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignetIssueTransactionState that = (SignetIssueTransactionState) o;
        return Objects.equals(getTransactionId(), that.getTransactionId()) &&
            Objects.equals(getTimestamp(), that.getTimestamp()) &&
            Objects.equals(getAmount(), that.getAmount()) &&
            Objects.equals(getSource(), that.getSource()) &&
            Objects.equals(getEscrow(), that.getEscrow()) &&
            Objects.equals(getSignetConfirmationId(), that.getSignetConfirmationId()) &&
            getState() == that.getState();
    }



    @Override
    public int hashCode() {
        return Objects.hash(getTransactionId(), getTimestamp(), getAmount(), getSource(), getEscrow(), getSignetConfirmationId(), getState());
    }

    @Override
    public String toString() {
        return "SignetIssueTransactionState{" +
            "transactionId=" + transactionId +
            ", timestamp=" + timestamp +
            ", amount=" + amount +
            ", source=" + source +
            ", escrow=" + escrow +
            ", signetConfirmationId='" + signetConfirmationId + '\'' +
            ", state=" + state +
            '}';
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(source.getOwner(), escrow.getOwner());
    }


}
