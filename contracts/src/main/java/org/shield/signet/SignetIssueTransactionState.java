package org.shield.signet;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;


@CordaSerializable
@BelongsToContract(SignetIssueTransactionContract.class)
public class SignetIssueTransactionState implements ContractState, Serializable {
    private UUID transactionId;
    private Timestamp timestamp;
    private Amount<Currency> amount;
    private SignetAccountState source;
    private SignetAccountState escrow;
    private String signetConfirmationId;
    private IssueState state;




    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(source.getOwner(), escrow.getOwner());
    }


}
