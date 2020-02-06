package org.shield.flows.treasurer.signet;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.json.simple.JSONObject;
import org.shield.flows.membership.MembershipFlows;
import org.shield.flows.treasurer.USDFiatTokenFlow;
import org.shield.signet.IssueState;
import org.shield.signet.SignetAccountState;
import org.shield.signet.SignetIssueTransactionContract;
import org.shield.signet.SignetIssueTransactionState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class SignetFlow {
    private static SignetAPI signetAPI = null;

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
    private static class SendToEscrow extends FlowLogic<Boolean>{
        private SignetAccountState source;
        private SignetAccountState escrow;
        private Amount amount;

        public SendToEscrow(SignetAccountState source, SignetAccountState escrow, Amount amount) {
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
                String referenceId = signetAPI.sendRequest(source.getWalletAddress(), escrow.getWalletAddress(), amount.getQuantity());

                // transfer is in progress. Let's check the status until completed or for 5 times
                SignetAPI.SendStatus sendStatus = null;
                for (int i=0; i<5; i++){
                    sendStatus = signetAPI.sendStatus(referenceId);

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
    @InitiatingFlow
    public static class SendToEscrowAndIssue extends FlowLogic<Void>{
        private SignetIssueTransactionState signetIssueTransaction;

        public SendToEscrowAndIssue(SignetIssueTransactionState signetIssueTransaction) {
            this.signetIssueTransaction = signetIssueTransaction;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // only treasurer can call
            if (!subFlow(new MembershipFlows.isTreasure())) throw new FlowException("Only an active treasurer organization can issue tokerns.");

            // lets validate the state
            if (!signetIssueTransaction.getState().equals(IssueState.CREATED)) throw new FlowException("Issue transaction must be in Created status");

            // we will validate that this issue request doesn't exists from before
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<SignetIssueTransactionState> stateAndRef : getServiceHub().getVaultService().queryBy(SignetIssueTransactionState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getTransactionId().equals(signetIssueTransaction.getTransactionId())) throw new FlowException("Provided Signet issue state already exists");
            }

            // caller must be escrow owner
            Party caller = getOurIdentity();
            if (caller.equals(signetIssueTransaction.getEscrow().getOwner())) throw new FlowException("Escrow account owner is not the caller.");

            // we will generate the transaction
            List<PublicKey> requiredSigners = Arrays.asList(signetIssueTransaction.getSource().getOwner().getOwningKey(), caller.getOwningKey());
            Command createCommand = new Command<>(new SignetIssueTransactionContract.Commands.create(), requiredSigners);
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(signetIssueTransaction, SignetIssueTransactionContract.ID)
                .addCommand(createCommand);

            // get it signed
            SignedTransaction partiallysignedTransaction = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession traderSession = initiateFlow(signetIssueTransaction.getSource().getOwner());
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallysignedTransaction,Arrays.asList(traderSession)));

            // finalty for creation
            subFlow(new FinalityFlow(signedTransaction,traderSession));


            // transfer from account to escrow
            boolean transferCompleted = subFlow(new SendToEscrow(signetIssueTransaction.getSource(), signetIssueTransaction.getEscrow(), signetIssueTransaction.getAmount()));
            if (transferCompleted) {
                // update signet Issue State to TRANSFER_COMPLETE
                Command updateCommand = new Command<>(new SignetIssueTransactionContract.Commands.updateState(),requiredSigners);
                StateAndRef<SignetIssueTransactionState> input = signedTransaction.getCoreTransaction().outRef(0);
                signetIssueTransaction.setState(IssueState.TRANSFER_COMPLETE);
                txBuilder = new TransactionBuilder(notary)
                    .addInputState(input)
                    .addOutputState(signetIssueTransaction, SignetIssueTransactionContract.ID)
                    .addCommand(updateCommand);

                partiallysignedTransaction = getServiceHub().signInitialTransaction(txBuilder);
                signedTransaction = subFlow(new CollectSignaturesFlow(partiallysignedTransaction, Arrays.asList(traderSession)));
                subFlow(new FinalityFlow(signedTransaction, traderSession));

                // now we will issue the tokens
                subFlow(new USDFiatTokenFlow.Issue(signetIssueTransaction.getSource().getOwner(), signetIssueTransaction.getAmount().getQuantity()));
                // and change the status of the transaction
                input = signedTransaction.getCoreTransaction().outRef(0);
                signetIssueTransaction.setState(IssueState.ISSUE_COMPLETE);
                txBuilder = new TransactionBuilder(notary)
                    .addInputState(input)
                    .addOutputState(signetIssueTransaction, SignetIssueTransactionContract.ID)
                    .addCommand(updateCommand);

                partiallysignedTransaction = getServiceHub().signInitialTransaction(txBuilder);
                signedTransaction = subFlow(new CollectSignaturesFlow(partiallysignedTransaction, Arrays.asList(traderSession)));
                subFlow(new FinalityFlow(signedTransaction, traderSession));
            } else {
                // update signet Issue State to TRANSFER_COMPLETE
                Command updateCommand = new Command<>(new SignetIssueTransactionContract.Commands.updateState(),requiredSigners);
                StateAndRef<SignetIssueTransactionState> input = signedTransaction.getCoreTransaction().outRef(0);
                signetIssueTransaction.setState(IssueState.ERROR);
                txBuilder = new TransactionBuilder(notary)
                    .addInputState(input)
                    .addOutputState(signetIssueTransaction, SignetIssueTransactionContract.ID)
                    .addCommand(updateCommand);

                partiallysignedTransaction = getServiceHub().signInitialTransaction(txBuilder);
                signedTransaction = subFlow(new CollectSignaturesFlow(partiallysignedTransaction, Arrays.asList(traderSession)));
                subFlow(new FinalityFlow(signedTransaction, traderSession));
            }
            return null;
        }
    }

    @InitiatedBy(SendToEscrowAndIssue.class)
    public static class SendToEscrowAndIssueResponder extends FlowLogic<Void>{
        private FlowSession otherSession;

        public SendToEscrowAndIssueResponder(FlowSession otherSession) {
            this.otherSession = otherSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new ReceiveTransactionFlow(otherSession));
            subFlow(new ReceiveFinalityFlow(otherSession));
            return null;
        }
    }


    private static class DepositToEscrow extends FlowLogic<String>{
        private SignetAccountState escrow;
        private Amount amount;

        public DepositToEscrow(SignetAccountState escrow, Amount amount) {
            this.escrow = escrow;
            this.amount = amount;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            // we initialize the API
            signetAPI = subFlow(new Configure());

            // lets validate the escrow account exists on signet
            try {
                if (!signetAPI.getUserWallets(escrow.getUserToken()).contains(escrow.getWalletAddress())) throw new FlowException(String.format("Specified escrow account doesn't contain wallet %s in Signet", escrow.getWalletAddress()));
            } catch (Exception e) {
                throw new FlowException(String.format("Unable to determine if escrow account exists. No action was performed. Exception: %s", e.toString()));
            }

            String referenceId = null;
            try {
                JSONObject response = signetAPI.depositRequest(escrow.getWalletAddress(), amount.getQuantity());
                if (!response.get("Status").toString().equals("0")) {
                    // todo there was an error on the request. Handle
                }
                referenceId = response.get("ReferenceId").toString();
            } catch (Exception e) {
                throw new FlowException("Unable to process deposit request.");
            }

            String confirmationId = null;
            for (int i = 0; i<10;i++){
                try {
                    JSONObject response = signetAPI.depositStatus(referenceId);
                    if (!response.get("Status").toString().equals("0")){
                        // handle error in response
                    }
                    String depositStatus = response.get("RequestStatus").toString();
                    if (depositStatus.equals("Done")){
                        // todo get the confirmationId
                        confirmationId = response.get("ConfirmationId").toString();
                        break;
                    }
                } catch (Exception e) {
                    throw new FlowException(String.format("Unable to get status of deposit request %s", referenceId));
                }
            }
            return confirmationId;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class DepositToEscrowAndIssue extends FlowLogic<Void>{
        private SignetIssueTransactionState signetIssueTransactionState;
        private UUID transactionId;

        public DepositToEscrowAndIssue(SignetIssueTransactionState signetIssueTransactionState) {
            this.signetIssueTransactionState = signetIssueTransactionState;
            this.transactionId = signetIssueTransactionState.getTransactionId();
        }

        public DepositToEscrowAndIssue(UUID transactionId) {
            this.transactionId = transactionId;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // we initialize the API
            signetAPI = subFlow(new Configure());

            // validate if it already exist.
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            StateAndRef<SignetIssueTransactionState> input = null;
            for (StateAndRef<SignetIssueTransactionState> stateAndRef : getServiceHub().getVaultService().queryBy(SignetIssueTransactionState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getTransactionId().equals(transactionId)){
                    input = stateAndRef;
                    signetIssueTransactionState = stateAndRef.getState().getData();
                }
            }

            if (signetIssueTransactionState == null) throw new FlowException("Signet transaction does not exists.");

            String confirmationId = subFlow(new DepositToEscrow(signetIssueTransactionState.getEscrow(), signetIssueTransactionState.getAmount()));

            subFlow(new USDFiatTokenFlow.Issue(signetIssueTransactionState.getSource().getOwner(), signetIssueTransactionState.getAmount().getQuantity()));

            return null;
        }
    }
}
