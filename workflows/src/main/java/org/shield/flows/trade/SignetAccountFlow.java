package org.shield.flows.trade;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.shield.flows.membership.MembershipFlows;
import org.shield.offer.OfferContract;
import org.shield.signet.SignetAccountContract;
import org.shield.signet.SignetAccountState;
import org.shield.trade.TradeContract;

public class SignetAccountFlow {
    private SignetAccountFlow(){
        // no instantiation
    }

    @InitiatingFlow
    @StartableByRPC
    public static class SetSignetAccount extends FlowLogic<Void>{
        private SignetAccountState signetAccountState;

        public SetSignetAccount(SignetAccountState signetAccountState) {
            this.signetAccountState = signetAccountState;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // must be a valid member
            if (!subFlow(new MembershipFlows.isMember())) throw new FlowException("Only valid active members can define a Signet wallet account");

            // account must not exists
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<SignetAccountState> stateAndRef : getServiceHub ().getVaultService().queryBy(SignetAccountState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().equals(signetAccountState)) throw new FlowException(String.format("Specified Signet account already exists. %s", signetAccountState.getWalletAddress()));
            }

            // caller must be the owner of the account ?
            Party caller = getOurIdentity();
            if (!caller.equals(signetAccountState.getOwner())) throw new FlowException("Caller must own the account being created");

            // we create the transaction
            Command createCommand = new Command<>(new SignetAccountContract.Commands.create(), caller.getOwningKey());
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(signetAccountState, SignetAccountContract.ID)
                .addCommand(createCommand);

            // we sign the transaction
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);
            return null;
        }
    }
}
