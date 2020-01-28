package org.shield.signet;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@CordaSerializable
public class SignetAccountState implements ContractState, Serializable {
    private Party owner;
    private List<String> walletAddresses;

    public SignetAccountState(Party owner, List<String> walletAddresses) {
        this.owner = owner;
        this.walletAddresses = walletAddresses;
    }

    public Party getOwner() {
        return owner;
    }

    public List<String> getWalletAddresses() {
        return walletAddresses;
    }

    public void setWalletAddresses(List<String> walletAddresses) {
        this.walletAddresses = walletAddresses;
    }

    public void addWalletAddress(String address){
        if (!this.walletAddresses.contains(address))
            this.walletAddresses.add(address);
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner);
    }
}
