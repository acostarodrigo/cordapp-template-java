package org.shield.flows.trade;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondContract;
import org.shield.bond.BondState;
import org.shield.trade.State;
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

    @StartableByRPC
    @InitiatingFlow
    public static class SendToBuyer extends FlowLogic<SignedTransaction>{
        private TradeState trade;

        public SendToBuyer(TradeState trade) {
            this.trade = trade;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // we validate the caller is an issuer
            if (!subFlow(new MembershipFlows.isIssuer())) throw new FlowException("Only an active issuer can send a trade to a buyer");

            // we validate the buyer of the trade is a buyer organization
            if (!subFlow(new MembershipFlows.isBuyer(trade.getBuyer()))) throw new FlowException(String.format("Buyer %s is not registered is not an active buyer organization", trade.getBuyer().getName().getCommonName()));

            // we validate the bond to be traded is already issued.
            BondState bond = null;
            StateAndRef<BondState> bondStateStateAndRef = null;
            for (StateAndRef<BondState> stateAndRef : getServiceHub().getVaultService().queryBy(BondState.class).getStates()){
                if (stateAndRef.getState().getData().getLinearId().equals(this.trade.getBondId())){
                    bondStateStateAndRef = stateAndRef;
                    bond = stateAndRef.component1().getData();
                    break;
                }
            }
            if (bondStateStateAndRef == null || bond == null) throw new FlowException(String.format("Specified bond %s does not exists.", this.trade.getBondId().toString()));

            // caller must be issuer of the bond
            Party caller = getOurIdentity();
            if (!caller.equals(bond.getIssuer())) throw new FlowException(String.format("Seller (%s) must be issuer (%s) of the bond.", caller.getName().getCommonName(), bond.getIssuer().getName().getCommonName() ));

            // todo verify contract before sending

            // we are good to go. let's generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<PublicKey> requiredSigners = Arrays.asList(trade.getSeller().getOwningKey(), trade.getBuyer().getOwningKey());
            Command command = new Command<>(new TradeContract.Commands.sendToBuyer(), requiredSigners);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(trade, TradeContract.ID)
                .addOutputState(bond, BondContract.ID)
                .addCommand(command);

            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            FlowSession buyerSession = initiateFlow(trade.getBuyer());
            subFlow(new SendTransactionFlow(buyerSession, signedTx));
            subFlow(new FinalityFlow(signedTx,buyerSession));

            return signedTx;
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
            // we don't need to check if caller is a buyer. If we have the passed trade, then we can cancel it.
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
        public Void call() throws FlowException {
            subFlow(new ReceiveFinalityFlow(callerSession));
            return null;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class Accept extends FlowLogic<SignedTransaction> {
        private UniqueIdentifier tradeId;

        public Accept(UniqueIdentifier tradeId) {
            this.tradeId = tradeId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // we don't need to check if caller is a buyer. If we have the passed trade, then we can accept it.
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
            Command command = new Command<>(new TradeContract.Commands.accept(), requiredSigners);

            trade.setState(State.ACCEPTED);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(tradeStateStateAndRef)
                .addOutputState(trade, TradeContract.ID)
                .addCommand(command);

            // if buyer has enought Fiat to pay, we are including them as output and input
            ///getServiceHub().getVaultService().queryBy(FungibleToken)


            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            FlowSession sellerSession = initiateFlow(trade.getSeller());
            subFlow(new SendTransactionFlow(sellerSession, signedTx));
            subFlow(new FinalityFlow(signedTx,sellerSession));

            return signedTx;
        }
    }

}
