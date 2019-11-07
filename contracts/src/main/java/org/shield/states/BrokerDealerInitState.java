package org.shield.states;

import org.shield.contracts.InitContract;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(InitContract.class)
public class BrokerDealerInitState implements ContractState {
    private Party brokerDealer;
    private List<Party> issuers;

    public BrokerDealerInitState(Party brokerDealer, List<Party> issuers) {
        this.brokerDealer = brokerDealer;
        this.issuers = issuers;
    }

    public Party getBrokerDealer() {
        return brokerDealer;
    }


    public List<Party> getIssuers() {
        return issuers;
    }

    public void setIssuers(List<Party> issuers) {
        this.issuers = issuers;
    }

    public void addIssuer(Party issuer){
        this.issuers.add(issuer);
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(this.brokerDealer);
    }
}
