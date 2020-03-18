package org.shield.webserver.offer;

import com.google.gson.JsonObject;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.shield.bond.BondTypeContract;
import org.shield.bond.DealType;

import java.util.Currency;
import java.util.Date;

public class MyInventory {
    private String bondId;
    private String ticker;
    private float offerPrice;
    private float offerYield;
    private long aggregatedTradeSize;
    private long afsSize;
    private Date bondMaturity;
    private double coupon;
    private int couponFrequency;
    private long dealSize;
    private DealType dealType;
    private Currency currency;
    private boolean afs;
    private UniqueIdentifier offerId;
    private UniqueIdentifier bondTraceId;
    private Party issuer;

    public MyInventory(String bondId, String ticker, float offerPrice, float offerYield, long aggregatedTradeSize, long afsSize, Date bondMaturity, double coupon, int couponFrequency, long dealSize, DealType dealType, Currency currency, boolean afs, UniqueIdentifier offerId, UniqueIdentifier bondTraceId, Party issuer) {
        this.bondId = bondId;
        this.ticker = ticker;
        this.offerPrice = offerPrice;
        this.offerYield = offerYield;
        this.aggregatedTradeSize = aggregatedTradeSize;
        this.afsSize = afsSize;
        this.bondMaturity = bondMaturity;
        this.coupon = coupon;
        this.couponFrequency = couponFrequency;
        this.dealSize = dealSize;
        this.dealType = dealType;
        this.currency = currency;
        this.afs = afs;
        this.offerId = offerId;
        this.bondTraceId = bondTraceId;
        this.issuer = issuer;
    }

    public String getBondId() {
        return bondId;
    }

    public void setBondId(String bondId) {
        this.bondId = bondId;
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

    public Date getBondMaturity() {
        return bondMaturity;
    }

    public void setBondMaturity(Date bondMaturity) {
        this.bondMaturity = bondMaturity;
    }

    public double getCoupon() {
        return coupon;
    }

    public void setCoupon(double coupon) {
        this.coupon = coupon;
    }

    public int getCouponFrequency() {
        return couponFrequency;
    }

    public void setCouponFrequency(int couponFrequency) {
        this.couponFrequency = couponFrequency;
    }

    public long getDealSize() {
        return dealSize;
    }

    public void setDealSize(long dealSize) {
        this.dealSize = dealSize;
    }

    public DealType getDealType() {
        return dealType;
    }

    public void setDealType(DealType dealType) {
        this.dealType = dealType;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public boolean isAfs() {
        return afs;
    }

    public void setAfs(boolean afs) {
        this.afs = afs;
    }

    public UniqueIdentifier getOfferId() {
        return offerId;
    }

    public void setOfferId(UniqueIdentifier offerId) {
        this.offerId = offerId;
    }

    public UniqueIdentifier getBondTraceId() {
        return bondTraceId;
    }

    public void setBondTraceId(UniqueIdentifier bondTraceId) {
        this.bondTraceId = bondTraceId;
    }

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public JsonObject toJson(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("bondId", bondId);
        jsonObject.addProperty("ticker", ticker);
        jsonObject.addProperty("offerPrice", offerPrice);
        jsonObject.addProperty("offerYield", offerYield);
        jsonObject.addProperty("aggregatedTradeSize", aggregatedTradeSize);
        jsonObject.addProperty("afsSize", afsSize);
        jsonObject.addProperty("bondMaturity", bondMaturity.toString());
        jsonObject.addProperty("coupon", coupon);
        jsonObject.addProperty("couponFrequency", couponFrequency);
        jsonObject.addProperty("dealSize", dealSize);
        jsonObject.addProperty("currency", currency.toString());
        jsonObject.addProperty("afs", afs);
        jsonObject.addProperty("offerId", offerId.getId().toString());
        jsonObject.addProperty("bondTraceId", bondTraceId.getId().toString());
        jsonObject.addProperty("issuer", issuer.getName().toString());

        return jsonObject;
    }
}
