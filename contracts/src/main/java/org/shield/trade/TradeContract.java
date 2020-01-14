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
        for (CommandWithParties<CommandData> command : tx.getCommands()){
            if (command.getValue() instanceof Commands.Proposed){
                // issuer

            } else if (command.getValue() instanceof Commands.Pending){

            } else if (command.getValue() instanceof Commands.Settled){

            } else if (command.getValue() instanceof Commands.Cancelled){

            }

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
