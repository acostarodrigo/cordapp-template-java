package org.shield.bond;

import com.google.gson.JsonObject;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

@BelongsToContract(BondTypeContract.class)
@CordaSerializable
public class BondState extends EvolvableTokenType implements Serializable{
    private String id;

    private Party issuer;
    private String issuerTicker;
    private Currency denomination;
    private Date startDate;
    private int couponFrequency;
    private long minDenomination;
    private long increment;
    private DealType dealType;
    private int redemptionPrice;
    private long dealSize;
    private double initialPrice;
    private Date maturityDate;
    private double couponRate;
    private BondType bondType;

    // Evolvable Token type
    private int fractionDigits;
    private List<Party> maintainers;
    private UniqueIdentifier linearId;


    @ConstructorForDeserialization
    public BondState(){
        // for serialization
    }


    public BondState(String id, String issuerTicker, Currency denomination, Date startDate, int couponFrequency, long minDenomination, long increment, DealType dealType, int redemptionPrice, long dealSize, double initialPrice, Date maturityDate, double couponRate, int fractionDigits, BondType bondType) {
        this.id = id;
        this.issuerTicker = issuerTicker;
        this.denomination = denomination;
        this.startDate = startDate;
        this.couponFrequency = couponFrequency;
        this.minDenomination = minDenomination;
        this.increment = increment;
        this.dealType = dealType;
        this.redemptionPrice = redemptionPrice;
        this.dealSize = dealSize;
        this.initialPrice = initialPrice;
        this.maturityDate = maturityDate;
        this.couponRate = couponRate;
        this.fractionDigits = fractionDigits;
        this.linearId = new UniqueIdentifier();
        this.maintainers = new ArrayList<>();
        this.bondType = bondType;
    }

    // getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public String getIssuerTicker() {
        return issuerTicker;
    }

    public void setIssuerTicker(String issuerTicker) {
        this.issuerTicker = issuerTicker;
    }

    public Currency getDenomination() {
        return denomination;
    }

    public void setDenomination(Currency denomination) {
        this.denomination = denomination;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public int getCouponFrequency() {
        return couponFrequency;
    }

    public void setCouponFrequency(int couponFrequency) {
        this.couponFrequency = couponFrequency;
    }

    public long getMinDenomination() {
        return minDenomination;
    }

    public void setMinDenomination(long minDenomination) {
        this.minDenomination = minDenomination;
    }

    public long getIncrement() {
        return increment;
    }

    public void setIncrement(long increment) {
        this.increment = increment;
    }

    public DealType getDealType() {
        return dealType;
    }

    public void setDealType(DealType dealType) {
        this.dealType = dealType;
    }

    public int getRedemptionPrice() {
        return redemptionPrice;
    }

    public void setRedemptionPrice(int redemptionPrice) {
        this.redemptionPrice = redemptionPrice;
    }

    public long getDealSize() {
        return dealSize;
    }

    public void setDealSize(long dealSize) {
        this.dealSize = dealSize;
    }

    public double getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(double initialPrice) {
        this.initialPrice = initialPrice;
    }

    public Date getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(Date maturityDate) {
        this.maturityDate = maturityDate;
    }

    public double getCouponRate() {
        return couponRate;
    }

    public void setCouponRate(double couponRate) {
        this.couponRate = couponRate;
    }

    public void setFractionDigits(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    public void setMaintainers(List<Party> maintainers) {
        this.maintainers = maintainers;
    }

    public void setLinearId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    public BondType getBondType() {
        return bondType;
    }

    public void setBondType(BondType bondType) {
        this.bondType = bondType;
    }

    @Override
    public int getFractionDigits() {
        return this.fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return Arrays.asList((this.issuer));
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.linearId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BondState bondState = (BondState) o;
        return getCouponFrequency() == bondState.getCouponFrequency() &&
            getMinDenomination() == bondState.getMinDenomination() &&
            getIncrement() == bondState.getIncrement() &&
            getRedemptionPrice() == bondState.getRedemptionPrice() &&
            getDealSize() == bondState.getDealSize() &&
            Double.compare(bondState.getInitialPrice(), getInitialPrice()) == 0 &&
            Double.compare(bondState.getCouponRate(), getCouponRate()) == 0 &&
            getFractionDigits() == bondState.getFractionDigits() &&
            Objects.equals(getId(), bondState.getId()) &&
            Objects.equals(getIssuer(), bondState.getIssuer()) &&
            Objects.equals(getIssuerTicker(), bondState.getIssuerTicker()) &&
            Objects.equals(getDenomination(), bondState.getDenomination()) &&
            Objects.equals(getStartDate(), bondState.getStartDate()) &&
            getDealType() == bondState.getDealType() &&
            Objects.equals(getMaturityDate(), bondState.getMaturityDate()) &&
            Objects.equals(getMaintainers(), bondState.getMaintainers()) &&
            Objects.equals(getBondType(), bondState.getBondType()) &&
            Objects.equals(getLinearId(), bondState.getLinearId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getIssuer(), getIssuerTicker(), getDenomination(), getStartDate(), getCouponFrequency(), getMinDenomination(), getIncrement(), getDealType(), getRedemptionPrice(), getDealSize(), getInitialPrice(), getMaturityDate(), getCouponRate(), getFractionDigits(), getMaintainers(), getLinearId(), getBondType());
    }

    @Override
    public String toString() {
        return "BondState{" +
            "id=" + id +
            ", issuer=" + issuer +
            ", issuerTicker='" + issuerTicker + '\'' +
            ", denomination=" + denomination +
            ", startDate=" + startDate +
            ", couponFrequency=" + couponFrequency +
            ", minDenomination=" + minDenomination +
            ", increment=" + increment +
            ", dealType=" + dealType +
            ", redemptionPrice=" + redemptionPrice +
            ", dealSize=" + dealSize +
            ", initialPrice=" + initialPrice +
            ", maturityDate=" + maturityDate +
            ", couponRate=" + couponRate +
            ", fractionDigits=" + fractionDigits +
            ", linearId=" + linearId +
            ", bondType=" + bondType.toString() +
            '}';
    }

    /**
     * Bond Ticker is generated by ID + Coupon Rate + Date + Currency
     * @return
     */
    public String getTicker(){
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        StringBuilder tickerBuilder = new StringBuilder();
        tickerBuilder.append(getId());
        tickerBuilder.append(" ");
        tickerBuilder.append(getCouponRate());
        tickerBuilder.append("% ");
        tickerBuilder.append(f.format(getMaturityDate()));
        tickerBuilder.append(" ");
        tickerBuilder.append(getDenomination().getCurrencyCode());
        return  tickerBuilder.toString();
    }

    /**
     * Gets the JSON version of the bond.
     * @return Json object with all bond properties.
     */
    public JsonObject toJson(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("issuer", issuer.getName().toString());
        jsonObject.addProperty("issuerTicker", issuerTicker);
        jsonObject.addProperty("ticker", getTicker());
        jsonObject.addProperty("denomination", denomination.getCurrencyCode());
        jsonObject.addProperty("startDate", startDate.toString());
        jsonObject.addProperty("couponFrequency", couponFrequency);
        jsonObject.addProperty("minDenomination", minDenomination);
        jsonObject.addProperty("increment", increment);
        jsonObject.addProperty("dealType", dealType.toString());
        jsonObject.addProperty("redemptionPrice", redemptionPrice);
        jsonObject.addProperty("dealSize", dealSize);
        jsonObject.addProperty("initialPrice", initialPrice);
        jsonObject.addProperty("maturityDate", maturityDate.toString());
        jsonObject.addProperty("couponRate", couponRate);
        jsonObject.addProperty("fractionDigits", fractionDigits);
        jsonObject.addProperty("linearId", linearId.getId().toString());
        jsonObject.addProperty("bondType", bondType.toString());
        return jsonObject;
    }
}
