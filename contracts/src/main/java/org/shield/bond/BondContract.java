package org.shield.bond;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;
import org.shield.trade.TradeContract;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class BondContract implements Contract {
    public static final String ID = "org.shield.bond.BondContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<TradeContract.Commands> cmd = requireSingleCommand(tx.getCommands(), TradeContract.Commands.class);
        BondState output = (BondState) tx.getOutput(0);

        requireThat(require -> {
            //todo implement
            // Don't allow people to issue commercial paper under other entities.
            require.using("Must have issuer signature", cmd.getSigners().contains(output.getIssuer().getOwningKey()));

            return null;
        });
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class issue implements TradeContract.Commands {}
    }
}
