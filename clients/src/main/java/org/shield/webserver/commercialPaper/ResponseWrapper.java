package org.shield.webserver.commercialPaper;

public class ResponseWrapper {
    private String issuer;
    private long paperValue;
    private long tokenQuantity;

    public ResponseWrapper(String issuer, long paperValue) {
        this.issuer = issuer;
        this.paperValue = paperValue;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getPaperValue() {
        return paperValue;
    }

    public void setPaperValue(long paperValue) {
        this.paperValue = paperValue;
    }

    public long getTokenQuantity() {
        return tokenQuantity;
    }

    public void setTokenQuantity(long tokenQuantity) {
        this.tokenQuantity = tokenQuantity;
    }
}
