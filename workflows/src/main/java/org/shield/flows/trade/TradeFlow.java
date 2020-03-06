package org.shield.flows.trade;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract;
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import kotlin.Pair;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.intellij.lang.annotations.Flow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.shield.bond.BondState;
import org.shield.custodian.CustodianState;
import org.shield.flows.custodian.CustodianFlows;
import org.shield.flows.membership.MembershipFlows;
import org.shield.flows.offer.OfferFlow;
import org.shield.offer.OfferContract;
import org.shield.offer.OfferState;
import org.shield.trade.State;
import org.shield.trade.TradeContract;
import org.shield.trade.TradeState;

import java.security.PublicKey;
import java.util.ArrayList;
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
            // we validate the caller is either a buyer or seller.
            if (!subFlow(new MembershipFlows.isBuyer()) && !subFlow(new MembershipFlows.isSeller())) throw new FlowException("Only an active trader can create a trade");

            // we validate caller is the buyer or seller
            Party caller = getOurIdentity();
            if (!trade.getBuyer().equals(caller) && !trade.getSeller().equals(caller)) throw new FlowException("Trade creator is not a trade participant");
            Party other;
            if (caller.equals(trade.getBuyer()))
               other = trade.getSeller();
            else
                other = trade.getBuyer();

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


            // we create the transaction
            progressTracker.setCurrentStep(TX_SIGNATURE);
            List<PublicKey> signers = ImmutableList.of(caller.getOwningKey(), other.getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.Proposed(), signers);
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(trade, TradeContract.ID)
                .addCommand(command);

            // we are ready to sign and collect signatures
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession otherSession = initiateFlow(other);
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTx, Arrays.asList(otherSession)));

            // we complete it.
            progressTracker.setCurrentStep(FINISH);
            subFlow(new FinalityFlow(signedTransaction,Arrays.asList(otherSession)));

            // we send to the custodian
            subFlow(new CustodianFlows.SendTrade(trade.getId()));
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

                    // we validate that sender is a participant in the deal
                    if (!trade.getBuyer().equals(callerSession.getCounterparty()) && !trade.getSeller().equals(callerSession.getCounterparty())) throw new FlowException("Sender is not a participant on the deal. Can't sign.");

                    // we validate that we are a participant on the deal
                    OfferState offer = trade.getOffer();
                    Party signer = getOurIdentity();
                    if (!trade.getBuyer().equals(signer) && !trade.getSeller().equals(signer)) throw new FlowException("We are not a participant on the deal. Can't sign.");

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
    public static class CancelSeller extends FlowLogic<Void>{
        private UniqueIdentifier tradeId;

        // constructor
        public CancelSeller(UniqueIdentifier tradeId) {
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
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallysignedTx, Arrays.asList(buyerSession)));

            progressTracker.setCurrentStep(FINISH);
            subFlow(new FinalityFlow(signedTransaction,Arrays.asList(buyerSession)));

            return null;
        }
    }

    @InitiatedBy(CancelSeller.class)
    public static class CancelSellerResponse extends FlowLogic<Void>{
        private FlowSession callerSession;

        public CancelSellerResponse(FlowSession callerSession) {
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
    public static class AcceptSeller extends FlowLogic<SignedTransaction> {
        private UniqueIdentifier tradeId;

        // constructor
        public AcceptSeller(UniqueIdentifier tradeId) {
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

            // current offer must be big enought to include this trade
            progressTracker.setCurrentStep(VALIDATE_OFFER);
            StateAndRef<OfferState> offerStateStateAndRef = null;
            OfferState offer = null;
            for (StateAndRef<OfferState> stateAndRef : getServiceHub().getVaultService().queryBy(OfferState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getOfferId().equals(trade.getOffer().getOfferId())){
                    offerStateStateAndRef = stateAndRef;
                    offer = stateAndRef.getState().getData();
                }
            }
            if (offer == null || offerStateStateAndRef == null) throw new FlowException("Provided offer in trade is not valid.");

            if (offer.getAfsSize() < trade.getSize()) throw new FlowException(String.format("Not enought Available for Sale bonds in the offer %s to accept the trace", offer.getOfferId().toString()));

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

            Command tradeCommand = new Command<>(new TradeContract.Commands.Pending(), requiredSigners);
            Command offerCommand = new Command<>(new OfferContract.Commands.notifyBuyers(), trade.getSeller().getOwningKey());
            trade.setState(State.PENDING);

            long currentAfsSize = offer.getAfsSize();
            offer.setAfsSize(currentAfsSize-trade.getSize());

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(tradeStateStateAndRef)
                .addInputState(offerStateStateAndRef)
                .addOutputState(trade, TradeContract.ID)
                .addOutputState(offer, OfferContract.ID)
                .addCommand(offerCommand)
                .addCommand(tradeCommand);

            // lets collect signatures
            FlowSession buyerSession = initiateFlow(trade.getBuyer());
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partiallySignedTx, ImmutableList.of(buyerSession)));

            progressTracker.setCurrentStep(FINISH);
            subFlow(new FinalityFlow(fullySignedTx,buyerSession));



            // now we will notify all buyers about the new offer status
            progressTracker.setCurrentStep(NOTIFY_BUYERS);
            subFlow(new OfferFlow.NotifyBuyers(fullySignedTx.getCoreTransaction().outRef(1), offer));

            return fullySignedTx;
        }
    }

    @InitiatedBy(AcceptSeller.class)
    public static class AcceptSellerResponse extends FlowLogic<Void> {
        private FlowSession callingSession;

        public AcceptSellerResponse(FlowSession callingSession) {
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
    private static class GenerateSettleTxResponse extends FlowLogic<SignedTransaction>{
        private FlowSession callingSession;

        public GenerateSettleTxResponse(FlowSession callingSession) {
            this.callingSession = callingSession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            TradeState trade = callingSession.receive(TradeState.class).unwrap(amount -> amount);

            // trade is being payed, so we are sending the bond
            BondState bond = trade.getOffer().getBond();
            TokenSelection tokenSelection = new TokenSelection(getServiceHub(), 8, 100, 2000);

            // we get the pointer based on the bond of the trade
            TokenPointer tokenPointer = bond.toPointer(bond.getClass());
            Amount amount = new Amount(trade.getSize(), tokenPointer);
            PartyAndAmount partyAndAmount = new PartyAndAmount<>(callingSession.getCounterparty(), amount);



            // we generate inputs and outputs of the bond and send them to the buyer
            Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs =
                tokenSelection.generateMove(getRunId().getUuid(), ImmutableList.of(partyAndAmount), getOurIdentity(), null);
            // we send the intputs and outputs to the buyer
            subFlow(new SendStateAndRefFlow(callingSession, inputsAndOutputs.getFirst()));
            callingSession.send(inputsAndOutputs.getSecond());

            return null;
        }
    }

    @InitiatingFlow
    private static class GenerateSettleTx extends FlowLogic<TransactionBuilder>{
        private TradeState trade;

        public GenerateSettleTx(TradeState trade) {
            this.trade = trade;
        }

        @Suspendable
        @Override
        public TransactionBuilder call() throws FlowException {
            // we build the txBuilder
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder txBuilder = new TransactionBuilder(notary);

            // must be buyer
            if (!subFlow(new MembershipFlows.isBuyer())) throw new FlowException("Must be an active registered buyer to settle a trade.");
            Party caller = getOurIdentity();

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

            Amount fiatAmount = new Amount(trade.getSize(),fiatToken);
            // we generate outputs and inputs for our fiat token
            txBuilder = MoveTokensUtilitiesKt.addMoveFungibleTokens(txBuilder,getServiceHub(),fiatAmount, trade.getSeller(), caller);

            // we send the update trade to the seller so that he sends the bond token
            FlowSession sellerSession = initiateFlow(trade.getSeller());
            sellerSession.send(trade);
            // we receive the inputs
            List<StateAndRef<FungibleToken>> inputs =  subFlow(new ReceiveStateAndRefFlow<>(sellerSession));
            // we received the outputs
            List<FungibleToken> outputs = sellerSession.receive(List.class).unwrap(value -> value);

            // Since we don't have local states of BondToken, the StateAndRef validations fails, but apparently inputs and outputs are still being added to tx Builder.
            //txBuilder = MoveTokensUtilitiesKt.addMoveTokens(txBuilder, inputs, outputs);
            List<Integer> inputIndexes = new ArrayList<>();
            int inputIndex = txBuilder.inputStates().size() -1;
            for (StateAndRef<FungibleToken> sellerInput : inputs){
                try {
                    inputIndex++;
                    inputIndexes.add(inputIndex);
                    txBuilder.addInputState(sellerInput);
                } catch (IllegalStateException e){
                    System.out.println(e.getMessage());
                }

            }
            List<Integer> outputIndexes = new ArrayList<>();
            int outputIndex = txBuilder.outputStates().size() -1;
            for (FungibleToken sellerOutput : outputs){
                try {
                    outputIndex++;
                    outputIndexes.add(outputIndex);
                    txBuilder.addOutputState(sellerOutput);
                } catch (IllegalStateException e){
                    System.out.println(e.getMessage());
                }
            }
            // Since MoveTokensUitilitiesKt is not working, I have to manually add the command.
            IssuedTokenType issuedTokenType = outputs.get(0).getIssuedTokenType();

            // todo indexes of inputs and outputs must be determined dinamically
            Command bondTokenCommand = new Command<>(new MoveTokenCommand(issuedTokenType, inputIndexes,outputIndexes), trade.getSeller().getOwningKey());
            txBuilder.addCommand(bondTokenCommand);



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
            if (!trade.getState().equals(State.PENDING)) throw new FlowException(String.format("Trade must be in Pending state to settle. current state is %s", trade.getState().toString()));

            // we must be the owners of the buyers of the trade
            Party caller = getOurIdentity();
            if (!trade.getBuyer().equals(caller)) throw new FlowException("Caller is not the owner of the trade we are trying to settle.");

            // we are good to go. let's generate the transaction
            List<PublicKey> requiredSigners = Arrays.asList(trade.getSeller().getOwningKey(), caller.getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.Settled(), requiredSigners);

            // we generate the trade transaction with the fiat and bond tokens inputs outputs
            TransactionBuilder txBuilder = subFlow(new GenerateSettleTx(trade));
            // we add the trade inputs and outputs.
            txBuilder.addInputState(input)
                .addCommand(command);

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
    public static class SettleResponse extends FlowLogic<Void>{
        private FlowSession callerSession;

        public SettleResponse(FlowSession callerSession) {
            this.callerSession = callerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new SignTransactionFlow(callerSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // we will validate that the buyer is sending us the correct amount of fiat tokens and is signed by him
                    // before submitting the signature
                    TradeState trade = stx.getCoreTransaction().outputsOfType(TradeState.class).get(0);
                    TokenType fiatCurrency = FiatCurrency.Companion.getInstance(trade.getCurrency().getCurrencyCode());
                    boolean isFiatToken = false;
                    for (FungibleToken token : stx.getCoreTransaction().outputsOfType(FungibleToken.class)){
                        if (token.getIssuedTokenType().getTokenType().equals(fiatCurrency)){
                            // we will only focus in the output that put us as new holder
                            if (token.getHolder().getOwningKey().equals(getOurIdentity().getOwningKey())){
                                isFiatToken = true;
                                // lets make sure the amount is correct
                                if (token.getAmount().getQuantity() != trade.getSize()) throw new FlowException(String.format("Token amount sent does not match trade amount. Can't sign"));
                                break;
                            }
                        }
                    }
                    // we couldn't find an output with the correct token
                    if (!isFiatToken) throw new FlowException("Unable to find a token output that matches the trade. Can't sign.");

                    // lets make sure the tx is already signed by caller.
                    if (!stx.getSigs().get(0).getBy().equals(callerSession.getCounterparty().getOwningKey())) throw new FlowException("Transaction is not signed by caller. Won't sign");
                    if (stx.getSigs().size() > 1) throw new FlowException("Too many signatures included in the transaction. Won't sign.");

                    // lets make sure caller is the buyer
                    if (!callerSession.getCounterparty().equals(trade.getBuyer())) throw new FlowException("Caller is not the buyer. Won't sign.");
                }
            });

            subFlow(new ReceiveFinalityFlow(callerSession));
            return null;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class CancelBuyer extends FlowLogic<SignedTransaction>{
        private UniqueIdentifier tradeId;

        public CancelBuyer(UniqueIdentifier tradeId) {
            this.tradeId = tradeId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // caller must be a valid buyer
            if (!subFlow(new MembershipFlows.isBuyer())) throw new FlowException("Only a valid buyer organization can cancel a trade.");

            // trade must exists
            TradeState trade = null;
            StateAndRef<TradeState> input = null;
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(tradeId)){
                    input = stateAndRef;
                    trade = stateAndRef.getState().getData();
                    break;
                }
            }

            if (trade == null || input == null) throw new FlowException(String.format("Trade with id %s does not exists. Can't cancel", tradeId.toString()));

            // trade must be in Propose state
            if (!trade.getState().equals(State.PROPOSED)) throw new FlowException(String.format("Trade is not in Proposed state. Current state is %s. Can't cancel", trade.getState().toString()));

            // we must be the buyer of the trade
            Party caller = getOurIdentity();
            if (!trade.getBuyer().equals(caller)) throw new FlowException("Caller is not the buyer of the trade. Can't cancel");

            // we are ready to cancel. we will generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // we require signatures from both buyer and seller
            List<PublicKey> requiredSigners = Arrays.asList(caller.getOwningKey(),trade.getSeller().getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.Cancelled(), requiredSigners);

            trade.setState(State.CANCELLED);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(input)
                .addOutputState(trade, TradeContract.ID)
                .addCommand(command);

            // we collect signature from the buyer
            SignedTransaction partiallysignedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession sellerSession = initiateFlow(trade.getSeller());
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallysignedTx, Arrays.asList(sellerSession)));

            subFlow(new FinalityFlow(signedTransaction,Arrays.asList(sellerSession)));

            return signedTransaction;
        }
    }

    @InitiatedBy(CancelBuyer.class)
    public static class CancelBuyerResponse extends FlowLogic<Void>{
        private FlowSession caller;

        public CancelBuyerResponse(FlowSession caller) {
            this.caller = caller;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new SignTransactionFlow(this.caller) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // we get the trade being cancelled
                    TradeState trade = (TradeState) stx.getCoreTransaction().getOutput(0);

                    // we make sure cancellation comes from the buyer of the trade
                    if (!caller.getCounterparty().equals(trade.getBuyer())) throw new FlowException(String.format("Cancellation of trade is not from the correct buyer. %s", caller.getCounterparty().toString()));

                    // lets make sure we are the seller of this trade
                    Party seller = getOurIdentity();
                    if (!trade.getSeller().equals(seller)) throw new FlowException("We are not the seller of this trade. Can't accept cancel request.");
                }
            });
            subFlow(new ReceiveFinalityFlow(caller));
            return null;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class AcceptBuyer extends FlowLogic<SignedTransaction>{
        private UniqueIdentifier tradeId;

        public AcceptBuyer(UniqueIdentifier tradeId) {
            this.tradeId = tradeId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // must be a buyer
            if (!subFlow(new MembershipFlows.isBuyer())) throw new FlowException("Must be a valid buyer organization to accept a trade.");

            // trade must exists
            TradeState trade = null;
            StateAndRef<TradeState> input = null;
            VaultService vaultService = getServiceHub().getVaultService();
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<TradeState> stateAndRef : vaultService.queryBy(TradeState.class,criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(tradeId)){
                    input = stateAndRef;
                    trade = stateAndRef.getState().getData();
                }
            }
            if (trade == null || input == null) throw new FlowException(String.format("Provided trade id %s doesn not exists. Can't accept", tradeId.toString()));

            // we must be the buyer of this trade
            Party buyer = getOurIdentity();
            if(!trade.getBuyer().equals(buyer)) throw new FlowException("We are not the buyer of this trade, can't accept.");

            // seller must be a valid seller organization
            if (!subFlow(new MembershipFlows.isSeller(trade.getSeller()))) throw new FlowException("Seller on trade is not an active seller organization. Can't accept.");

            // trade must be in Proposed state
            if (!trade.getState().equals(State.PROPOSED)) throw new FlowException(String.format("Trade is not in proposed state. Current state is %s. Can't accept.", trade.getState().toString()));

            // current offer must be big enought to include this trade
            StateAndRef<OfferState> offerStateStateAndRef = null;
            OfferState offer = null;
            for (StateAndRef<OfferState> stateAndRef : vaultService.queryBy(OfferState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getOfferId().equals(trade.getOffer().getOfferId())){
                    offerStateStateAndRef = stateAndRef;
                    offer = stateAndRef.getState().getData();
                }
            }
            if (offer == null || offerStateStateAndRef == null) throw new FlowException("Provided offer in trade is not valid.");
            if (offer.getAfsSize() < trade.getSize()) throw new FlowException(String.format("Not enought Available for Sale bonds in the offer %s to accept the trace", offer.getOfferId().toString()));


            // we are ready to accept the trade
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(trade.getBuyer().getOwningKey(), trade.getSeller().getOwningKey());

            Command tradeCommand = new Command<>(new TradeContract.Commands.Pending(), requiredSigners);
            Command offerCommand = new Command<>(new OfferContract.Commands.notifyBuyers(), trade.getSeller().getOwningKey());
            trade.setState(State.PENDING);

            long currentAfsSize = offer.getAfsSize();
            offer.setAfsSize(currentAfsSize-trade.getSize());

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(input)
                .addInputState(offerStateStateAndRef)
                .addOutputState(trade, TradeContract.ID)
                .addOutputState(offer, OfferContract.ID)
                .addCommand(offerCommand)
                .addCommand(tradeCommand);

            // lets collect signatures
            FlowSession sellerSession = initiateFlow(trade.getSeller());
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partiallySignedTx, ImmutableList.of(sellerSession)));
            subFlow(new FinalityFlow(fullySignedTx,sellerSession));

            // now we will pay if we have the money
            TokenType fiat = FiatCurrency.Companion.getInstance(trade.getCurrency().getCurrencyCode());
            Amount balance = QueryUtilitiesKt.tokenBalance(vaultService,fiat);
            if (balance.getQuantity() >= trade.getSize()){
                subFlow(new Settle(tradeId));
            }

            return fullySignedTx;
        }
    }

    @InitiatedBy(AcceptBuyer.class)
    public static class AcceptBuyerResponder extends FlowLogic<Void>{
        private FlowSession caller;

        public AcceptBuyerResponder(FlowSession caller) {
            this.caller = caller;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new SignTransactionFlow(this.caller) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    OfferState offer = (OfferState) stx.getCoreTransaction().outRef(1).getState().getData();
                    TradeState trade = (TradeState) stx.getCoreTransaction().outRef(0).getState().getData();

                    // we must be issuers of the offer
                    Party issuer = getOurIdentity();
                    if (!offer.getIssuer().equals(issuer)) throw new FlowException(String.format("Provided offer %s was not issued by us. We won't sign.", offer.getOfferId().toString()));

                    // we validate we are the seller
                    if (!trade.getSeller().equals(issuer)) throw new FlowException(String.format("We are not the sellers of  trade %s. We won't sign.", trade.getId().toString()));
                }
            });

            SignedTransaction fullySignedTx = subFlow(new ReceiveFinalityFlow(caller));
            // now we will notify all buyers about the new offer change
            OfferState offer = (OfferState) fullySignedTx.getCoreTransaction().outRef(1).getState().getData();
            if (offer.isAfs()) {
                subFlow(new OfferFlow.NotifyBuyers(fullySignedTx.getCoreTransaction().outRef(1), offer));
            }
            return null;
        }
    }

}
