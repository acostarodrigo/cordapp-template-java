package org.shield.flows.arrangement;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;
import org.shield.contracts.ArrangementContract;
import org.shield.flows.commercialPaper.CommercialPaperTokenFlow;
import org.shield.states.ArrangementState;
import org.shield.states.BrokerDealerInitState;
import org.shield.states.CommercialPaperState;
import org.shield.states.IssuerInitState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ArrangementFlow {
    // we are disabling instantiation
    private ArrangementFlow() {}

    @StartableByRPC
    @InitiatingFlow
    public static class Accept extends FlowLogic<Void> {
        private UniqueIdentifier id;
        private ArrangementState arrangementState;
        private StateAndRef<ArrangementState> input;
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

    @InitiatedBy(Accept.class)
    public static class AcceptResponse extends FlowLogic<Void>{
        private FlowSession brokerDealerSession;

        public AcceptResponse(FlowSession brokerDealerSession) {
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

    @InitiatingFlow
    @StartableByRPC
    public static class Cancel extends FlowLogic<Void> {
        private UniqueIdentifier id;
        private ArrangementState arrangementState;
        private StateAndRef<ArrangementState> input;
        private Party issuer;

        public Cancel(UniqueIdentifier id) {
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
            output.setState(ArrangementState.State.CANCELLED);

            // we are good to go. let's generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey());
            Command command = new Command<>(new ArrangementContract.Commands.cancel(), requiredSigners);

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

    @InitiatedBy(Cancel.class)
    public static class CancelResponse extends FlowLogic<Void>{
        private FlowSession brokerDealerSession;

        public CancelResponse(FlowSession brokerDealerSession) {
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

    @StartableByRPC
    @InitiatingFlow
    public static class Issue extends FlowLogic<UniqueIdentifier>{
        private UniqueIdentifier id;
        private ArrangementState input;
        private Party issuer;
        private Party brokerDealer;
        private StateAndRef<ArrangementState> inputArrangement;
        private StateAndRef<CommercialPaperState> inputCommercialPaper;

        public Issue(UniqueIdentifier arrangementId) {
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

            // we can't go on if we don't have it.
            if (this.input == null)throw new FlowException("Arrangement with id " + this.id.toString() + " doesn't exists.");

            this.brokerDealer = this.input.getBrokerDealer();

            // we issue the tokens.
            CommercialPaperTokenFlow.IssueFungibleToken issueFungibleToken = new CommercialPaperTokenFlow.IssueFungibleToken(input.getOfferingDate(), new Long(input.getSize()), this.brokerDealer);
            UniqueIdentifier paperId = subFlow(issueFungibleToken);

            // now we generate the new output of the arrangement with the commercialPaper id attached.
            ArrangementState output = this.input;
            output.setPaperId(paperId);
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

    @InitiatingFlow
    @StartableByRPC
    public static class PreIssue extends FlowLogic<UniqueIdentifier>{
        // we define the steps of the flow
        private final ProgressTracker.Step PREISSUING = new ProgressTracker.Step("Creating arrangement output and generating transaction.");
        private final ProgressTracker.Step ISSUER_SIGNING = new ProgressTracker.Step("Signing arrangement proposal from issuer.");
        private final ProgressTracker.Step BROKER_SIGNATURE = new ProgressTracker.Step("Getting broker dealer signature."){
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };

        private final ProgressTracker.Step RECORDING = new ProgressTracker.Step("Recording completed transaction"){
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        // and create the progress tracker with our own steps.
        private final ProgressTracker progressTracker = new ProgressTracker(
            PREISSUING,
            ISSUER_SIGNING,
            BROKER_SIGNATURE,
            RECORDING
        );

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
            this.issuer = getOurIdentity();

            // We retrieve the notary identity from the network map.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            progressTracker.setCurrentStep(PREISSUING);

            // We create the transaction components.
            // output is created without input for PreIssueFlow command
            UniqueIdentifier id = new UniqueIdentifier(ArrangementState.externalKey, UUID.randomUUID());
            ArrangementState outputState = new ArrangementState(id, this.issuer, this.brokerDealer, this.size, this.offeringDate, ArrangementState.State.PREISSUE);

            List<PublicKey> requiredSigners = Arrays.asList(this.issuer.getOwningKey(), this.brokerDealer.getOwningKey());
            // Command contract only requires the issuer signature.
            Command command = new Command<>(new ArrangementContract.Commands.preIssue(), requiredSigners);

            // We create a transaction builder and add the components.
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, ArrangementContract.ID)
                .addCommand(command);

            progressTracker.setCurrentStep(ISSUER_SIGNING);

            // Signing the transaction.
            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Obtaining the counterparty's signature.
            progressTracker.setCurrentStep(BROKER_SIGNATURE);
            FlowSession brokerDealerSession = initiateFlow(brokerDealer);
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                signedTx, Arrays.asList(brokerDealerSession)));

            progressTracker.setCurrentStep(RECORDING);

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
        public Void call() throws FlowException {
            // we validate the transaction first
            subFlow(new PreIssueResponse.ValidateTx(this.issuerSession));

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

}
