package org.shield.flows.fiat;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.UnexpectedFlowEndException;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.shield.fiat.FiatContract;
import org.shield.fiat.FiatState;
import org.shield.fiat.FiatTransaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FiatFlow {
    private FiatFlow() {
    }

    public static class NewTransaction extends FlowLogic<Void>{
        private FiatTransaction fiatTransaction;

        public NewTransaction(FiatTransaction fiatTransaction) {
            this.fiatTransaction = fiatTransaction;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // we will validate if this node already has a FiatState
            VaultService vaultService = getServiceHub().getVaultService();
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            FiatState fiatState = null;
            StateAndRef<FiatState> input = null;
            for (StateAndRef<FiatState> stateAndRef : vaultService.queryBy(FiatState.class, criteria).getStates()){
                input = stateAndRef;
                fiatState = stateAndRef.getState().getData();
                break;
            }

            // we don't have one, lets create it
            Party caller = getOurIdentity();
            if (fiatState == null){
                fiatState = new FiatState(caller, Arrays.asList(fiatTransaction), Instant.now().getEpochSecond());
            } else {
                // fiat state exists, lets add the transaction
                List<FiatTransaction> fiatTransactions = new ArrayList<>(fiatState.getFiatTransactionList());
                fiatTransactions.add(fiatTransaction);
                fiatState.setFiatTransactionList(fiatTransactions);
            }


            // caller must be the issuer
            if (!caller.equals(fiatState.getIssuer())) throw new FlowException("Only the issuer of the FiatState can add a new transaction");

            // we generate the transaction
            Command newTransactionCommand = new Command(new FiatContract.Commands.newTransaction(),caller.getOwningKey());
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

            if (input != null) transactionBuilder.addInputState(input);
            transactionBuilder.addOutputState(fiatState, FiatContract.ID);
            transactionBuilder.addCommand(newTransactionCommand);

            // we sign it and store it only locally.
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);
            subFlow(new FinalityFlow(signedTransaction));
            return null;
        }
    }
}
