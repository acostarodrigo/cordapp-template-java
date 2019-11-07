package org.shield.flows.arrangement;

import co.paralleluniverse.fibers.Suspendable;
import org.shield.contracts.ArrangementContract;
import org.shield.states.ArrangementState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;


// start PreIssueFlow brokerDealer: "O=PartyB,L=New York,C=US", size: 100, offeringDate: "2019-10-18"
// run vaultQuery contractStateType: ArrangementState
@InitiatingFlow
@StartableByRPC
public class PreIssueFlow extends FlowLogic<UniqueIdentifier> {
    // we define the steps of the flow
    private final ProgressTracker.Step PREISSUING = new ProgressTracker.Step("Creating arrangement output and generating transaction.");
    private final ProgressTracker.Step ISSUER_SIGNING = new ProgressTracker.Step("Signing arrangement proposal from issuer.");
    private final ProgressTracker.Step BROKER_SIGNATURE = new ProgressTracker.Step("Getting broker dealer signature."){
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.tracker();
        }
    };

    private final ProgressTracker.Step RECORDING = new ProgressTracker.Step("Recording completed transaction"){
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    // and create the progress tracker with our own steps.
    private final ProgressTracker progressTracker = new ProgressTracker(
        PREISSUING,
        ISSUER_SIGNING,
        BROKER_SIGNATURE,
        RECORDING
    );

    private Party issuer;
    private Party brokerDealer;
    private int size;
    private Date offeringDate;

    // constructor
    public PreIssueFlow(Party brokerDealer, int size, Date offeringDate){
        this.brokerDealer = brokerDealer;
        this.size = size;
        this.offeringDate = offeringDate;
    }

    @Override
    @Suspendable
    public UniqueIdentifier call() throws FlowException {
        this.issuer = getOurIdentity();

        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        progressTracker.setCurrentStep(PREISSUING);

        // We create the transaction components.
        // output is created without input for PreIssueFlow command
        UniqueIdentifier id = new UniqueIdentifier(ArrangementState.externalKey, UUID.randomUUID());
        ArrangementState outputState = new ArrangementState(id, this.issuer, this.brokerDealer, this.size, this.offeringDate, ArrangementState.State.PREISSUE);

        List<PublicKey> requiredSigners = Arrays.asList(this.issuer.getOwningKey(), this.brokerDealer.getOwningKey());
        // Command contract only requires the issuer signature.
        Command command = new Command<>(new ArrangementContract.Commands.preIssue(), requiredSigners);

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
            .addOutputState(outputState, ArrangementContract.ID)
            .addCommand(command);

        progressTracker.setCurrentStep(ISSUER_SIGNING);

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Obtaining the counterparty's signature.
        progressTracker.setCurrentStep(BROKER_SIGNATURE);
        FlowSession brokerDealerSession = initiateFlow(brokerDealer);
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
            signedTx, Arrays.asList(brokerDealerSession)));

        progressTracker.setCurrentStep(RECORDING);

        // Finalising the transaction.
        subFlow(new FinalityFlow(fullySignedTx, brokerDealerSession));
        return outputState.getId();
    }
}





