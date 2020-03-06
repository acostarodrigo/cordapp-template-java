package org.shield.custodian;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.offer.OfferState;
import org.shield.trade.TradeState;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

@BelongsToContract(CustodianContract.class)
@CordaSerializable
public class CustodianState implements Serializable,ContractState {
    private List<Party> custodians;
    private Party issuer;
    private List<BondState> bonds;
    private List<TradeState> trades;
    private List<OfferState> offers;
    private Date lastUpdated;

    @ConstructorForDeserialization
    public CustodianState() {
    }

    public CustodianState(Party issuer, List<Party> custodians) {
        this.custodians = custodians;
        this.issuer = issuer;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> participants = new ArrayList<>(custodians);
        participants.add(issuer);
        return participants;
    }

    public List<Party> getCustodians() {
        return custodians;
    }

    public void setCustodians(List<Party> custodians) {
        this.custodians = custodians;
    }

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public List<BondState> getBonds() {
        return bonds;
    }

    public void setBonds(List<BondState> bonds) {
        this.bonds = bonds;
    }

    public List<TradeState> getTrades() {
        return trades;
    }

    public void setTrades(List<TradeState> trades) {
        this.trades = trades;
    }

    public List<OfferState> getOffers() {
        return offers;
    }

    public void setOffers(List<OfferState> offers) {
        this.offers = offers;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
