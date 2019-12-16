package org.shield.flows.trade;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;
import org.shield.trade.TradeContract;
import org.shield.flows.bond.BondFlow;
import org.shield.flows.membership.MembershipFlows;
import org.shield.trade.TradeState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TradeFlow {
    // we are disabling instantiation
    private TradeFlow() {}

    /**
     * Accept flow.
     */
    @StartableByRPC
    @InitiatingFlow
    public static class Accept extends FlowLogic<Void> {
        private UniqueIdentifier id;
        private TradeState tradeState;
        private StateAndRef<TradeState> input;
        private Party issuer;
        private int size;

        public Accept(UniqueIdentifier id, int newSize) {
            this.id = id;
            this.size = newSize;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // we get the arrangement state based in the id
            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class).getStates()){
                if (stateAndRef.getState().getData().getId().equals(this.id)) {
                    this.tradeState = stateAndRef.getState().getData();
                    this.input =stateAndRef;
                    this.issuer = this.tradeState.getIssuer();
                }
            }

            // we couldn't find it. we are cancelling
            if (this.tradeState == null) {
                throw new FlowException("Provided arrangement id " + id.toString() + " does not exists.");
            }

            // we change the state in the output
            TradeState output = tradeState;
            output.setState(TradeState.State.ACCEPTED);

            if (this.size != 0)
                output.setSize(this.size);

            // we are good to go. let's generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.accept(), requiredSigners);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(this.input)
                .addOutputState(output, TradeContract.ID)
                .addCommand(command);

            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            FlowSession issuerSession = initiateFlow(issuer);
            subFlow(new SendTransactionFlow(issuerSession, signedTx));
            subFlow(new FinalityFlow(signedTx,issuerSession));

            return null;
        }
    }

    @InitiatedBy(Accept.class)
    public static class AcceptResponse extends FlowLogic<Void>{
        private FlowSession brokerDealerSession;

        public AcceptResponse(FlowSession brokerDealerSession) {
            this.brokerDealerSession = brokerDealerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {

            subFlow(new ReceiveFinalityFlow(brokerDealerSession));

            return null;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class Cancel extends FlowLogic<Void> {
        private UniqueIdentifier id;
        private TradeState tradeState;
        private StateAndRef<TradeState> input;
        private Party issuer;

        public Cancel(UniqueIdentifier id) {
            this.id = id;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // we get the arrangement state based in the id
            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class).getStates()){
                if (stateAndRef.getState().getData().getId().equals(this.id)) {
                    this.tradeState = stateAndRef.getState().getData();
                    this.input =stateAndRef;
                    this.issuer = this.tradeState.getIssuer();
                }
            }

            // we couldn't find it. we are cancelling
            if (this.tradeState == null) {
                throw new FlowException("Provided arrangement id " + id.toString() + " does not exists.");
            }

            // we change the state in the output
            TradeState output = tradeState;
            output.setState(TradeState.State.CANCELLED);

            // we are good to go. let's generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.cancel(), requiredSigners);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(this.input)
                .addOutputState(output, TradeContract.ID)
                .addCommand(command);

            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            FlowSession issuerSession = initiateFlow(issuer);
            subFlow(new SendTransactionFlow(issuerSession, signedTx));
            subFlow(new FinalityFlow(signedTx,issuerSession));

            return null;
        }
    }

    @InitiatedBy(Cancel.class)
    public static class CancelResponse extends FlowLogic<Void>{
        private FlowSession brokerDealerSession;

        public CancelResponse(FlowSession brokerDealerSession) {
            this.brokerDealerSession = brokerDealerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {

            subFlow(new ReceiveFinalityFlow(brokerDealerSession));

            return null;
        }
    }

    @StartableByRPC
    @InitiatingFlow
    public static class Issue extends FlowLogic<UniqueIdentifier>{
        private UniqueIdentifier id;
        private TradeState input;
        private Party issuer;
        private Party brokerDealer;
        private StateAndRef<TradeState> inputArrangement;

        public Issue(UniqueIdentifier arrangementId) {
            this.id = arrangementId;
        }

        @Override
        @Suspendable
        public UniqueIdentifier call() throws FlowException {
            this.issuer = getOurIdentity();

            // we get the arrangement from the Vault
            for (StateAndRef<TradeState> stateAndRef : getServiceHub ().getVaultService().queryBy(TradeState.class).getStates()){
                if (stateAndRef.getState().getData().getId().equals(this.id)){
                    this.input = stateAndRef.getState().getData();
                    this.inputArrangement = stateAndRef;
                }
            }

            // we can't go on if we don't have it.
            if (this.input == null)throw new FlowException("Arrangement with id " + this.id.toString() + " doesn't exists.");

            this.brokerDealer = this.input.getBrokerDealer();

            // we issue the tokens.
            BondFlow.IssueFungibleToken issueFungibleToken = new BondFlow.IssueFungibleToken(input.getOfferingDate(), new Long(input.getSize()), this.brokerDealer);
            UniqueIdentifier paperId = subFlow(issueFungibleToken);

            // now we generate the new output of the arrangement with the commercialPaper id attached.
            TradeState output = this.input;
            output.setPaperId(paperId);
            output.setState(TradeState.State.ISSUED);

            // it is time to generate the transaction.
            List<PublicKey> requiredSigners = Arrays.asList(this.issuer.getOwningKey(), this.brokerDealer.getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.issue(), requiredSigners);

            // We retrieve the notary identity from the network map.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // We create a transaction builder and add the components.
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(inputArrangement)
                .addOutputState(output, TradeContract.ID)
                .addCommand(command);

            // Signing the transaction.
            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            FlowSession brokerDealerSession = initiateFlow(brokerDealer);
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                signedTx, Arrays.asList(brokerDealerSession)));

            // Finalising the transaction.
            subFlow(new FinalityFlow(fullySignedTx, brokerDealerSession));
            return paperId;
        }
    }

    @InitiatedBy(Issue.class)
    public static class IssueResponse extends FlowLogic<Void> {
        private FlowSession issuerSession;

        public IssueResponse(FlowSession issuerSession) {
            this.issuerSession = issuerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new SignTransactionFlow(issuerSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    TradeState input = null;

                    // before signing, we need to validate arrangement has not changed. And it is the same that we agreed.
                    TradeState output = stx.getTx().outputsOfType(TradeState.class).get(0);
                    for (StateAndRef<TradeState> stateAndRefs : getServiceHub().getVaultService().queryBy(TradeState.class).getStates()){
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

    @InitiatingFlow
    @StartableByRPC
    public static class PreIssue extends FlowLogic<UniqueIdentifier> {
        private Party issuer;
        private Party brokerDealer;
        private int size;
        private Date offeringDate;

        // constructor
        public PreIssue(Party brokerDealer, int size, Date offeringDate){
            this.brokerDealer = brokerDealer;
            this.size = size;
            this.offeringDate = offeringDate;
        }

        @Override
        @Suspendable
        public UniqueIdentifier call() throws FlowException {
            // we first validate caller is a valid issuer
            if (!subFlow(new MembershipFlows.isIssuer())) throw new FlowException("Only active issuer organizations can preIssue a bond.");

            this.issuer = getOurIdentity();

            // We retrieve the notary identity from the network map.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // We create the transaction components.
            // output is created without input for PreIssueFlow command
            UniqueIdentifier id = new UniqueIdentifier(TradeState.externalKey, UUID.randomUUID());
            TradeState outputState = new TradeState(id, this.issuer, this.brokerDealer, this.size, this.offeringDate, TradeState.State.PREISSUE);

            List<PublicKey> requiredSigners = Arrays.asList(this.issuer.getOwningKey(), this.brokerDealer.getOwningKey());
            // Command contract only requires the issuer signature.
            Command command = new Command<>(new TradeContract.Commands.preIssue(), requiredSigners);

            // We create a transaction builder and add the components.
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, TradeContract.ID)
                .addCommand(command);


            // Signing the transaction.
            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Obtaining the counterparty's signature.

            FlowSession brokerDealerSession = initiateFlow(brokerDealer);
            SignedTransaction fullySignedTx = null;

            fullySignedTx = subFlow(new CollectSignaturesFlow(
                signedTx, Arrays.asList(brokerDealerSession)));

            // Finalising the transaction.
            subFlow(new FinalityFlow(fullySignedTx, brokerDealerSession));

            return outputState.getId();
        }
    }

    @InitiatedBy(PreIssue.class)
    public static class PreIssueResponse extends  FlowLogic<Void> {
        private FlowSession issuerSession;


        // constructor
        public PreIssueResponse(FlowSession flowSession) {
            this.issuerSession = flowSession;
        }


        @Override
        @Suspendable
        public Void call() {
            // we validate the transaction first
            try {
                subFlow(new ValidateTx(this.issuerSession));
            } catch (FlowException e) {
                throw new IllegalArgumentException(e.getLocalizedMessage());
            }

            try {
                subFlow(new ReceiveFinalityFlow(issuerSession));
            } catch (FlowException e) {
                throw new IllegalArgumentException(e.getLocalizedMessage());
            }
            return null;
        }


        private class ValidateTx extends SignTransactionFlow{
            public ValidateTx(@NotNull FlowSession otherSideSession) {
                super(otherSideSession);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {


            }
        }
    }

}
