package org.shield.offer;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.trade.TradeContract;

import java.util.Date;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class OfferContract implements Contract {
    public static final String ID = "org.shield.offer.OfferContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<OfferContract.Commands> cmd = requireSingleCommand(tx.getCommands(), OfferContract.Commands.class);
        OfferState output = (OfferState) tx.getOutput(0);

        if (cmd.getValue() instanceof OfferContract.Commands.create){
            requireThat(require -> {
                // must be signed by owner of the offer
                require.using("Must have issuer signature", cmd.getSigners().contains(output.getIssuer().getOwningKey()));
                // only one signature is required
                require.using("Only one signature is required", cmd.getSigners().size() == 1);
                // creation date can't be in the future
                Date now = new Date();
                require.using("Creation date can't be in the future", output.getCreationDate().before(now));

                return null;
            });
        }

        if (cmd.getValue() instanceof OfferContract.Commands.notifyBuyers){
            requireThat(require -> {
                // must be signed by owner of the offer
                require.using("Must have issuer signature", cmd.getSigners().contains(output.getIssuer().getOwningKey()));
                // only one signature is required
                require.using("Only one signature is required", cmd.getSigners().size() == 1);
                // we only allow changes to AFS, and offer size

                return null;
            });
        }



    }

    public interface Commands extends CommandData {
        class notifyBuyers implements OfferContract.Commands {}
        class create implements OfferContract.Commands {}
    }
}
