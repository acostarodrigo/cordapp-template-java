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
import org.shield.states.ArrangementState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

@StartableByRPC
@InitiatingFlow
public class AcceptFlow extends FlowLogic<Void> {
    private UniqueIdentifier id;
    private ArrangementState arrangementState;
    private StateAndRef<ArrangementState> input;
    private Party issuer;
    private int size;

    public AcceptFlow(UniqueIdentifier id, int newSize) {
        this.id = id;
        this.size = newSize;
    }

    public AcceptFlow(UniqueIdentifier id) {
        this.id = id;
    }


    @Override
    @Suspendable
    public Void call() throws FlowException {
        // we get the arrangement state based in the id
        for (StateAndRef<ArrangementState> stateAndRef : getServiceHub().getVaultService().queryBy(ArrangementState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(this.id)) {
                this.arrangementState = stateAndRef.getState().getData();
                this.input =stateAndRef;
                this.issuer = this.arrangementState.getIssuer();
            }
        }

        // we couldn't find it. we are cancelling
        if (this.arrangementState == null) {
            throw new FlowException("Provided arrangement id " + id.toString() + " does not exists.");
        }

        // we change the state in the output
        ArrangementState output = arrangementState;
        output.setState(ArrangementState.State.ACCEPTED);

        if (this.size != 0)
            output.setSize(this.size);

        // we are good to go. let's generate the transaction
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey());
        Command command = new Command<>(new ArrangementContract.Commands.accept(), requiredSigners);

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(this.input)
                .addOutputState(output, ArrangementContract.ID)
                .addCommand(command);

        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        FlowSession issuerSession = initiateFlow(issuer);
        subFlow(new SendTransactionFlow(issuerSession, signedTx));
        subFlow(new FinalityFlow(signedTx,issuerSession));

        return null;
    }
}
