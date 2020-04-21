package org.shield.offer;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Date;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class OfferContract implements Contract {
    public static final String ID = "org.shield.offer.OfferContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        final Commands command = tx.findCommand(OfferContract.Commands.class, cmd -> true).getValue();
        final List<PublicKey> signers = tx.findCommand(OfferContract.Commands.class, cmd -> true).getSigners();


        OfferState output = (OfferState) tx.outputsOfType(OfferState.class).get(0);

        if (command instanceof OfferContract.Commands.create){
            requireThat(require -> {
                // must be signed by owner of the offer
                require.using("Must have issuer signature", signers.contains(output.getIssuer().getOwningKey()));
                // only one signature is required
                require.using("Only one signature is required", signers.size() == 1);
                // creation date can't be in the future
                Date now = new Date();
                require.using("Creation date can't be in the future", output.getCreationDate().before(now));

                return null;
            });
        }

        if (command instanceof OfferContract.Commands.notifyBuyers){
            OfferState input = (OfferState) tx.inputsOfType(OfferState.class).get(0);
            requireThat(require -> {
                // must be signed by owner of the offer
                require.using("Must have issuer signature", signers.contains(output.getIssuer().getOwningKey()));
                // only one signature is required
                require.using("Only one signature is required", signers.size() == 1);
                // we only allow changes to AFS, and offer size
                OfferState modifiedInput = input;
                modifiedInput.setAfs(output.isAfs());
                modifiedInput.setAfsSize(output.getAfsSize());
                require.using("Only AFS value and AFS Size can be updated.", modifiedInput.equals(output));
                return null;
            });
        }



    }

    public interface Commands extends CommandData {
        class notifyBuyers implements OfferContract.Commands {}
        class create implements OfferContract.Commands {}
        class modify implements OfferContract.Commands {}
    }
}
