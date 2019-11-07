package org.shield.flows.init;

import co.paralleluniverse.fibers.Suspendable;

import org.shield.contracts.InitContract;
import org.shield.states.IssuerInitState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

import net.corda.core.contracts.Command;
import net.corda.core.identity.Party;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

@InitiatingFlow
@StartableByRPC
// start IssuerInit brokerDealers: "O=brokerDealer1,L=New York,C=US"
public class IssuerInit extends FlowLogic<Void> {
    private List<Party> brokerDealers;
    private Party issuer;

    public IssuerInit(List<Party> brokerDealers) {
        this.brokerDealers = brokerDealers;
    }

    @Override
    @Suspendable
    // start IssuerInit brokerDealers: "O=brokerDealer1,L=New York,C=US"
    // run vaultQuery contractStateType: org.shield.states.IssuerInitState
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
