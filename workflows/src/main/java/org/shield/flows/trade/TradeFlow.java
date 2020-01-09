package org.shield.flows.trade;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.internal.flows.confidential.Exception;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.shield.bond.BondState;
import org.shield.flows.membership.MembershipFlows;
import org.shield.flows.offer.OfferFlow;
import org.shield.offer.OfferContract;
import org.shield.offer.OfferState;
import org.shield.trade.State;
import org.shield.trade.TradeContract;
import org.shield.trade.TradeState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import static com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt.tokenBalance;

public class TradeFlow {
    // we are disabling instantiation
    private TradeFlow() {}

    @StartableByRPC
    @InitiatingFlow
    public static class Create extends FlowLogic<UniqueIdentifier>{
        private TradeState trade;

        /**
         * Constructor
          * @param trade the trade we are sending.
         */
        public Create(TradeState trade) {
            this.trade = trade;
        }

        // progress tracker steps
        private static ProgressTracker.Step VALIDATE_OFFER = new ProgressTracker.Step("Validating offer of the trade.");
        private static ProgressTracker.Step VALIDATE_TRADE = new ProgressTracker.Step("Validating values of the trade.");
        private static ProgressTracker.Step TX_SIGNATURE = new ProgressTracker.Step("Signing and collecting signature from seller."){
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };
        private static ProgressTracker.Step FINISH = new ProgressTracker.Step("Finishing"){
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        public static final ProgressTracker progressTracker = new ProgressTracker(VALIDATE_OFFER, VALIDATE_TRADE, TX_SIGNATURE, FINISH);

        @Override
        @Suspendable
        public UniqueIdentifier call() throws FlowException {
            // we validate the caller is an issuer
            if (!subFlow(new MembershipFlows.isBuyer())) throw new FlowException("Only an active user can create a trade");

            // we validate caller is the buyer
            Party buyer = getOurIdentity();
            if (!trade.getBuyer().equals(buyer)) throw new FlowException("Trade creator is not the trade buyer");

            // we validate the offer included in the trade exists.
            progressTracker.setCurrentStep(VALIDATE_OFFER);
            boolean isOfferValid = false;
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<OfferState> stateAndRef : getServiceHub().getVaultService().queryBy(OfferState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().equals(trade.getOffer())) {
                    isOfferValid = true;
                    break;
                }
            }
            if (!isOfferValid) throw new FlowException(String.format("Provided offer %s in trade is not valid.", trade.getOffer().getOfferId().toString()));

            // we validate data of the trade
            progressTracker.setCurrentStep(VALIDATE_TRADE);
            trade.setState(State.PROPOSED);
            if (trade.getId() == null) trade.setId(new UniqueIdentifier());
            if (trade.getSize() > trade.getOffer().getAfsSize())
                throw new FlowException(String.format("Trade size is incorrect. AFS is %s while trade size is %s",String.valueOf(trade.getOffer().getAfsSize()),String.valueOf(trade.getSize())));
            Party caller = getOurIdentity();

            // Buyer must be the calling identity
            if (!caller.equals(trade.getBuyer())) throw new FlowException("Buyer of the trade must be the caller to create one.");

            // we create the transaction
            progressTracker.setCurrentStep(TX_SIGNATURE);
            List<PublicKey> signers = ImmutableList.of(trade.getBuyer().getOwningKey(),trade.getSeller().getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.Proposed(), signers);
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(trade, TradeContract.ID)
                .addCommand(command);

            // we are ready to sign and collect signatures
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession sellerSession = initiateFlow(trade.getSeller());
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTx, Arrays.asList(sellerSession),progressTracker));

