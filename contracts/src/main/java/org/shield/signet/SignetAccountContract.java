package org.shield.signet;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;


public class SignetAccountContract implements Contract {
    public static final String ID = "org.shield.signet.SignetAccountContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // no verifications yet
    }

    public interface Commands extends CommandData {
        class create implements SignetAccountContract.Commands {}
        class modify implements SignetAccountContract.Commands {}
    }
}
