package org.shield.contracts;

import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CommercialPaperContract implements Contract {
    public static final String ID = "org.shield.contracts.CommercialPaperContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<ArrangementContract.Commands> cmd = requireSingleCommand(tx.getCommands(), ArrangementContract.Commands.class);

        requireThat(require -> {
            //todo implement
            // Don't allow people to issue commercial paper under other entities.
            //require.using("Must have issuer signature", cmd.getSigners().contains(output.getIssuer().getOwningKey()));

            return null;
        });
    }
}
