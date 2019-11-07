package org.shield.flows.init;

import co.paralleluniverse.fibers.Suspendable;
import org.shield.contracts.InitContract;
import org.shield.states.IssuerInitState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.List;

@StartableByRPC
@InitiatingFlow
// start IssuerUpdate brokerDealers: "O=brokerDealer1,L=New York,C=US"
public class IssuerUpdate extends FlowLogic<Void> {
    List<Party> brokerDealers;

    // constructor with the brokerDealer list to update
    public IssuerUpdate(List<Party> brokerDealers){
        this.brokerDealers = brokerDealers;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        // we are getting the notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // lets get the IssuerInit state from our vault as input
        List<StateAndRef<IssuerInitState>> issuerInitStates = getServiceHub().getVaultService().queryBy(IssuerInitState.class).getStates();
        if (issuerInitStates.size() == 0) throw new FlowException("Issuer Init state does not exists on this node vault.");
        StateAndRef<IssuerInitState> input = issuerInitStates.get(0);

        // we make the changes with the new list of broker dealers
        IssuerInitState output = input.getState().getData();
        output.setBrokerDealers(this.brokerDealers);

        // we create the command and the transaction
        Command command = new Command<>(new InitContract.Commands.issuerUpdate(), getOurIdentity().getOwningKey());
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
