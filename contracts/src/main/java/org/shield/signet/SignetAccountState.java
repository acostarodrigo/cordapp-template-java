package org.shield.signet;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@CordaSerializable
public class SignetAccountState implements ContractState, Serializable {
    private Party owner;
    private String walletAddress;


    public SignetAccountState(Party owner, String walletAddresses) {
        this.owner = owner;
        this.walletAddress = walletAddresses;
    }

    public Party getOwner() {
        return owner;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignetAccountState that = (SignetAccountState) o;
        return Objects.equals(getOwner(), that.getOwner()) &&
            Objects.equals(getWalletAddress(), that.getWalletAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwner(), getWalletAddress());
    }

    @Override
    public String toString() {
        return "SignetAccountState{" +
            "owner=" + owner +
            ", walletAddresses='" + walletAddress + '\'' +
            '}';
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner);
    }
}
