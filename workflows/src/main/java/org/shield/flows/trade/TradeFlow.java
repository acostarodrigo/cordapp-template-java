package org.shield.flows.trade;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken;
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
import net.corda.core.node.services.VaultService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.flows.membership.MembershipFlows;
import org.shield.trade.State;
import org.shield.trade.TradeContract;
import org.shield.trade.TradeState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

public class TradeFlow {
    // we are disabling instantiation
    private TradeFlow() {}

    @StartableByRPC
    @InitiatingFlow
    public static class SendToBuyer extends FlowLogic<SignedTransaction>{
        private TradeState trade;

        /**
         * Constructor
          * @param trade the trade we are sending.
         */
        public SendToBuyer(TradeState trade) {
            this.trade = trade;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // we validate the caller is an issuer
            if (!subFlow(new MembershipFlows.isIssuer())) throw new FlowException("Only an active issuer can send a trade to a buyer");

            // we validate the buyer of the trade is a buyer organization
            if (!subFlow(new MembershipFlows.isBuyer(trade.getBuyer()))) throw new FlowException(String.format("%s is not an active buyer organization", trade.getBuyer().getName().getCommonName()));

            // we validate the bond to be traded is already issued.
            BondState bond = null;
            StateAndRef<BondState> bondStateStateAndRef = null;
            for (StateAndRef<BondState> stateAndRef : getServiceHub().getVaultService().queryBy(BondState.class).getStates()){
                if (stateAndRef.getState().getData().equals(trade.getBond())){
                    bondStateStateAndRef = stateAndRef;
                    bond = stateAndRef.getState().getData();
                    break;
                }
            }
            if (bondStateStateAndRef == null || bond == null) throw new FlowException(String.format("Specified bond %s does not exists.", this.trade.getBond().getId().toString()));

            // caller must be issuer of the bond
            Party caller = getOurIdentity();
            if (!caller.equals(bond.getIssuer())) throw new FlowException(String.format("Seller (%s) must be issuer (%s) of the bond.", caller.getName().getCommonName(), bond.getIssuer().getName().getCommonName() ));

            // we validate issuer has enought balance
            TokenPointer tokenPointer = bond.toPointer(bond.getClass());
            Amount currentBalance = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(), tokenPointer);
            if (currentBalance.getQuantity()<trade.getSize()) throw new FlowException(String.format("Issuer doesn't have enought balance for this trade. Current balance is %s",String.valueOf(currentBalance.getQuantity())));

            // we are good to go. let's generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(trade.getSeller().getOwningKey(), trade.getBuyer().getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.sendToBuyer(), requiredSigners);


            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addCommand(command)
                .addOutputState(trade, TradeContract.ID);

            // we sign it
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // we add the buyer as an observer, and send it to him.
            subFlow(new UpdateEvolvableToken(bondStateStateAndRef, bond,ImmutableList.of(trade.getBuyer())));
            subFlow(new UpdateDistributionListFlow(partiallySignedTx));

