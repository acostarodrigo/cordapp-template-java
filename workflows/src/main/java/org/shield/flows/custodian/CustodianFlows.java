package org.shield.flows.custodian;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ReferencedStateAndRef;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.custodian.CustodianContract;
import org.shield.custodian.CustodianState;
import org.shield.flows.membership.MembershipFlows;
import org.shield.membership.ShieldMetadata;
import org.shield.trade.TradeState;

import java.security.PublicKey;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CustodianFlows {
    private CustodianFlows(){
        // no instantiation
    }

    /**
     * returns both input (StateAndRef<CustodianState>) and output (CustodianState) to be used on a transaction.
     */
    private static class GetCustodianState extends FlowLogic<List<Object>>{
        @Override
        @Suspendable
        public List<Object> call() throws FlowException {
            Party caller = getOurIdentity();

            // we get all custodians for the caller
            List<Party> custodians = subFlow(new GetCustodians());

            // todo what happens if there are no custodians?

            // we get custodian state data
            CustodianState custodianState = null;
            StateAndRef<CustodianState> input = null;
            QueryCriteria.VaultQueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            try {
                input = getServiceHub().getVaultService().queryBy(CustodianState.class, queryCriteria).getStates().get(0);
                custodianState = input.getState().getData();
            } catch (Exception e){
                // we don't have a custodian state to use, must be the first one.
                custodianState = new CustodianState(caller, custodians);
            }
            // we will update the list of custodians, in case a new one was added.
            custodianState.setCustodians(custodians);

            return Arrays.asList(input, custodianState);
        }
    }

    @InitiatingFlow
    public static class SendBond extends FlowLogic<SignedTransaction>{
        private String bondId;

        /**
         * we are sending an specific bond.
         * @param bondId
         */
        public SendBond(String bondId) {
            this.bondId = bondId;
        }


        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party caller = getOurIdentity();

            // we get the bond from the vault
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            BondState bond = null;
            StateAndRef<BondState> bondStateStateAndRef = null;
            for (StateAndRef<BondState> stateAndRef : getServiceHub().getVaultService().queryBy(BondState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(bondId)){
                    bondStateStateAndRef = stateAndRef;
                    bond = stateAndRef.getState().getData();
                    break;
                }
            }

            if (bond == null || bondStateStateAndRef == null) throw new FlowException(String.format("Bond with id %s does not exists.", bondId));

            // lets generate the custodian data
            List<Object> result = subFlow(new GetCustodianState());
            StateAndRef<CustodianState> input = (StateAndRef<CustodianState>) result.get(0);
            CustodianState custodianState = (CustodianState) result.get(1);

            // we will add the bond to the Custodian state
            List<BondState> bonds = custodianState.getBonds();
            if (bonds == null)
                custodianState.setBonds(Arrays.asList(bond));
            else {
                ArrayList bondList = new ArrayList(bonds);
                bondList.add(bond);
                custodianState.setBonds(bondList);
            }

            custodianState.setLastUpdated(new Timestamp(System.currentTimeMillis()));

            // we get custodians sessions and public keys
            List<PublicKey> signers = new ArrayList<>();
            List<FlowSession> sessions = new ArrayList<>();
            for (Party party : custodianState.getCustodians()){
                signers.add(party.getOwningKey());
                FlowSession flowSession = initiateFlow(party);
                sessions.add(flowSession);
            }
            signers.add(caller.getOwningKey());
            Command command = new Command<>(new CustodianContract.Commands.notifyBond(), signers);

            // we will generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(custodianState,CustodianContract.ID)
                .addReferenceState(new ReferencedStateAndRef<>(bondStateStateAndRef))
                .addCommand(command);

            // if we have an input, we will include it
            if (input != null) txBuilder.addInputState(input);

            // we are ready to sign
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // we get custodians signature
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partiallySignedTx,sessions));

            // all done
            subFlow(new FinalityFlow(fullySignedTx, sessions));
            return fullySignedTx;
        }
    }

    @InitiatedBy(SendBond.class)
    public static class SendBondResponder extends FlowLogic<Void>{
        private FlowSession callerSession;

        public SendBondResponder(FlowSession callerSession) {
            this.callerSession = callerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new SignTransactionFlow(callerSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // for now we are just signing with no validations.
                    System.out.println("Validating transaction from " + callerSession.getCounterparty().getName().toString() + " ok. " + stx.getCoreTransaction().toString());
                }
            });

            subFlow(new ReceiveFinalityFlow(callerSession));
            return null;
        }
    }

    @InitiatingFlow
    public static class SendTrade extends FlowLogic<SignedTransaction>{
        private UniqueIdentifier tradeId;

        /**
         * we are sending an specific trade.
         * @param tradeId
         */
        public SendTrade(UniqueIdentifier tradeId) {
            this.tradeId = tradeId;
        }


        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party caller = getOurIdentity();

            // we get the trade from the vault
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            TradeState trade = null;
            StateAndRef<TradeState> tradeStateStateAndRef = null;
            for (StateAndRef<TradeState> stateAndRef : getServiceHub().getVaultService().queryBy(TradeState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(tradeId)){
                    tradeStateStateAndRef = stateAndRef;
                    trade = stateAndRef.getState().getData();
                    break;
                }
            }

            if (trade == null || tradeStateStateAndRef == null) throw new FlowException(String.format("Trade with id %s does not exists.", tradeId));

            // lets generate the custodian data
            List<Object> result = subFlow(new GetCustodianState());
            StateAndRef<CustodianState> input = (StateAndRef<CustodianState>) result.get(0);
            CustodianState custodianState = (CustodianState) result.get(1);

            // we will add the trade to the Custodian state
            List<TradeState> trades = custodianState.getTrades();
            if (trades == null)
                custodianState.setTrades(Arrays.asList(trade));
            else {
                ArrayList tradeList = new ArrayList(trades);
                tradeList.add(trade);
                custodianState.setTrades(tradeList);
            }

            custodianState.setLastUpdated(new Timestamp(System.currentTimeMillis()));

            // we get custodians sessions and public keys
            List<PublicKey> signers = new ArrayList<>();
            List<FlowSession> sessions = new ArrayList<>();
            for (Party party : custodianState.getCustodians()){
                signers.add(party.getOwningKey());
                FlowSession flowSession = initiateFlow(party);
                sessions.add(flowSession);
            }
            signers.add(caller.getOwningKey());
            Command command = new Command<>(new CustodianContract.Commands.notifyTrade(), signers);

            // we will generate the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(custodianState,CustodianContract.ID)
                .addReferenceState(new ReferencedStateAndRef<>(tradeStateStateAndRef))
                .addCommand(command);

            // if we have an input, we will include it
            if (input != null) txBuilder.addInputState(input);

            // we are ready to sign
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // we get custodians signature
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partiallySignedTx,sessions));

            // all done
            subFlow(new FinalityFlow(fullySignedTx, sessions));
            return fullySignedTx;
        }
    }

    @InitiatedBy(SendTrade.class)
    public static class SendTradeResponder extends FlowLogic<Void>{
        private FlowSession callerSession;

        public SendTradeResponder(FlowSession callerSession) {
            this.callerSession = callerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new SignTransactionFlow(callerSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // for now we are just signing with no validations.
                }
            });

            subFlow(new ReceiveFinalityFlow(callerSession));
            return null;
        }
    }


    /**
     * gets the list of custodians for the caller
     */
    private static class GetCustodians extends FlowLogic<List<Party>>{
        @Override
        @Suspendable
        public List<Party> call() throws FlowException {
            // we get the custodians sessions of the caller.
            ShieldMetadata metadata =  (ShieldMetadata) subFlow(new MembershipFlows.getMembership()).getMembershipMetadata();
            List<Party> custodians = new ArrayList<>();
            for (Party custodian :  metadata.getCustodians()){
                // we are only adding the custodian after we verify it is a valid network member
                if (subFlow(new MembershipFlows.isCustodian(custodian)))  custodians.add(custodian);
            }

            return custodians;
        }
    }
}
