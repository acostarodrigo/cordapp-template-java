package org.shield.signet;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class SignetIssueTransactionContract implements Contract {
    public static final String ID = "org.shield.signet.SignetIssueTransactionContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // no verifications yets.
    }

    public interface Commands extends CommandData {
        class create implements SignetIssueTransactionContract.Commands {}
        class updateState implements SignetIssueTransactionContract.Commands {}
        class retry implements SignetIssueTransactionContract.Commands {}
    }
}
