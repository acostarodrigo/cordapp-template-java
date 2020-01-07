package org.shield.offer;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@CordaSerializable
public class OfferState implements ContractState, Serializable {
    private UniqueIdentifier offerId;
    private BondState bond;
    private String ticker;
    private float offerPrice;
    private float offerYield;
    private int aggregatedTradeSize;
    private int afsSize;
    private boolean afs;
    private List<AbstractParty> participants;


    @ConstructorForDeserialization
    public OfferState(){
        // for deserialization only
    }

    public UniqueIdentifier getOfferId() {
        return offerId;
    }

    public void setOfferId(UniqueIdentifier offerId) {
        this.offerId = offerId;
    }

    public BondState getBond() {
        return bond;
    }

    public void setBond(BondState bond) {
        this.bond = bond;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public float getOfferPrice() {
        return offerPrice;
    }

    public void setOfferPrice(float offerPrice) {
        this.offerPrice = offerPrice;
    }

    public float getOfferYield() {
        return offerYield;
    }

    public void setOfferYield(float offerYield) {
        this.offerYield = offerYield;
    }

    public int getAggregatedTradeSize() {
        return aggregatedTradeSize;
    }

    public void setAggregatedTradeSize(int aggregatedTradeSize) {
        this.aggregatedTradeSize = aggregatedTradeSize;
    }

    public int getAfsSize() {
        return afsSize;
    }

    public void setAfsSize(int afsSize) {
        this.afsSize = afsSize;
    }

    public boolean isAfs() {
        return afs;
    }

    public void setAfs(boolean afs) {
        this.afs = afs;
    }

    public void setParticipants(List<AbstractParty> participants) {
        this.participants = participants;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfferState that = (OfferState) o;
        return Float.compare(that.getOfferPrice(), getOfferPrice()) == 0 &&
            Float.compare(that.getOfferYield(), getOfferYield()) == 0 &&
            getAggregatedTradeSize() == that.getAggregatedTradeSize() &&
            getAfsSize() == that.getAfsSize() &&
            isAfs() == that.isAfs() &&
            Objects.equals(getOfferId(), that.getOfferId()) &&
            Objects.equals(getBond(), that.getBond()) &&
            Objects.equals(getTicker(), that.getTicker()) &&
            Objects.equals(getParticipants(), that.getParticipants());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOfferId(), getBond(), getTicker(), getOfferPrice(), getOfferYield(), getAggregatedTradeSize(), getAfsSize(), isAfs(), getParticipants());
    }

    @Override
    public String toString() {
        return "OfferState{" +
            "offerId=" + offerId +
            ", bond=" + bond +
            ", ticker='" + ticker + '\'' +
            ", offerPrice=" + offerPrice +
            ", offerYield=" + offerYield +
            ", aggregatedTradeSize=" + aggregatedTradeSize +
            ", afsSize=" + afsSize +
            ", afs=" + afs +
            ", participants=" + participants +
            '}';
    }
}
