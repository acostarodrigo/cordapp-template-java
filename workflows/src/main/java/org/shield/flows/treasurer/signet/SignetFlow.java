package org.shield.flows.treasurer.signet;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import org.json.simple.parser.ParseException;
import org.shield.flows.membership.MembershipFlows;
import org.shield.flows.treasurer.USDFiatTokenFlow;
import org.shield.signet.SignetAccountState;
import org.shield.signet.SignetIssueTransactionState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class SignetFlow {
    private static SignetAPI signetAPI;

    private SignetFlow(){
        // we don't allow instantiation
    }

    /**
     * Configures Signet API parameters
     */
    private static class Configure extends FlowLogic<SignetAPI> {
        @Override
        @Suspendable
        public SignetAPI call() throws FlowException {
            if (!subFlow(new MembershipFlows.isTreasure())) throw new FlowException("Only an active treasurer organization can log in into Signature Bank");

            if (signetAPI == null){
                String url = subFlow(new ConfigurationFlow.GetURL());
                Map<String,String> header = subFlow(new ConfigurationFlow.GetLoginHeader());
                Map<String,String> loginParameters = subFlow(new ConfigurationFlow.GetLoginBody());

                signetAPI = new SignetAPI(url, header, loginParameters);
            }
            return signetAPI;
        }
    }


    /**
     * Takes care of sending funds from trader wallet to escrow account
     */
    private static class TransferToEscrow extends FlowLogic<Boolean>{
        private SignetAccountState source;
        private SignetAccountState escrow;
        private Amount amount;

        public TransferToEscrow(SignetAccountState source, SignetAccountState escrow, Amount amount) {
            this.source = source;
            this.escrow = escrow;
            this.amount = amount;
        }

        @Override
        @Suspendable
        public Boolean call() throws FlowException {
            // we get the Signet API
            if (signetAPI == null) signetAPI = subFlow(new Configure());

            // lets validate the specified wallet accounts exists on Signet
            try {
                if (!signetAPI.getUserWallets(source.getUserToken()).contains(source.getWalletAddress())) throw new FlowException(String.format("Specified source account doesn't contain wallet %s in Signet", source.getWalletAddress()));
                if (!signetAPI.getUserWallets(escrow.getUserToken()).contains(escrow.getWalletAddress())) throw new FlowException(String.format("Specified source account doesn't contain wallet %s in Signet", escrow.getWalletAddress()));
            } catch (Exception e) {
                throw new FlowException("Unable to validate if provided Signet wallet exists.");
            }

            // Lets validate that the account has enought balance on Signet to issue tokens
            try {
                long currentBalance = signetAPI.getWalletBalance(source.getUserToken(),source.getWalletAddress());
                if (currentBalance < amount.getQuantity()) {
                    //todo we might want to do a transfer from DDA account into wallet
                    throw new FlowException(String.format("Current balance %s is not enought to issue %s tokens", String.valueOf(currentBalance), String.valueOf(amount.getQuantity())));
                }
            } catch (Exception e) {
                throw new FlowException("Unable to determine current account balance on Signet.");
            }

            // we are ready to perform the transfer. We start the request
            try {
                String referenceId = signetAPI.transferSend(source.getWalletAddress(), escrow.getWalletAddress(), amount.getQuantity());

                // transfer is in progress. Let's check the status until completed or for 5 times
                SignetAPI.SendStatus sendStatus = null;
                for (int i=0; i<5; i++){
                    sendStatus = signetAPI.transferStatus(referenceId);

                    // transfer is done. we are good to go
                    if (sendStatus.equals(SignetAPI.SendStatus.DONE)){
                        return true;
                    }
                }
                // something is not right
                return false;
            } catch (Exception e) {
                throw new FlowException("Unable to transfer funds from specified accounts.");
            }
        }
    }

    /**
     * takes care of sending funds from trader to escrow and issue tokens to trader.
     */
    public static class TransferToEscrowAndIssue extends FlowLogic<Void>{
        private SignetIssueTransactionState signetIssueTransaction;

        public TransferToEscrowAndIssue(SignetIssueTransactionState signetIssueTransaction) {
            this.signetIssueTransaction = signetIssueTransaction;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // we will validate that this issue request doesn't exists from before
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<SignetIssueTransactionState> stateAndRef : getServiceHub().getVaultService().queryBy(SignetIssueTransactionState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getTransactionId().equals(signetIssueTransaction.getTransactionId())) throw new FlowException("Provided Signet issue state already exists");
            }

            // we will generate the transaction

            // get it signed

            // transfer from account to escrow
            boolean transferCompleted = subFlow(new TransferToEscrow(signetIssueTransaction.getSource(), signetIssueTransaction.getEscrow(), signetIssueTransaction.getAmount()));
            if (transferCompleted) {
                // update signet Issue State
                subFlow(new USDFiatTokenFlow.Issue(signetIssueTransaction.getSource().getOwner(), signetIssueTransaction.getAmount().getQuantity()));
                // update signet Issue State
            } else {
                // update Signet issue state with value
            }
            return null;
        }
    }
}
