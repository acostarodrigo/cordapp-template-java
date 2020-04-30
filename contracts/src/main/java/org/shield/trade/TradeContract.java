package org.shield.trade;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.Date;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;


public class TradeContract implements Contract {

    public static final String ID = "org.shield.trade.TradeContract";

    @Override
    public void verify(LedgerTransaction tx) {
        final Commands command = tx.findCommand(TradeContract.Commands.class, cmd -> true).getValue();
        final List<PublicKey> signers = tx.findCommand(TradeContract.Commands.class, cmd -> true).getSigners();

        TradeState output = (TradeState) tx.outputsOfType(TradeState.class).get(0);

        if (command instanceof TradeContract.Commands.Proposed){
            requireThat(require -> {
                // state must be proposed.
                require.using("State of the trade must be proposed.", output.getState().equals(State.PROPOSED));
                // must be signed by owner of the offer
                require.using("Must have issuer signature", signers.contains(output.getIssuer().getOwningKey()));
                // must have issuer and seller / buyer signature
                require.using("Only two signature are required", signers.size() == 2);
                // settle date can't be in the past
                Date now = new Date();
                require.using("Settle date can't be in the future", output.getSettleDate().before(now));
                // size can be 0
                require.using("Trade size can't be zero", output.getSize() > 0);
                // Offer size must greater than zero
                require.using("Offer size must be greater than zero", output.getOffer().getAfsSize() > 0);
                // Trade size must be greater or equal than offer size.
                require.using("Trade size can't be greater than offer size", output.getSize() <= output.getOffer().getAfsSize());
                // buyer and seller can't be the same
                require.using("Can't buy your own bond.", !output.getBuyer().equals(output.getSeller()));
                return null;
            });
        }

        // trade has been accepted.
        if (command instanceof TradeContract.Commands.Pending){
            TradeState input = (TradeState) tx.inputsOfType(TradeState.class).get(0);

            requireThat(require -> {
                // state must be Pending
                require.using("State of the trade must be pending.", output.getState().equals(State.PENDING));
                // we don't allow any change in the trade other than state.
                // So changing input state to pending should be equal to output
                input.setState(State.PENDING);
                input.setStateUpdate(output.getStateUpdate());
                require.using("Trade modified from original before accepting.", output.equals(input));
                // must have issuer and seller / buyer signature
                require.using("Only two signatures are required", signers.size() == 2);
                return null;
            });
        }

        // trade has been cancelled.
        if (command instanceof TradeContract.Commands.Cancelled){
            // this is the structure of the transaction
            TradeState tradeInput = (TradeState) tx.inputsOfType(TradeState.class).get(0);
            requireThat(require -> {
                // state must be cancelled
                require.using("State of the trade must be cancelled.", output.getState().equals(State.CANCELLED));
                // must have issuer and seller / buyer signature
                require.using("Only 2 signatures are required", signers.size() == 2);
                return null;
            });
        }

        // trade has been settled.
        if (command instanceof TradeContract.Commands.Settled){
            // todo add validations
        }

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Proposed implements Commands {}
        class Pending implements Commands {}
        class Settled implements Commands{}
        class Cancelled implements Commands {}
    }
}
