package org.shield.flows.bond;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.shield.bond.BondContract;
import org.shield.bond.BondState;
import org.shield.bond.BondTypeContract;
import org.shield.flows.membership.MembershipFlows;

import java.math.BigDecimal;
import java.util.Arrays;

public class BondFlow {

    // instantiation now allowed
    private BondFlow(){
    }


    @StartableByRPC
    @InitiatingFlow
    public static class Issue extends FlowLogic<UniqueIdentifier> {
        private BondState bond;

        public Issue(BondState bond) {
            this.bond = bond;
        }

        @Override
        @Suspendable
        public UniqueIdentifier call() throws FlowException {
            // we validate caller is an issuer
            if (!subFlow(new MembershipFlows.isIssuer())) throw new FlowException("Only active issuer organizations can issue a bond.");

            // we force the issuer to the caller
            Party issuer = getOurIdentity();
            bond.setIssuer(issuer);

            // we make sure bond is verified before we move on
            // todo validate contract

            // we create the bond type
            subFlow(new IssueType(this.bond));

            // now we issue the tokens
            TokenPointer tokenPointer = bond.toPointer(bond.getClass());
            IssuedTokenType issuedTokenType = new IssuedTokenType(bond.getIssuer(), tokenPointer);
            Amount<IssuedTokenType> amount = new Amount<>(bond.getDealSize(), BigDecimal.ONE, issuedTokenType);
            FungibleToken fungibleToken = new FungibleToken(amount,bond.getIssuer(), null);

            subFlow(new IssueTokens(Arrays.asList(fungibleToken)));

            return bond.getId();
        }
    }

    /**
     * Creates CommercialPaper token type into blockchain.
     */
    @InitiatingFlow
    private static class IssueType extends FlowLogic<SignedTransaction> {
        private BondState bond;

        public IssueType(BondState bond) {
            this.bond = bond;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party notary = getServiceHub(). getNetworkMapCache(). getNotaryIdentities(). get(0);
            TransactionState transactionState = new TransactionState(this.bond, BondTypeContract.ID, notary);
            return (SignedTransaction) subFlow(new CreateEvolvableTokens(transactionState));
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class updateBond extends FlowLogic<SignedTransaction> {
        private BondState bond;

        public updateBond(BondState bond) {
            this.bond = bond;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // we validate caller is an issuer
            if (!subFlow(new MembershipFlows.isIssuer())) throw new FlowException("Only active issuer organizations can update a bond.");

            // we make sure we have the bond in our vault
            StateAndRef<BondState> stateAndRef = null;
            for (StateAndRef<BondState> stateAndRefs : getServiceHub().getVaultService().queryBy(BondState.class).getStates()){
                if (stateAndRefs.getState().getData().getLinearId() == bond.getLinearId()) stateAndRef = stateAndRefs;
            }

            if (stateAndRef==null) throw new FlowException(String.format("Bond with ID %s was not found.", bond.getLinearId().toString()));

            SignedTransaction transaction = subFlow(new UpdateEvolvableToken(stateAndRef, bond));
            return transaction;
        }
    }

    @InitiatingFlow
    public static class Sell extends FlowLogic<SignedTransaction> {
        private BondState bond;
        private long amount;
        private Party buyer;

        public Sell(BondState bond, long amount, Party buyer) {
            this.bond = bond;
            this.amount = amount;
            this.buyer = buyer;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // we validate caller is an issuer
            if (!subFlow(new MembershipFlows.isIssuer())) throw new FlowException("Only active issuer organizations can sell a bond.");

            // buyer must be a valid buyer organization
            if (!subFlow(new MembershipFlows.isBuyer(buyer))) throw new FlowException(String.format("%s is not an active Buyer organization.", buyer.toString()));

            // caller must be bond issuer
            Party issuer = getOurIdentity();
            if (!bond.getIssuer().equals(issuer)) throw new FlowException(String.format("%s is not the issuer of provided bond.", issuer.toString()));

            // we, as issuer, must have the token in our vault
            // we will validate transaction includes correct payment
            // todo validate in contract, transaction should include payment and signatures.

            // we are ready to send bond token
            TokenPointer tokenPointer = bond.toPointer(bond.getClass());
            Amount bondAmount = new Amount(amount,tokenPointer);
            PartyAndAmount partyAndAmount = new PartyAndAmount(buyer,bondAmount);
            SignedTransaction signedTransaction = subFlow(new MoveFungibleTokens(partyAndAmount));

            return signedTransaction;
        }
    }



}
