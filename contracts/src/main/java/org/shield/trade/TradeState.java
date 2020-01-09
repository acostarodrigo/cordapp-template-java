package org.shield.trade;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.offer.OfferState;

import java.io.Serializable;
import java.util.*;

@BelongsToContract(TradeContract.class)
@CordaSerializable
public class TradeState implements ContractState, Serializable {
    public static final String externalKey = "org.shield.trade.TradeState";
    private UniqueIdentifier id;
    private OfferState offer;
    private BondState bond;
    private Date tradeDate;
    private Date settleDate;
    private Party buyer;
    private Party seller;
    private float price;
    private float yield;
    private long size;
    private long proceeds;
    private Currency currency;
    private State state;

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(this.buyer, this.seller);
    }

    @ConstructorForDeserialization
    public TradeState(){
        // construtor for deserialization
    }

    public TradeState(UniqueIdentifier id, OfferState offer, BondState bond, Date tradeDate, Date settleDate, Party buyer, Party seller, float price, float yield, long size, long proceeds, Currency currency, State state) {
        this.id = id;
        this.offer = offer;
        this.bond = bond;
        this.tradeDate = tradeDate;
        this.settleDate = settleDate;
        this.buyer = buyer;
        this.seller = seller;
        this.price = price;
        this.yield = yield;
        this.size = size;
        this.proceeds = proceeds;
        this.currency = currency;
        this.state = state;
    }


    public UniqueIdentifier getId() {
        return id;
    }

    public void setId(UniqueIdentifier id) {
        this.id = id;
    }

    public OfferState getOffer() {
        return offer;
    }

    public void setOffer(OfferState offer) {
        this.offer = offer;
    }

    public BondState getBond() {
        return bond;
    }

    public void setBond(BondState bond) {
        this.bond = bond;
    }

    public Date getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
    }

    public Date getSettleDate() {
        return settleDate;
    }

    public void setSettleDate(Date settleDate) {
        this.settleDate = settleDate;
    }

    public Party getBuyer() {
        return buyer;
    }

    public void setBuyer(Party buyer) {
        this.buyer = buyer;
    }

    public Party getSeller() {
        return seller;
    }

    public void setSeller(Party seller) {
        this.seller = seller;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public float getYield() {
        return yield;
    }

    public void setYield(float yield) {
        this.yield = yield;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getProceeds() {
        return proceeds;
    }

    public void setProceeds(long proceeds) {
        this.proceeds = proceeds;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeState that = (TradeState) o;
        return Float.compare(that.getPrice(), getPrice()) == 0 &&
            Float.compare(that.getYield(), getYield()) == 0 &&
            getSize() == that.getSize() &&
            getProceeds() == that.getProceeds() &&
            Objects.equals(getId(), that.getId()) &&
            Objects.equals(getOffer(), that.getOffer()) &&
            Objects.equals(getBond(), that.getBond()) &&
            Objects.equals(getTradeDate(), that.getTradeDate()) &&
            Objects.equals(getSettleDate(), that.getSettleDate()) &&
            Objects.equals(getBuyer(), that.getBuyer()) &&
            Objects.equals(getSeller(), that.getSeller()) &&
            Objects.equals(getCurrency(), that.getCurrency()) &&
            getState() == that.getState();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getOffer(), getBond(), getTradeDate(), getSettleDate(), getBuyer(), getSeller(), getPrice(), getYield(), getSize(), getProceeds(), getCurrency(), getState());
    }

    @Override
    public String toString() {
        return "TradeState{" +
            "id=" + id +
            ", offer=" + offer +
            ", bond=" + bond +
            ", tradeDate=" + tradeDate +
            ", settleDate=" + settleDate +
            ", buyer=" + buyer +
            ", seller=" + seller +
            ", price=" + price +
            ", yield=" + yield +
            ", size=" + size +
            ", proceeds=" + proceeds +
            ", currency=" + currency +
            ", state=" + state +
            '}';
    }
}
