package org.shield.flows.init;

import co.paralleluniverse.fibers.Suspendable;
import org.shield.contracts.InitContract;
import org.shield.states.BrokerDealerInitState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

// start BrokerDealerInit issuers: "O=PartyA,L=London,C=GB"
// run vaultQuery contractStateType: BrokerDealerInitState
@InitiatingFlow
@StartableByRPC
public class BrokerDealerInit extends FlowLogic<Void> {
    private List<Party> issuers;
    Party brokerDealer;


    public BrokerDealerInit (List<Party> issuers) {
        this.issuers = issuers;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        // we create the broker dealer init State
        this.brokerDealer = getOurIdentity();
        BrokerDealerInitState outputState = new BrokerDealerInitState(brokerDealer, issuers);

        // lets make sure there are no other BrokerDealerInitState already created.
        List<StateAndRef<BrokerDealerInitState>> brokerDealerInitStates = getServiceHub().getVaultService().queryBy(BrokerDealerInitState.class).getStates();
        if (!brokerDealerInitStates.isEmpty()) throw new FlowException("Broker Dealer state already exists. Modify with updateBrokerDealerInit flow.");

        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We create the transaction components.
        List<PublicKey> requiredSigners = Arrays.asList(this.brokerDealer.getOwningKey());

        // Command contract only requires the broker Dealer signature.
        Command command = new Command<>(new InitContract.Commands.brokerDealerSet(), requiredSigners);

        // we create the transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, InitContract.ID)
                .addCommand(command);

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
        subFlow(new FinalityFlow(signedTx));
        return null;
    }
}
