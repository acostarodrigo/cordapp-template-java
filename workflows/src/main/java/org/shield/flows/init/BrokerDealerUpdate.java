package org.shield.flows.init;

import org.shield.contracts.InitContract;
import org.shield.states.BrokerDealerInitState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.List;

// start BrokerDealerUpdate issuers: "O=issuer,L=London,C=GB"
// run vaultQuery contractStateType: org.shield.states.BrokerDealerInitState
@InitiatingFlow
@StartableByRPC
public class BrokerDealerUpdate extends FlowLogic<Void> {
    List<Party> issuers;

    public BrokerDealerUpdate (List<Party> issuers) {
        this.issuers = issuers;
    }

    @Override
    public Void call() throws FlowException {
        // we are getting the notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // lets get the IssuerInit state from our vault as input
        List<StateAndRef<BrokerDealerInitState>> brokerDealerInitStates = getServiceHub().getVaultService().queryBy(BrokerDealerInitState.class).getStates();
        if (brokerDealerInitStates.size() == 0) throw new FlowException("Broker Dealer Init state does not exists on this node vault.");
        StateAndRef<BrokerDealerInitState> input = brokerDealerInitStates.get(0);

        // we make the changes with the new list of broker dealers
        BrokerDealerInitState output = input.getState().getData();
        output.setIssuers(this.issuers);

        // we create the command and the transaction
        Command command = new Command<>(new InitContract.Commands.brokerDealerUpdate(), getOurIdentity().getOwningKey());
        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(input)
                .addOutputState(output, InitContract.ID)
                .addCommand(command);

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        subFlow(new FinalityFlow(signedTx));
        return null;
    }
}
