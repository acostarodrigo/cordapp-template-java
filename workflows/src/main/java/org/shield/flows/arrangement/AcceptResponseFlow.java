package org.shield.flows.arrangement;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.shield.states.ArrangementState;
import org.shield.states.IssuerInitState;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(AcceptFlow.class)
public class AcceptResponseFlow extends FlowLogic<Void> {
    private FlowSession brokerDealerSession;

    public AcceptResponseFlow(FlowSession brokerDealerSession) {
        this.brokerDealerSession = brokerDealerSession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        SignedTransaction stx = subFlow(new ReceiveTransactionFlow(brokerDealerSession,false));
        // we will validate the transaction if we have the init configured.
        if (!getServiceHub().getVaultService().queryBy(IssuerInitState.class).getStates().isEmpty()){
            IssuerInitState issuerInitState = getServiceHub().getVaultService().queryBy(IssuerInitState.class).getStates().get(0).getState().getData();

            requireThat(require -> {
                require.using("Broker Dealer must be authorized.", issuerInitState.getBrokerDealers().contains(brokerDealerSession.getCounterparty()));

                ArrangementState output = stx.getCoreTransaction().outputsOfType(ArrangementState.class).get(0);
                require.using("Output must be from authorized broker.", issuerInitState.getBrokerDealers().contains(output.getBrokerDealer()));
                return null;
            });
        }
        subFlow(new ReceiveFinalityFlow(brokerDealerSession));

        return null;
    }
}
