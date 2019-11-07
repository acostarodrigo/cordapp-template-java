package org.shield.flows.arrangement;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.shield.contracts.ArrangementContract;
import org.shield.contracts.CommercialPaperContract;
import org.shield.states.ArrangementState;
import org.shield.states.CommercialPaperState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@InitiatingFlow
@StartableByRPC
public class IssueFlow  extends FlowLogic<UniqueIdentifier> {
    private UniqueIdentifier id;
    private ArrangementState input;
    private CommercialPaperState commercialPaperState;
    private Party issuer;
    private Party brokerDealer;
    private StateAndRef<ArrangementState> inputArrangement;
    private StateAndRef<CommercialPaperState> inputCommercialPaper;

    public IssueFlow(UniqueIdentifier arrangementId) {
        this.id = arrangementId;
    }

    @Override
    @Suspendable
    public UniqueIdentifier call() throws FlowException {
        this.issuer = getOurIdentity();

        // we get the arrangement from the Vault
        for (StateAndRef<ArrangementState> stateAndRef : getServiceHub ().getVaultService().queryBy(ArrangementState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(this.id)){
                this.input = stateAndRef.getState().getData();
                this.inputArrangement = stateAndRef;
            }
        }

        if (this.input == null)throw new FlowException("Arrangement with id " + this.id.toString() + " doesn't exists.");

        this.brokerDealer = this.input.getBrokerDealer();

        // we have the arrangement, now we need to validate if we have a commercial paper for it.
        for (StateAndRef<CommercialPaperState> stateAndRef : getServiceHub ().getVaultService().queryBy(CommercialPaperState.class).getStates()){
            if (stateAndRef.getState().getData().getOfferingDate().equals(input.getOfferingDate())){
                commercialPaperState = stateAndRef.getState().getData();
                inputCommercialPaper = stateAndRef;
            }
        }
        // we couldn't find a matching commercial paper, we will create it.
        if (commercialPaperState == null){
            UniqueIdentifier paperId = new UniqueIdentifier(CommercialPaperState.externalKey, UUID.randomUUID());
            commercialPaperState = new CommercialPaperState(paperId, this.issuer, input.getSize(), input.getOfferingDate());
        } else {
            int currentSize = commercialPaperState.getSize();
            int newSize = currentSize + input.getSize();
            commercialPaperState.setSize(newSize);
        }


        // now we generate the new output of the arrangement with the commercialPaper id attached.
        ArrangementState output = this.input;
        output.setPaperId(this.commercialPaperState.getId());
        output.setState(ArrangementState.State.ISSUED);

        // it is time to generate the transaction.
        List<PublicKey> requiredSigners = Arrays.asList(this.issuer.getOwningKey(), this.brokerDealer.getOwningKey());
        Command command = new Command<>(new ArrangementContract.Commands.issue(), requiredSigners);

        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(inputArrangement)
                .addOutputState(output, ArrangementContract.ID)
                .addOutputState(commercialPaperState, CommercialPaperContract.ID)
                .addCommand(command);

        // if we already had a commercial paper, we add the input
        if (inputCommercialPaper != null)
            txBuilder.addInputState(inputCommercialPaper);

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        FlowSession brokerDealerSession = initiateFlow(brokerDealer);
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                signedTx, Arrays.asList(brokerDealerSession)));

        // Finalising the transaction.
        subFlow(new FinalityFlow(fullySignedTx, brokerDealerSession));
        return commercialPaperState.getId();
    }
}
