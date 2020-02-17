package org.shield.offer;

import com.google.gson.JsonObject;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@BelongsToContract(OfferContract.class)
@CordaSerializable
public class OfferState implements ContractState, Serializable {
    private UniqueIdentifier offerId;
    private Party issuer;
    private BondState bond;
    private String ticker;
    private float offerPrice;
    private float offerYield;
    private long aggregatedTradeSize;
    private long afsSize;
    private boolean afs;
    private Date creationDate;
    private List<AbstractParty> participants;


    @ConstructorForDeserialization
    public OfferState(){
        // for deserialization only
    }

    public OfferState(UniqueIdentifier offerId, Party issuer, BondState bond, String ticker, float offerPrice, float offerYield, long aggregatedTradeSize, long afsSize, boolean afs, Date creationDate) {
        this.offerId = offerId;
        this.issuer = issuer;
        this.bond = bond;
        this.ticker = ticker;
        this.offerPrice = offerPrice;
        this.offerYield = offerYield;
        this.aggregatedTradeSize = aggregatedTradeSize;
        this.afsSize = afsSize;
        this.afs = afs;
        this.creationDate = creationDate;
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

    public long getAggregatedTradeSize() {
        return aggregatedTradeSize;
    }

    public void setAggregatedTradeSize(long aggregatedTradeSize) {
        this.aggregatedTradeSize = aggregatedTradeSize;
    }

    public long getAfsSize() {
        return afsSize;
    }

    public void setAfsSize(long afsSize) {
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

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(this.issuer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfferState that = (OfferState) o;
        return Float.compare(that.getOfferPrice(), getOfferPrice()) == 0 &&
            Float.compare(that.getOfferYield(), getOfferYield()) == 0 &&
            // getAggregatedTradeSize() == that.getAggregatedTradeSize() &&
            // getAfsSize() == that.getAfsSize() &&
            isAfs() == that.isAfs() &&
            Objects.equals(getOfferId(), that.getOfferId()) &&
            Objects.equals(getIssuer(), that.getIssuer()) &&
            Objects.equals(getBond(), that.getBond()) &&
            Objects.equals(getTicker(), that.getTicker()) &&
            Objects.equals(getCreationDate(), that.getCreationDate()) &&
            Objects.equals(getParticipants(), that.getParticipants());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOfferId(), getIssuer(), getBond(), getTicker(), getOfferPrice(), getOfferYield(), getAggregatedTradeSize(), getAfsSize(), isAfs(), getCreationDate(), getParticipants());
    }

    @Override
    public String toString() {
        return "OfferState{" +
            "offerId=" + offerId +
            ", issuer=" + issuer +
            ", bond=" + bond +
            ", ticker='" + ticker + '\'' +
            ", offerPrice=" + offerPrice +
            ", offerYield=" + offerYield +
            ", aggregatedTradeSize=" + aggregatedTradeSize +
            ", afsSize=" + afsSize +
            ", afs=" + afs +
            ", creationDate=" + creationDate +
            ", participants=" + participants +
            '}';
    }

    public JsonObject toJson(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("offerId", offerId.getId().toString());
        jsonObject.addProperty("issuer", issuer.getName().toString());
        jsonObject.add("bond", bond.toJson());
        jsonObject.addProperty("ticker", ticker);
        jsonObject.addProperty("offerPrice", offerPrice);
        jsonObject.addProperty("offerYield", offerYield);
        jsonObject.addProperty("aggregatedTradeSize", aggregatedTradeSize);
        jsonObject.addProperty("afsSize", afsSize);
        jsonObject.addProperty("afs", afs);
        jsonObject.addProperty("creationDate", creationDate.toString());
        return jsonObject;
    }
}
