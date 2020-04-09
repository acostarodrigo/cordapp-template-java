package org.shield.fiat;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class FiatContract implements Contract {
    public static final String ID = "org.shield.fiat.FiatContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // no verifications needed for now.
    }

    public interface Commands extends CommandData {
        class newTransaction implements FiatContract.Commands {}
    }
}
