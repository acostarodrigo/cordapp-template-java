package org.shield.flows.init;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.shield.contracts.InitContract;
import org.shield.states.IssuerInitState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

public class IssuerInitFlow {
    private IssuerInitFlow(){}

    public class Issue extends FlowLogic<Void>{
        private List<Party> brokerDealers;
        private Party issuer;

        public Issue(List<Party> brokerDealers) {
            this.brokerDealers = brokerDealers;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // we create the issuer init State
            this.issuer = getOurIdentity();
            IssuerInitState outputState = new IssuerInitState(issuer, brokerDealers);

            // lets make sure there are no other issuerStates already created.
            List<StateAndRef<IssuerInitState>> issuerInitStates = getServiceHub().getVaultService().queryBy(IssuerInitState.class).getStates();
            if (!issuerInitStates.isEmpty()) throw new FlowException("Issuer Init state already exists. Modify with IssuerUpdate flow.");

            // We retrieve the notary identity from the network map.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // We create the transaction components.
            List<PublicKey> requiredSigners = Arrays.asList(this.issuer.getOwningKey());

            // Command contract only requires the issuer signature.
            Command command = new Command<>(new InitContract.Commands.issuerSet(), requiredSigners);

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

    public class Update extends FlowLogic<Void>{
        List<Party> brokerDealers;

        public Update(List<Party> brokerDealers) {
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
}
