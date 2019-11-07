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
public class IssuerInitState implements ContractState {
    private Party issuer;
    private List<Party> brokerDealers;

    /**
     * constructos
     * @param issuer
     * @param brokerDealers
     */
    public IssuerInitState(Party issuer, List<Party> brokerDealers) {
        this.issuer = issuer;
        this.brokerDealers = brokerDealers;
    }

    public Party getIssuer() {
        return issuer;
    }


    public List<Party> getBrokerDealers() {
        return brokerDealers;
    }

    public void setBrokerDealers(List<Party> brokerDealers) {
        this.brokerDealers = brokerDealers;
    }

    public void addBrokerDealer(Party brokerDealer){
        this.brokerDealers.add(brokerDealer);
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(this.issuer);
    }
}
