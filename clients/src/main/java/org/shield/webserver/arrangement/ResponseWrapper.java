package org.shield.webserver.arrangement;

import java.util.Date;

public class ResponseWrapper {
    private String id;
    private String issuer;
    private String brokerDealer;
    private int size;
    private Date offeringDate;
    private String state;

    public ResponseWrapper(String id,String issuer, String brokerDealer, int size, Date offeringDate, String state) {
        this.id = id;
        this.issuer = issuer;
        this.brokerDealer = brokerDealer;
        this.size = size;
        this.offeringDate = offeringDate;
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getBrokerDealer() {
        return brokerDealer;
    }

    public void setBrokerDealer(String brokerDealer) {
        this.brokerDealer = brokerDealer;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Date getOfferingDate() {
        return offeringDate;
    }

    public void setOfferingDate(Date offeringDate) {
        this.offeringDate = offeringDate;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
