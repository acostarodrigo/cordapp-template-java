package org.shield.flows.arrangement;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;
import org.shield.states.ArrangementState;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(IssueFlow.class)
public class IssueResponseFlow extends FlowLogic<Void> {
    private FlowSession issuerSession;

    public IssueResponseFlow(FlowSession issuerSession) {
        this.issuerSession = issuerSession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new SignTransactionFlow(issuerSession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                ArrangementState input = null;

                // before signing, we need to validate arrangement has not changed. And it is the same that we agreed.
                ArrangementState output = stx.getTx().outputsOfType(ArrangementState.class).get(0);
                for (StateAndRef<ArrangementState> stateAndRefs : getServiceHub().getVaultService().queryBy(ArrangementState.class).getStates()){
                    if (stateAndRefs.getState().getData().getId().equals(output.getId())){
                        input = stateAndRefs.getState().getData();
                    }
                }

                if (input == null) throw new FlowException("Provided Arrangement does not exists.");

                if (!output.equals(input)) throw new FlowException("Can't sign because arragement is not equal to what we accepted.");
            }
        });

        subFlow(new ReceiveFinalityFlow(issuerSession));
        return null;
    }
}
