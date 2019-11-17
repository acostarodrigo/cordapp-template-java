package org.shield.webserver.init.issuer;

import org.shield.states.BrokerDealerInitState;
import org.shield.states.IssuerInitState;

public class ResponseWrapper {
    private String issuer;
    private String brokerDealers;

    public ResponseWrapper(IssuerInitState issuerInitState) {
        this.issuer = issuerInitState.getIssuer().getName().toString();
        this.brokerDealers = issuerInitState.getBrokerDealers().get(0).getName().toString();
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getBrokerDealers() {
        return brokerDealers;
    }

    public void setBrokerDealers(String brokerDealers) {
        this.brokerDealers = brokerDealers;
    }
}
