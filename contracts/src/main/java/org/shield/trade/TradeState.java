package org.shield.trade;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.io.Serializable;
import java.util.*;

// *********
// * State *
// *********
@BelongsToContract(TradeContract.class)
public class TradeState implements ContractState, Serializable {
    public static final String externalKey = "org.shield.trade.TradeState";
    private UniqueIdentifier id;
    private Party issuer;
    private Party brokerDealer;
    private int size;
    private Date offeringDate;
    private State state;
    private UniqueIdentifier paperId;


    @CordaSerializable
    public static enum State {
        PREISSUE, ACCEPTED, CANCELLED, ISSUED
    }

    @ConstructorForDeserialization
    public TradeState() {
    }

    public TradeState(UniqueIdentifier id, Party issuer, Party brokerDealer, int size, Date offeringDate, State state) {
        this.id = id;
        this.issuer = issuer;
        this.brokerDealer = brokerDealer;
        this.size = size;
        this.offeringDate = offeringDate;
        this.state = state;
        this.paperId = null;
    }


    public UniqueIdentifier getId() {
        return id;
    }

    public void setId(UniqueIdentifier id) {
        this.id = id;
    }

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public Party getBrokerDealer() {
        return brokerDealer;
    }

    public void setBrokerDealer(Party brokerDealer) {
        this.brokerDealer = brokerDealer;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Date getOfferingDate() {
        return offeringDate;
    }

    public void setOfferingDate(Date offeringDate) {
        this.offeringDate = offeringDate;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public UniqueIdentifier getPaperId() {
        return paperId;
    }

    public void setPaperId(UniqueIdentifier paperId) {
        this.paperId = paperId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(this.issuer, this.brokerDealer);
    }

    @Override
    public String toString() {
        return "ArrangementState{" +
                "id=" + id +
                ", issuer=" + issuer +
                ", brokerDealer=" + brokerDealer +
                ", size=" + size +
                ", offeringDate=" + offeringDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeState that = (TradeState) o;
        return getSize() == that.getSize() &&
                getId().equals(that.getId()) &&
                getIssuer().equals(that.getIssuer()) &&
                getBrokerDealer().equals(that.getBrokerDealer()) &&
                getOfferingDate().equals(that.getOfferingDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getIssuer(), getBrokerDealer(), getSize(), getOfferingDate(), getState());
    }
}
