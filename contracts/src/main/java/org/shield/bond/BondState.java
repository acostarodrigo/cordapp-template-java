package org.shield.bond;

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondTypeContract;

import java.io.Serializable;
import java.util.*;

@BelongsToContract(BondTypeContract.class)
@CordaSerializable
public class BondState extends EvolvableTokenType {
    private Party issuer;
    private String issuerName;
    private Currency denomination;
    private Date startDate;
    private int couponFrequency;
    private long minDenomination;
    private long increment;
    private DealType dealType;
    private int redemptionPrice;
    private long dealSize;
    private float initialPrice;
    private Date maturityDate;
    private float couponRate;

    // Evolvable Token type
    private int fractionDigits;
    private List<Party> maintainers;
    private UniqueIdentifier linearId;


    public BondState(Party issuer, String issuerName, Currency denomination, Date startDate, int couponFrequency, long minDenomination, long increment, DealType dealType, int redemptionPrice, long dealSize, float initialPrice, Date maturityDate, float couponRate, int fractionDigits) {
        this.issuer = issuer;
        this.issuerName = issuerName;
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
        this.maintainers = Arrays.asList(issuer); //issuer is the maintainer
        this.linearId = new UniqueIdentifier(); //
    }


    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
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

    public float getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(float initialPrice) {
        this.initialPrice = initialPrice;
    }

    public Date getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(Date maturityDate) {
        this.maturityDate = maturityDate;
    }

    public float getCouponRate() {
        return couponRate;
    }

    public void setCouponRate(float couponRate) {
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

    @Override
    public int getFractionDigits() {
        return this.fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return this.maintainers;
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
            Float.compare(bondState.getInitialPrice(), getInitialPrice()) == 0 &&
            Float.compare(bondState.getCouponRate(), getCouponRate()) == 0 &&
            getFractionDigits() == bondState.getFractionDigits() &&
            Objects.equals(getIssuer(), bondState.getIssuer()) &&
            Objects.equals(getIssuerName(), bondState.getIssuerName()) &&
            Objects.equals(getDenomination(), bondState.getDenomination()) &&
            Objects.equals(getStartDate(), bondState.getStartDate()) &&
            getDealType() == bondState.getDealType() &&
            Objects.equals(getMaturityDate(), bondState.getMaturityDate()) &&
            Objects.equals(getMaintainers(), bondState.getMaintainers()) &&
            Objects.equals(getLinearId(), bondState.getLinearId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIssuer(), getIssuerName(), getDenomination(), getStartDate(), getCouponFrequency(), getMinDenomination(), getIncrement(), getDealType(), getRedemptionPrice(), getDealSize(), getInitialPrice(), getMaturityDate(), getCouponRate(), getFractionDigits(), getMaintainers(), getLinearId());
    }
}
