package org.shield.flows.init;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.shield.contracts.InitContract;
import org.shield.states.BrokerDealerInitState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

public class BrokerDealerInitFlow {
    private BrokerDealerInitFlow(){}

    @StartableByRPC
    @InitiatingFlow
    public static class Issue extends FlowLogic<Void> {
        private List<Party> issuers;
        Party brokerDealer;

        public Issue(List<Party> issuers) {
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

    @StartableByRPC
    @InitiatingFlow
    public static class Update extends FlowLogic<Void>{
        List<Party> issuers;

        public Update(List<Party> issuers) {
            this.issuers = issuers;
        }

        @Override
        @Suspendable
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
}