            // we complete it.
            progressTracker.setCurrentStep(FINISH);
            subFlow(new FinalityFlow(signedTransaction,Arrays.asList(sellerSession),progressTracker));
            return trade.getId();
        }
    }

    @InitiatedBy(Create.class)
    public static class CreateResponse extends FlowLogic<Void> {
        private FlowSession callerSession;

        public CreateResponse(FlowSession callerSession) {
            this.callerSession = callerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // we run transaction validations before signing.
            subFlow(new SignTransactionFlow(this.callerSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    TradeState trade = (TradeState) stx.getCoreTransaction().outRef(0).getState().getData();

                    // we validate the buyer of the trade is who is requesting the signing
                    if (!trade.getBuyer().equals(callerSession.getCounterparty())) throw new FlowException();

                    // we validate trade is sent from a buyer organization
                    if (!subFlow(new MembershipFlows.isBuyer(trade.getBuyer()))) throw new FlowException(String.format("Trade buyer (%s) is not an active Buyer organization", trade.getBuyer().getName().getCommonName()));

                    // we must be the owners of the offer included in the trade
                    OfferState offer = trade.getOffer();
                    if (!offer.getIssuer().equals(getOurIdentity())) throw new FlowException("We are not the issuers of the Offer provided in the trade. Can't sign");

                    // offer in transaction must be valid at the time of signature. Since buyer is just an observer, our offer is the only valid one.
                    QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
                    boolean isOfferValid = false;
                    for (StateAndRef<OfferState> stateAndRef : getServiceHub().getVaultService().queryBy(OfferState.class,criteria).getStates()){
                        if (stateAndRef.getState().getData().equals(offer)){
                            isOfferValid = true;
                            break;
                        }
                    }
                    if (!isOfferValid) throw new FlowException("Provided offer in trade is no longer valid. Can't sign.");
                }
            });

            subFlow(new ReceiveFinalityFlow(callerSession));
            return null;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class Cancel extends FlowLogic<Void>{
        private UniqueIdentifier tradeId;

        // constructor
        public Cancel(UniqueIdentifier tradeId) {
            this.tradeId = tradeId;
        }

        // progress tracker steps
        private static ProgressTracker.Step VALIDATE_TRADE = new ProgressTracker.Step("Validating values of the trade.");
        private static ProgressTracker.Step TX_SIGNATURE = new ProgressTracker.Step("Signing and collecting signature from seller."){
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };
        private static ProgressTracker.Step FINISH = new ProgressTracker.Step("Finishing"){
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        public static final ProgressTracker progressTracker = new ProgressTracker(VALIDATE_TRADE, TX_SIGNATURE, FINISH);

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // check permissions.
            if (!subFlow(new MembershipFlows.isSeller())) throw new FlowException("Only an active Seller organization can cancel a trade.");

            // get the state
            progressTracker.setCurrentStep(VALIDATE_TRADE);
            StateAndRef<TradeState> tradeStateStateAndRef = null;
            TradeState trade = null;
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(this.tradeId)){
                    tradeStateStateAndRef = stateAndRef;
                    trade = stateAndRef.getState().getData();
                    break;
                }
            }
            // lets make sure the trade we are cancelling exists.
            if (tradeStateStateAndRef == null || trade == null) throw new FlowException(String.format("Specified trade %s does not exists.", this.tradeId.toString()));

            // we can cacel only trades if we are the seller
            Party caller = getOurIdentity();
            if (!caller.equals(trade.getSeller())) throw new FlowException("Can cancel a trade if we are not the seller party");

            // trade must be in Proposed state.
            if (!trade.getState().equals(State.PROPOSED)) throw new FlowException(String.format("Can cancel a state not in Proposed state. Current state is %s", trade.getState().toString()));

            // we are ready to cancel it.
            progressTracker.setCurrentStep(TX_SIGNATURE);
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            // we require signatures from both buyer and seller
            List<PublicKey> requiredSigners = Arrays.asList(caller.getOwningKey(),trade.getBuyer().getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.Cancelled(), requiredSigners);

            trade.setState(State.CANCELLED);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(tradeStateStateAndRef)
                .addOutputState(trade, TradeContract.ID)
                .addCommand(command);

            // we collect signature from the buyer
            SignedTransaction partiallysignedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession buyerSession = initiateFlow(trade.getBuyer());
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallysignedTx, Arrays.asList(buyerSession), progressTracker));

            progressTracker.setCurrentStep(FINISH);
            subFlow(new FinalityFlow(signedTransaction,Arrays.asList(buyerSession),progressTracker));

            return null;
        }
    }

    @InitiatedBy(Cancel.class)
    public static class CancelResponse extends FlowLogic<Void>{
        private FlowSession callerSession;

        public CancelResponse(FlowSession callerSession) {
            this.callerSession = callerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new SignTransactionFlow(this.callerSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // we get the trade being cancelled
                    TradeState trade = (TradeState) stx.getCoreTransaction().getOutput(0);

                    // we make sure cancellation comes from the seller of the trade
                    if (!callerSession.getCounterparty().equals(trade.getSeller())) throw new FlowException(String.format("Cancellation of trade is not from the correct Seller. %s", callerSession.getCounterparty().toString()));
                }
            });
            subFlow(new ReceiveFinalityFlow(callerSession));
            return null;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class Accept extends FlowLogic<SignedTransaction> {
        private UniqueIdentifier tradeId;

        // constructor
        public Accept(UniqueIdentifier tradeId) {
            this.tradeId = tradeId;
        }

        // progress tracker steps
        private static ProgressTracker.Step VALIDATE_TRADE = new ProgressTracker.Step("Validating values of the trade.");
        private static ProgressTracker.Step VALIDATE_OFFER = new ProgressTracker.Step("Validating offer provided in the trade.");
        private static ProgressTracker.Step TX_SIGNATURE = new ProgressTracker.Step("Signing and collecting signature from participants."){
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };
        private static ProgressTracker.Step FINISH = new ProgressTracker.Step("Finishing"){
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };
        private static ProgressTracker.Step NOTIFY_BUYERS = new ProgressTracker.Step("Notifying buyers of changed offer"){
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return OfferFlow.NotifyBuyers.progressTracker;
            }
        };

        public static final ProgressTracker progressTracker = new ProgressTracker(VALIDATE_TRADE, VALIDATE_OFFER, TX_SIGNATURE, FINISH, NOTIFY_BUYERS);

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // lets make sure is a seller.
            if (!subFlow(new MembershipFlows.isSeller())) throw new FlowException("Must be a valid Seller organization to accept trade.");

            progressTracker.setCurrentStep(VALIDATE_TRADE);
            // we must have the trade already in order to accept it.
            StateAndRef<TradeState> tradeStateStateAndRef = null;
            TradeState trade = null;
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(this.tradeId)){
                    tradeStateStateAndRef = stateAndRef;
                    trade = stateAndRef.getState().getData();
                    break;
                }
            }
            // lets make sure the trade to accept exists.
            if (tradeStateStateAndRef == null || trade == null) throw new FlowException(String.format("Specified trade %s does not exists.", this.tradeId.toString()));

            // trade must include last Offer State
            progressTracker.setCurrentStep(VALIDATE_OFFER);
            StateAndRef<OfferState> offerStateStateAndRef = null;
            OfferState offer = null;
            for (StateAndRef<OfferState> stateAndRef : getServiceHub().getVaultService().queryBy(OfferState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().equals(trade.getOffer())){
                    offerStateStateAndRef = stateAndRef;
                    offer = stateAndRef.getState().getData();
                }
            }
            if (offer == null || offerStateStateAndRef == null) throw new FlowException("Provided offer in trade is not valid.");

            // caller must be the issuer of the trade
            Party caller = getOurIdentity();
            if (!caller.equals(offer.getIssuer())) throw new FlowException("Caller is not the issuer of the provided trade. Can't accept.");
            // and also the seller of the trade
            if (!caller.equals(trade.getSeller())) throw new FlowException("Caller is not the seller of the trade. Can't accept");
            // trade must be in Proposed state
            if (!trade.getState().equals(State.PROPOSED)) throw new FlowException(String.format("Trade is not in Proposed state. Current state is %s", trade.getState().toString()));

            // we are ready to accept it.
            progressTracker.setCurrentStep(TX_SIGNATURE);
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(trade.getBuyer().getOwningKey(), trade.getSeller().getOwningKey());

            Command command = new Command<>(new TradeContract.Commands.Pending(), requiredSigners);
            trade.setState(State.PENDING);

            long currentAfsSize = offer.getAfsSize();
            offer.setAfsSize(currentAfsSize-trade.getSize());

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(tradeStateStateAndRef)
                .addInputState(offerStateStateAndRef)
                .addOutputState(trade, TradeContract.ID)
                .addOutputState(offer, OfferContract.ID)
                .addCommand(command);

            // lets collect signatures
            FlowSession buyerSession = initiateFlow(trade.getBuyer());
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partiallySignedTx, ImmutableList.of(buyerSession)));


            progressTracker.setCurrentStep(FINISH);
            subFlow(new FinalityFlow(fullySignedTx,buyerSession));

            // now we will notify all buyers about the new offer status
            progressTracker.setCurrentStep(NOTIFY_BUYERS);
            subFlow(new OfferFlow.NotifyBuyers(offerStateStateAndRef, offer));

            return fullySignedTx;
        }
    }

    @InitiatedBy(Accept.class)
    public static class AcceptResponse extends FlowLogic<Void> {
        private FlowSession callingSession;

        public AcceptResponse(FlowSession callingSession) {
            this.callingSession = callingSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // trade is just being accepted, we will validate transaction
            subFlow(new SignTransactionFlow(callingSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // trade seller and offer issuer must be from the caller
                    TradeState trade = (TradeState) stx.getCoreTransaction().getOutput(0);
                    OfferState offer = (OfferState) stx.getCoreTransaction().getOutput(1);
                    if (!trade.getSeller().equals(callingSession.getCounterparty())) throw new FlowException("Can't accept trade. Caller is not the seller.");
                    if (!offer.getIssuer().equals(callingSession.getCounterparty())) throw new FlowException("Can't accept trade. Caller is not the issuer of the offer");
                }
            });

            SignedTransaction signedTransaction = subFlow(new ReceiveFinalityFlow(callingSession));
            TradeState trade = (TradeState) signedTransaction.getCoreTransaction().getOutput(0);
            // now we will try to pay for it. Lets see if we have the money
            TokenType fiatCurrency = FiatCurrency.Companion.getInstance(trade.getCurrency().getCurrencyCode());
            Amount fiatBalance = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(), fiatCurrency);

            // we are ready to pay for it.
            if (fiatBalance.getQuantity() >= trade.getSize()){
                subFlow(new Settle(trade.getId()));
            }
            return null;
        }
    }

    @InitiatedBy(GenerateSettleTx.class)
    private static class SettleResponse extends FlowLogic<SignedTransaction>{
        private FlowSession callingSession;

        public SettleResponse(FlowSession callingSession) {
            this.callingSession = callingSession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            TradeState trade = callingSession.receive(TradeState.class).unwrap(amount -> amount);

            // trade is being payed, so we are sending the bond
            BondState bond = trade.getBond();
            TokenSelection tokenSelection = new TokenSelection(getServiceHub(), 8, 100, 2000);
            // we get the pointer based on the bond of the trade
            TokenPointer tokenPointer = bond.toPointer(bond.getClass());
            Amount amount = new Amount(trade.getSize(), tokenPointer);
            PartyAndAmount partyAndAmount = new PartyAndAmount<>(callingSession.getCounterparty(), amount);
            // we generate inputs and outputs of the bond and send them to the buyer
            Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs =
                tokenSelection.generateMove(getRunId().getUuid(), ImmutableList.of(partyAndAmount), getOurIdentity(), null);
            subFlow(new SendStateAndRefFlow(callingSession, inputsAndOutputs.getFirst()));
            callingSession.send(inputsAndOutputs.getSecond());

            // at this point, buyer has send us the fiat token, we will validate this.
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(callingSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    TokenType fiatCurrency = FiatCurrency.Companion.getInstance(trade.getCurrency().getCurrencyCode());
                    // validate output has fiat currency and with the correct amount
                    // and we are the destination of the fiat.

                }
            });
            subFlow(new UpdateDistributionListFlow(signedTransaction));
            return null;
        }
    }

    @InitiatingFlow
    private static class GenerateSettleTx extends FlowLogic<TransactionBuilder>{
        private TradeState trade;
        private TransactionBuilder txBuilder;

        public GenerateSettleTx(TradeState trade, TransactionBuilder txBuilder) {
            this.trade = trade;
            this.txBuilder = txBuilder;
        }

        @Suspendable
        @Override
        public TransactionBuilder call() throws FlowException {
            // must be buyer
            if (!subFlow(new MembershipFlows.isBuyer())) throw new FlowException("Must be an active registered buyer to settle a trade.");

            // trade must exists
            VaultService vaultService = getServiceHub().getVaultService();
            StateAndRef<TradeState> input = null;
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<TradeState> stateAndRef : vaultService.queryBy(TradeState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(trade.getId())) {
                    input = stateAndRef;
                    break;
                }
            }

            if (input==null) throw new FlowException(String.format("Specified trade %s does not exists.", trade.getId().toString()));

            // trade must be in accepted status
            if (!trade.getState().equals(State.PENDING)) throw new FlowException(String.format("Trade is not in Accepted status. Current status is %s", trade.getState()));

            // lets validate we have the balance to pay
            TokenType fiatToken = FiatCurrency.Companion.getInstance(trade.getCurrency().getCurrencyCode());
            if (tokenBalance(vaultService,fiatToken).getQuantity() < trade.getSize()) throw new FlowException("Not enought fiat balance to settle trade.");

            List<PublicKey> requiredSigners = ImmutableList.of(trade.getBuyer().getOwningKey(), trade.getSeller().getOwningKey());
            // and we add the settle command to perform validations on the contract.
            Command settle = new Command<>(new TradeContract.Commands.Settled(), requiredSigners);
            txBuilder.addCommand(settle);

            Amount fiatAmount = new Amount(trade.getSize(),fiatToken);
            // we generate outputs and inputs for our fiat token
            MoveTokensUtilitiesKt.addMoveFungibleTokens(txBuilder,getServiceHub(),fiatAmount,trade.getSeller(), getOurIdentity());

            // we send the update trade to the seller so that he sends the bond token
            FlowSession sellerSession = initiateFlow(trade.getSeller());
            sellerSession.send(trade);
            // we receive the inputs
            List<StateAndRef<FungibleToken>> inputs =  subFlow(new ReceiveStateAndRefFlow<>(sellerSession));
            // we received the outputs
            List<FungibleToken> outputs = sellerSession.receive(List.class).unwrap(value -> value);
            MoveTokensUtilitiesKt.addMoveTokens(txBuilder, inputs, outputs);
            // at this point, transaction includes my fiat token and seller bond tokens.

            return txBuilder;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class Settle extends FlowLogic<SignedTransaction>{
        private UniqueIdentifier tradeId;

        public Settle(UniqueIdentifier tradeId) {
            this.tradeId = tradeId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            if (!subFlow(new MembershipFlows.isBuyer())) throw new FlowException("Only an active buyer organization can settle a trade.");

            TradeState trade = null;
            StateAndRef<TradeState> input = null;
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(tradeId)){
                    input = stateAndRef;
                    trade = input.getState().getData();
                    break;
                }
            }

            if (input == null || trade == null)  throw new FlowException(String.format("Trade with id %s was not found on this node.", tradeId.toString()));

            // trade must be accepted and not payed yet.
            if (!trade.getState().equals(State.PENDING)) throw new FlowException(String.format("Trade must be in Accepted_NotPayed state to settle. current state is %s", trade.getState().toString()));

            // we must be the owners of the buyers of the trade
            Party caller = getOurIdentity();
            if (!trade.getBuyer().equals(caller)) throw new FlowException("Caller is not the owner of the trade we are trying to settle.");

            // we are good to go. let's generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(trade.getSeller().getOwningKey(), caller.getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.Settled(), requiredSigners);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(input)
                .addCommand(command);

            // we generate the trade transaction
            txBuilder = subFlow(new GenerateSettleTx(trade, txBuilder));

            // and add the trade as output.
            trade.setState(State.SETTLED);
            txBuilder.addOutputState(trade, TradeContract.ID);

            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // collect signature from the seller. he will validate trade and bond are accurate.
            FlowSession sellerSession = initiateFlow(trade.getSeller());
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTx, Arrays.asList(sellerSession)));

            // we are done.
            subFlow(new FinalityFlow(signedTransaction,sellerSession));
            return signedTransaction;
        }
    }

    @InitiatedBy(Settle.class)
    public static class SettleAcceptedResponse extends FlowLogic<Void>{
        private FlowSession callerSession;

        public SettleAcceptedResponse(FlowSession callerSession) {
            this.callerSession = callerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new SignTransactionFlow(callerSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // todo add validations.
                }
            });

            subFlow(new ReceiveFinalityFlow(callerSession));
            return null;
        }
    }

}
