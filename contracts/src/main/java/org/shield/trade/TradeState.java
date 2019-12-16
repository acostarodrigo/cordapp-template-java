package org.shield.trade;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

@BelongsToContract(TradeContract.class)
@CordaSerializable
public class TradeState implements ContractState, Serializable {
    public static final String externalKey = "org.shield.trade.TradeState";
    private UniqueIdentifier id;
    private UniqueIdentifier bondId;
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

    public TradeState(UniqueIdentifier bondId, Date tradeDate, Date settleDate, Party buyer, Party seller, float price, float yield, long size, long proceeds, Currency currency, State state) {
        this.id = new UniqueIdentifier();
        this.bondId = bondId;
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

    public static String getExternalKey() {
        return externalKey;
    }

    public UniqueIdentifier getId() {
        return id;
    }

    public UniqueIdentifier getBondId() {
        return bondId;
    }

    public void setBondId(UniqueIdentifier bondId) {
        this.bondId = bondId;
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
            Objects.equals(getBondId(), that.getBondId()) &&
            Objects.equals(getTradeDate(), that.getTradeDate()) &&
            Objects.equals(getSettleDate(), that.getSettleDate()) &&
            Objects.equals(getBuyer(), that.getBuyer()) &&
            Objects.equals(getSeller(), that.getSeller()) &&
            Objects.equals(getCurrency(), that.getCurrency()) &&
            getState() == that.getState();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getBondId(), getTradeDate(), getSettleDate(), getBuyer(), getSeller(), getPrice(), getYield(), getSize(), getProceeds(), getCurrency(), getState());
    }
}
