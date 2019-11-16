package org.shield.webserver.init.brokerDealer;

import org.shield.states.BrokerDealerInitState;

import java.util.List;

public class ResponseWrapper {
    private String brokerDealer;
    private String issuers;

    public ResponseWrapper(BrokerDealerInitState brokerDealerInitState) {
         this.brokerDealer = brokerDealerInitState.getBrokerDealer().getName().toString();
         this.issuers = brokerDealerInitState.getIssuers().get(0).getName().toString();
    }

    public String getIssuers() {
        return issuers;
    }

    public void setIssuers(String issuers) {
        this.issuers = issuers;
    }

    public String getBrokerDealer() {
        return brokerDealer;
    }

    public void setBrokerDealer(String brokerDealer) {
        this.brokerDealer = brokerDealer;
    }
}
