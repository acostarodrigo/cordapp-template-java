package org.shield.flows.arrangement;

import co.paralleluniverse.fibers.Suspendable;
import org.shield.states.ArrangementState;
import org.shield.states.BrokerDealerInitState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(PreIssueFlow.class)
public class PreIssueResponseFlow extends FlowLogic<Void> {
    private FlowSession issuerSession;

    // constructor
    public PreIssueResponseFlow(FlowSession flowSession) {
        this.issuerSession = flowSession;
    }


    @Override
    @Suspendable
    public Void call() throws FlowException {
        // we validate the transaction first
        subFlow(new ValidateTx(this.issuerSession));

        subFlow(new ReceiveFinalityFlow(issuerSession));
        return null;
    }

    /**
     * Transaction validation depends if the broker Dealer has created the {@link BrokerDealerInitState state}.
     * If the broker dealer specified a list of Issuers to accept arrangements from, then we validate it.     *
     */
    private class ValidateTx extends SignTransactionFlow{
        public ValidateTx(@NotNull FlowSession otherSideSession) {
            super(otherSideSession);
        }

        @Override
        protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
            List<StateAndRef<BrokerDealerInitState>> brokerDealerInitStates = getServiceHub().getVaultService().queryBy(BrokerDealerInitState.class).getStates();

            if (!brokerDealerInitStates.isEmpty()) {
                BrokerDealerInitState brokerDealerInitState = brokerDealerInitStates.get(0).getState().getData();
                ArrangementState output = stx.getTx().outputsOfType(ArrangementState.class).get(0);
                requireThat(require -> {
                    require.using("Arrangement must be from authorized issuer.", brokerDealerInitState.getIssuers().contains(output.getIssuer()));
                    return null;
                });
            }
        }
    }
}