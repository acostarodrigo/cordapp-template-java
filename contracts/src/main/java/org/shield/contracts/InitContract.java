package org.shield.contracts;

import org.shield.states.BrokerDealerInitState;
import org.shield.states.IssuerInitState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


/**
 * Contract to verify IssuerInitState and BrokerDealer Init state configurations.
 * We make specific validations for all commands.
 * Init states exists only in caller's vault and are used to verify other transactions.
 */
public class InitContract implements Contract {

    public static final String ID = "org.shield.contracts.InitContract";

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class issuerSet implements InitContract.Commands {}
        class issuerUpdate implements InitContract.Commands {}
        class brokerDealerSet implements InitContract.Commands {}
        class brokerDealerUpdate implements InitContract.Commands {}
    }

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<InitContract.Commands> cmd = requireSingleCommand(tx.getCommands(), InitContract.Commands.class);

        // requires for all command types
        requireThat(require -> {
                require.using("Only one output of Init configurations is allowed.", tx.getOutputs().size() == 1);
                require.using("Only one signer is allowed", cmd.getSigners().size() == 1);
                return null;
            });



        // Issuer set comand vaidations
        if (cmd.getValue() instanceof InitContract.Commands.issuerSet) {
            IssuerInitState output = tx.outputsOfType(IssuerInitState.class).get(0);
            requireThat(require ->{
                require.using("Specified Issuer must be the same as caller.", output.getIssuer().getOwningKey().equals(cmd.getSigners().get(0)));
               require.using("Set commands can only have outputs.", tx.getInputs().isEmpty());
                return null;
            });
        }

        // Issuer update command validations
        if (cmd.getValue() instanceof InitContract.Commands.issuerUpdate) {
            IssuerInitState output = tx.outputsOfType(IssuerInitState.class).get(0);
            requireThat(require ->{
                require.using("Specified Issuer must be the same as caller.", output.getIssuer().getOwningKey().equals(cmd.getSigners().get(0)));
                require.using("Update command can have only 1 input.", tx.getInputs().size() == 1);
                return null;
            });
        }

        // Broker Dealer set command validations
        if (cmd.getValue() instanceof InitContract.Commands.brokerDealerSet) {
            BrokerDealerInitState output = tx.outputsOfType(BrokerDealerInitState.class).get(0);
            requireThat(require ->{
                require.using("Specified Broker Dealer must be the same as caller.", output.getBrokerDealer().getOwningKey().equals(cmd.getSigners().get(0)));
                require.using("Set commands can only have outputs.", tx.getInputs().isEmpty());
                return null;
            });
        }

        // Broker Dealer set command validations
        if (cmd.getValue() instanceof InitContract.Commands.brokerDealerUpdate) {
            BrokerDealerInitState output = tx.outputsOfType(BrokerDealerInitState.class).get(0);
            requireThat(require ->{
                require.using("Specified Broker Dealer must be the same as caller.", output.getBrokerDealer().getOwningKey().equals(cmd.getSigners().get(0)));
                require.using("Update command can have only 1 input.", tx.getInputs().size() == 1);
                return null;
            });
        }
    }
}
