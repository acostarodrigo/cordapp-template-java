package org.shield.custodian;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.offer.OfferState;
import org.shield.trade.TradeState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@BelongsToContract(CustodianContract.class)
public class CustodianState implements ContractState {
    private Party custodian;
    private Party issuer;
    private List<BondState> bonds;
    private List<TradeState> trades;
    private List<OfferState> offers;

    @ConstructorForDeserialization
    public CustodianState(){
        // initialize lists.
        this.bonds = new ArrayList<>();
        this.trades = new ArrayList<>();
        this.offers = new ArrayList<>();
    }

    public CustodianState(Party issuer, Party custodian){
        this();
        this.issuer = issuer;
        this.custodian = custodian;
    }

    public void addBond(BondState bond){
        if (bonds == null) bonds = new ArrayList<>();
        if (!bonds.contains(bond)) bonds.add(bond);
    }

    public void addOffer(OfferState offer){
        if (offers == null) offers = new ArrayList<>();
        if (!offers.contains(offer)) offers.add(offer);
    }

    public void addTrade(TradeState trade){
        if (trades == null) trades = new ArrayList<>();
        if (!trades.contains(trade)) trades.add(trade);
    }

    public Party getCustodian() {
        return custodian;
    }

    public void setCustodian(Party custodian) {
        this.custodian = custodian;
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

    @Override
    public String toString() {
        return "CustodianState{" +
            "custodian=" + custodian +
            ", issuer=" + issuer +
            ", bonds=" + bonds +
            ", trades=" + trades +
            ", offers=" + offers +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustodianState that = (CustodianState) o;
        return Objects.equals(getCustodian(), that.getCustodian()) &&
            Objects.equals(getIssuer(), that.getIssuer()) &&
            Objects.equals(getBonds(), that.getBonds()) &&
            Objects.equals(getTrades(), that.getTrades()) &&
            Objects.equals(getOffers(), that.getOffers());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCustodian(), getIssuer(), getBonds(), getTrades(), getOffers());
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(custodian, issuer);
    }
}
