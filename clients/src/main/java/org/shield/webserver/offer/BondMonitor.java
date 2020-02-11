package org.shield.webserver.offer;

import com.google.gson.JsonObject;
import net.corda.core.contracts.UniqueIdentifier;
import org.shield.bond.BondState;
import org.shield.bond.DealType;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Date;


public class BondMonitor {
    private UniqueIdentifier offerId;
    private UniqueIdentifier bondId;
    private String ticker;
    private float currentPrice;
    private float currentYield;
    private Date bondMaturity;
    private String coupon;
    private String couponFrequency;
    private String bondStructure;
    private String market;
    private long dealSize;
    private DealType dealType;
    private Currency currency;

    public BondMonitor(UniqueIdentifier offerId, UniqueIdentifier bondId, String ticker, float currentPrice, float currentYield, Date bondMaturity, String coupon, String couponFrequency, String bondStructure, String market, long dealSize, DealType dealType, Currency currency) {
        this.offerId = offerId;
        this.bondId = bondId;
        this.ticker = ticker;
        this.currentPrice = currentPrice;
        this.currentYield = currentYield;
        this.bondMaturity = bondMaturity;
        this.coupon = coupon;
        this.couponFrequency = couponFrequency;
        this.bondStructure = bondStructure;
        this.market = market;
        this.dealSize = dealSize;
        this.dealType = dealType;
        this.currency = currency;
    }

    public UniqueIdentifier getOfferId() {
        return offerId;
    }

    public void setOfferId(UniqueIdentifier offerId) {
        this.offerId = offerId;
    }

    public UniqueIdentifier getBondId() {
        return bondId;
    }

    public String getTicker() {
        return ticker;
    }

    public float getCurrentPrice() {
        return currentPrice;
    }

    public float getCurrentYield() {
        return currentYield;
    }

    public Date getBondMaturity() {
        return bondMaturity;
    }

    public String getCoupon() {
        return coupon;
    }

    public String getCouponFrequency() {
        return couponFrequency;
    }

    public String getBondStructure() {
        return bondStructure;
    }

    public String getMarket() {
        return market;
    }

    public long getDealSize() {
        return dealSize;
    }

    public DealType getDealType() {
        return dealType;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public String toString() {
        return "BondMonitor{" +
            "offerId=" + offerId +
            ", bondId=" + bondId +
            ", ticker='" + ticker + '\'' +
            ", currentPrice=" + currentPrice +
            ", currentYield=" + currentYield +
            ", bondMaturity=" + bondMaturity +
            ", coupon='" + coupon + '\'' +
            ", couponFrequency='" + couponFrequency + '\'' +
            ", bondStructure='" + bondStructure + '\'' +
            ", market='" + market + '\'' +
            ", dealSize=" + dealSize +
            ", dealType=" + dealType +
            ", currency=" + currency +
            '}';
    }

    public JsonObject toJson(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("offerId", offerId.getId().toString());
        jsonObject.addProperty("bondId", bondId.getId().toString());
        jsonObject.addProperty("ticker", ticker);
        jsonObject.addProperty("currentPrice", currentPrice);
        jsonObject.addProperty("currentYield", currentYield);
        jsonObject.addProperty("bondMaturity", bondMaturity.toString());
        jsonObject.addProperty("coupon", coupon);
        jsonObject.addProperty("couponFrequency", couponFrequency);
        jsonObject.addProperty("bondStructure", bondStructure);
        jsonObject.addProperty("market", market);
        jsonObject.addProperty("dealSize", dealSize);
        jsonObject.addProperty("dealType", dealType.toString());
        jsonObject.addProperty("currency", currency.getCurrencyCode());

        return jsonObject;
    }
}