            // collect signature from the buyer. he will validate trade and bond are accurate.
            FlowSession buyerSession = initiateFlow(trade.getBuyer());
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTx, Arrays.asList(buyerSession)));

            // we are done.
            subFlow(new FinalityFlow(signedTransaction,buyerSession));

            return signedTransaction;
        }
    }

    @InitiatedBy(SendToBuyer.class)
    public static class SendToBuyerResponse extends FlowLogic<Void> {
        private FlowSession callerSession;

        public SendToBuyerResponse(FlowSession callerSession) {
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
                    // we validate trade is send from an Issuer organization
                    if (!subFlow(new MembershipFlows.isIssuer(trade.getSeller()))) throw new FlowException(String.format("Trade seller (%s) is not an Issuer organization", trade.getSeller().getName().getCommonName()));


                    // we must have the bond as an observer
                    BondState storedBond = null;
                    for (StateAndRef<BondState> stateAndRef : getServiceHub().getVaultService().queryBy(BondState.class).getStates()){
                        if (stateAndRef.getState().getData().equals(trade.getBond())) {
                            storedBond = stateAndRef.getState().getData();
                            break;
                        }

                    }
                    if (storedBond == null) throw new FlowException(String.format("Bond %s is not locally stored. We are not observers.", trade.getBond().getId().toString()));
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

        public Cancel(UniqueIdentifier tradeId) {
            this.tradeId = tradeId;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // check permissions.
            if (!subFlow(new MembershipFlows.isBuyer())) throw new FlowException("Only an active Buyer organization can cancell a trade.");

            // get the state
            StateAndRef<TradeState> tradeStateStateAndRef = null;
            TradeState trade = null;

            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class).getStates()){
                if (stateAndRef.getState().getData().getId().equals(this.tradeId)){
                    tradeStateStateAndRef = stateAndRef;
                    trade = stateAndRef.getState().getData();
                    break;
                }
            }
            // lets make sure the trade we are cancelling exists.
            if (tradeStateStateAndRef == null || trade == null) throw new FlowException(String.format("Specified trade %s does not exists.", this.tradeId.toString()));


            // we are ready to cancel it.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(trade.getBuyer().getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.cancel(), requiredSigners);

            trade.setState(State.CANCELLED);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(tradeStateStateAndRef)
                .addOutputState(trade, TradeContract.ID)
                .addCommand(command);

            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            FlowSession sellerSession = initiateFlow(trade.getSeller());
            subFlow(new FinalityFlow(signedTx,sellerSession));

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
            subFlow(new ReceiveFinalityFlow(callerSession));
            return null;
        }
    }

    @InitiatingFlow
    public static class Accept extends FlowLogic<SignedTransaction> {
        private UniqueIdentifier tradeId;

        public Accept(UniqueIdentifier tradeId) {
            this.tradeId = tradeId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // lets make sure is a buyer.
            if (!subFlow(new MembershipFlows.isBuyer())) throw new FlowException("Must be a valid Buyer organization to accept trade.");

            // we must have the trade already in order to accept it.
            StateAndRef<TradeState> tradeStateStateAndRef = null;
            TradeState trade = null;
            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class).getStates()){
                if (stateAndRef.getState().getData().getId().equals(this.tradeId)){
                    tradeStateStateAndRef = stateAndRef;
                    trade = stateAndRef.getState().getData();
                    break;
                }
            }
            // lets make sure the trade to accept exists.
            if (tradeStateStateAndRef == null || trade == null) throw new FlowException(String.format("Specified trade %s does not exists.", this.tradeId.toString()));

            // we are ready to accept it.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(trade.getBuyer().getOwningKey(), trade.getSeller().getOwningKey());

            // for now, we are only accepting
            Command command = new Command<>(new TradeContract.Commands.accept(), requiredSigners);
            trade.setState(State.ACCEPTED_NOTPAYED);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(tradeStateStateAndRef)
                .addCommand(command);

            // lets validate if we have enought fiat token to pay
            TokenType fiat = FiatCurrency.Companion.getInstance(trade.getCurrency().getCurrencyCode());
            Amount tokenBalance = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(), fiat);
            boolean hasBalance = false;
            if (tokenBalance.getQuantity()>= trade.getSize()){
                hasBalance = true;
            }

            FlowSession sellerSession = initiateFlow(trade.getSeller());

            if (hasBalance) {
                txBuilder = subFlow(new GenerateSettleTx(trade,txBuilder));
                trade.setState(State.ACCEPTED_PAYED);
            } else{
                trade.setState(State.ACCEPTED_NOTPAYED);
            }

            // we add the trade with the current status
            txBuilder.addOutputState(trade, TradeContract.ID);
            // we sign the transaction
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);
            // we get the signature of the seller to validate bond outputs.
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partiallySignedTx, ImmutableList.of(sellerSession)));

            //end
            subFlow(new FinalityFlow(fullySignedTx,sellerSession));

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
            // trade is just being accepted, we will not execute fiat token validations
            subFlow(new SignTransactionFlow(callingSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // Custom Logic to validate transaction.
                }
            });

            subFlow(new ReceiveFinalityFlow(callingSession));
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
            for (StateAndRef<TradeState> stateAndRef : vaultService.queryBy(TradeState.class).getStates()){
                if (stateAndRef.getState().getData().getId().equals(trade.getId())) {
                    input = stateAndRef;
                    break;
                }
            }

            if (input==null) throw new FlowException(String.format("Specified trade %s does not exists.", trade.getId().toString()));

            // trade must be in accepted status
            if (!trade.getState().equals(State.ACCEPTED_NOTPAYED)) throw new FlowException(String.format("Trade is not in Accepted status. Current status is %s", trade.getState()));

            // lets validate we have the balance to pay
            TokenType fiatToken = FiatCurrency.Companion.getInstance(trade.getCurrency().getCurrencyCode());
            if (QueryUtilitiesKt.tokenBalance(vaultService,fiatToken).getQuantity() < trade.getSize()) throw new FlowException("Not enought fiat balance to settle trade.");

            List<PublicKey> requiredSigners = ImmutableList.of(trade.getBuyer().getOwningKey(), trade.getSeller().getOwningKey());
            // and we add the settle command to perform validations on the contract.
            Command settle = new Command<>(new TradeContract.Commands.settle(), requiredSigners);
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
            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class).getStates()){
                if (stateAndRef.getState().getData().getId().equals(tradeId)){
                    input = stateAndRef;
                    trade = input.getState().getData();
                    break;
                }
            }

            if (input == null || trade == null)  throw new FlowException(String.format("Trade with id %s was not found on this node.", tradeId.toString()));

            // trade must be accepted and not payed yet.
            if (trade.getState() != State.ACCEPTED_NOTPAYED) throw new FlowException(String.format("Trade must be in Accepted_NotPayed state to settle. current state is %s", trade.getState().toString()));

            // we are good to go. let's generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(trade.getSeller().getOwningKey(), trade.getBuyer().getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.settle(), requiredSigners);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(input)
                .addCommand(command);

            // we generate the trade transaction
            txBuilder = subFlow(new GenerateSettleTx(trade, txBuilder));

            // and add the trade as output.
            trade.setState(State.ACCEPTED_PAYED);
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
