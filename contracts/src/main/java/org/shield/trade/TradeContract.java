package org.shield.trade;

import org.shield.trade.TradeState;

import net.corda.core.contracts.*;
import net.corda.core.transactions.LedgerTransaction;

import java.util.Date;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class TradeContract implements Contract {

    public static final String ID = "org.shield.trade.TradeContract";

    @Override
    public void verify(LedgerTransaction tx) {
        CommandWithParties<Commands> cmd = requireSingleCommand(tx.getCommands(), Commands.class);
        TradeState output =  tx.outputsOfType(TradeState.class).get(0);
        // preIssue
        if (cmd.getValue() instanceof Commands.sendToBuyer) {
            requireThat(require -> {
//                // Don't allow people to issue commercial paper under other entities identities.
//                require.using("Must have issuer signature", cmd.getSigners().contains(output.getIssuer().getOwningKey()));
//
//                require.using("Must have broker dealer signature.", cmd.getSigners().contains(output.getBrokerDealer().getOwningKey()));
//
//                // no inputs are consumed
//                require.using("We are not using inputs to issue arrangement", tx.getInputs().isEmpty());
//
//                // size is greater than zero
//                require.using("arrangement size is greater than zero", output.getSize() >= 0);
//
//                require.using("Offering date can't be in the past", output.getOfferingDate().after(new Date()));
//
//                require.using("State of the arrangement must be preIssue.", output.getState().equals(TradeState.State.PREISSUE));

                return null;

            });

        } else if (cmd.getValue() instanceof Commands.accept) {
            requireThat(require -> {
                require.using("Must have only 1 input", tx.inputsOfType(TradeState.class).size() == 1);

                // we get the input
//                TradeState input = tx.inputsOfType(TradeState.class).get(0);
//
//                require.using("Must have broker dealer signature.", cmd.getSigners().contains(output.getBrokerDealer().getOwningKey()));
//
//                // we only allow changing state of a preIssue arrangement
//                require.using("State of old arrangement must be preIssue.", input.getState().equals(TradeState.State.PREISSUE));
//
//                // output state must be Accepted
//                require.using("State of new arrangement must be Accepted.", output.getState().equals(TradeState.State.ACCEPTED));
//
//                // we only allow certain fields to change, so we compare input and output fields remained unchanged
//                require.using("Id value can't change", output.getId().equals(input.getId()));
//                require.using("Issuer value can't change", output.getIssuer().equals(input.getIssuer()));
//                require.using("Broker Dealer value can't change", output.getBrokerDealer().equals(input.getBrokerDealer()));
//                require.using("Offering date can't change", output.getOfferingDate().equals(input.getOfferingDate()));
                return null;
            });

        } else if (cmd.getValue() instanceof Commands.cancel) {
            requireThat(require -> {
                require.using("Must have only 1 input", tx.inputsOfType(TradeState.class).size() == 1);

                // we get the input
//                TradeState input = tx.inputsOfType(TradeState.class).get(0);
//
//                // we only allow changing state of a preIssue arrangement
//                require.using("State of old arrangement must be preIssue.", input.getState().equals(TradeState.State.PREISSUE));
//
//                require.using("Must have broker dealer signature.", cmd.getSigners().contains(output.getBrokerDealer().getOwningKey()));
//
//                // output state must be cancelled
//                require.using("State of passed arrangement must be cancelled.", output.getState().equals(TradeState.State.CANCELLED));
                return null;
            });

        } else if (cmd.getValue() instanceof Commands.issue) {
            requireThat(require -> {
                require.using("Must have only 1 input", tx.inputsOfType(TradeState.class).size() == 1);

//                require.using("Must have issuer signature", cmd.getSigners().contains(output.getIssuer().getOwningKey()));
//
//                // we get the input
//                TradeState input = tx.inputsOfType(TradeState.class).get(0);
//
//                // we only allow changing state of a preIssue arrangement
//                require.using("State of old arrangement must be Accepted.", input.getState().equals(TradeState.State.ACCEPTED));
//
//                require.using("Must have broker dealer signature.", cmd.getSigners().contains(output.getBrokerDealer().getOwningKey()));
//
//                // output state must be cancelled
//                require.using("State of passed arrangement must be ISSUED.", output.getState().equals(TradeState.State.ISSUED));
//
//                // must specify a paperId
//                require.using("Must specify a paperId.", output.getPaperId() != null);
                return null;
            });

        } else {
            throw new IllegalArgumentException("Unrecognised command");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class sendToBuyer implements Commands {}
        class accept implements Commands {}
        class cancel implements Commands {}
        class issue implements Commands {}
    }
}
