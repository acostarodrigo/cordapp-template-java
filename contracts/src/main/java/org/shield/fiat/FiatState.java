package org.shield.fiat;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(FiatContract.class)
public class FiatState implements ContractState {
    private Party issuer;
    private List<FiatTransaction> fiatTransactionList;
    private long lastUpdate;

    public FiatState(Party issuer, List<FiatTransaction> fiatTransactionList, long lastUpdate) {
        this.issuer = issuer;
        this.fiatTransactionList = fiatTransactionList;
        this.lastUpdate = lastUpdate;
    }

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public List<FiatTransaction> getFiatTransactionList() {
        return fiatTransactionList;
    }

    public void setFiatTransactionList(List<FiatTransaction> fiatTransactionList) {
        this.fiatTransactionList = fiatTransactionList;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(issuer);
    }
}
