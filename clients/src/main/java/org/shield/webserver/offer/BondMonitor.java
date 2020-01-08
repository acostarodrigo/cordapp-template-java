package org.shield.webserver.offer;

import net.corda.core.contracts.UniqueIdentifier;
import org.shield.bond.BondState;
import org.shield.bond.DealType;

import java.util.Currency;
import java.util.Date;

public class BondMonitor {
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

    public BondMonitor(UniqueIdentifier bondId, String ticker, float currentPrice, float currentYield, Date bondMaturity, String coupon, String couponFrequency, String bondStructure, String market, long dealSize, DealType dealType, Currency currency) {
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
            "bondId=" + bondId +
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
}
