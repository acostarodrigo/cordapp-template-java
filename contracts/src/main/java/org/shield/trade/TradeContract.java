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
            if (command.getValue() instanceof Commands.sendToBuyer){

            } else if (command.getValue() instanceof Commands.accept){

            } else if (command.getValue() instanceof Commands.settle){

            }
        }

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class sendToBuyer implements Commands {}
        class accept implements Commands {}
        class settle implements Commands{}
        class cancel implements Commands {}
        class issue implements Commands {}
    }
}
