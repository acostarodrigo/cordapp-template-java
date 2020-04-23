package org.shield.trade;

import com.google.gson.JsonObject;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;
import org.shield.offer.OfferState;

import java.io.Serializable;
import java.util.*;

@BelongsToContract(TradeContract.class)
@CordaSerializable
public class TradeState implements ContractState, Serializable {
    public static final String externalKey = "org.shield.trade.TradeState";
    private UniqueIdentifier id;
    private OfferState offer;
    private Date tradeDate;
    private Date settleDate;
    private Party issuer;
    private Party buyer;
    private Party seller;
    private float price;
    private float yield;
    private long size;
    private long proceeds;
    private Currency currency;
    private State state;
    private Date stateUpdate;
    private String arranger;

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(this.buyer, this.seller);
    }

    @ConstructorForDeserialization
    public TradeState(){
        // construtor for deserialization
    }

    public TradeState(UniqueIdentifier id, OfferState offer, Date tradeDate, Date settleDate, Party issuer, Party buyer, Party seller, String arranger, float price, float yield, long size, long proceeds, Currency currency, State state, Date stateUpdate) {
        this.id = id;
        this.offer = offer;
        this.tradeDate = tradeDate;
        this.settleDate = settleDate;
        this.issuer = issuer;
        this.buyer = buyer;
        this.seller = seller;
        this.arranger = arranger;
        this.price = price;
        this.yield = yield;
        this.size = size;
        this.proceeds = proceeds;
        this.currency = currency;
        this.state = state;
        this.stateUpdate = stateUpdate;
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

    public String getArranger() {
        return arranger;
    }

    public void setArranger(String arranger) {
        this.arranger = arranger;
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

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public Date getStateUpdate() {
        return stateUpdate;
    }

    public void setStateUpdate(Date stateUpdate) {
        this.stateUpdate = stateUpdate;
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
            Objects.equals(getTradeDate(), that.getTradeDate()) &&
            Objects.equals(getSettleDate(), that.getSettleDate()) &&
            Objects.equals(getIssuer(), that.getIssuer()) &&
            Objects.equals(getBuyer(), that.getBuyer()) &&
            Objects.equals(getSeller(), that.getSeller()) &&
            Objects.equals(getCurrency(), that.getCurrency()) &&
            getState() == that.getState() &&
            Objects.equals(getStateUpdate(), that.getStateUpdate()) &&
            Objects.equals(getArranger(), that.getArranger());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getOffer(), getTradeDate(), getSettleDate(), getIssuer(), getBuyer(), getSeller(), getPrice(), getYield(), getSize(), getProceeds(), getCurrency(), getState(), getStateUpdate(), getArranger());
    }

    @Override
    public String toString() {
        return "TradeState{" +
            "id=" + id +
            ", offer=" + offer +
            ", tradeDate=" + tradeDate +
            ", settleDate=" + settleDate +
            ", issuer=" + issuer +
            ", buyer=" + buyer +
            ", seller=" + seller +
            ", arranger=" + arranger +
            ", price=" + price +
            ", yield=" + yield +
            ", size=" + size +
            ", proceeds=" + proceeds +
            ", currency=" + currency +
            ", state=" + state +
            ", stateUpdate=" + stateUpdate +
            '}';
    }

    public JsonObject toJson(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id.getId().toString());
        jsonObject.add("offer", offer.toJson());
        jsonObject.addProperty("tradeDate",tradeDate.toString());
        jsonObject.addProperty("settleDate",settleDate.toString());
        jsonObject.addProperty("issuer",issuer.getName().toString());
        jsonObject.addProperty("buyer",buyer.getName().toString());
        jsonObject.addProperty("seller",seller.getName().toString());
        jsonObject.addProperty("arranger",arranger);
        jsonObject.addProperty("price",price);
        jsonObject.addProperty("yield",yield);
        jsonObject.addProperty("size",size);
        jsonObject.addProperty("proceeds",proceeds);
        jsonObject.addProperty("currency",currency.getCurrencyCode());
        jsonObject.addProperty("state",state.toString());
        jsonObject.addProperty("stateUpdate",stateUpdate.toString());

        return jsonObject;
    }
}
