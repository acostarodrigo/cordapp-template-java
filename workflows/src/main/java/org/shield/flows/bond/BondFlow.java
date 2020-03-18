package org.shield.flows.bond;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.businessnetworks.membership.flows.member.PartyAndMembershipMetadata;
import com.r3.businessnetworks.membership.states.MembershipState;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.shield.bond.BondState;
import org.shield.bond.BondTypeContract;
import org.shield.flows.custodian.CustodianFlows;
import org.shield.flows.membership.MembershipFlows;
import org.shield.flows.offer.OfferFlow;
import org.shield.membership.ShieldMetadata;
import org.shield.offer.OfferState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BondFlow {

    // instantiation now allowed
    private BondFlow(){
    }


    @StartableByRPC
    @InitiatingFlow
    public static class Issue extends FlowLogic<String> {
        private BondState bond;

        public Issue(BondState bond) {
            this.bond = bond;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            // we validate caller is an issuer
            if (!subFlow(new MembershipFlows.isIssuer())) throw new FlowException("Only active issuer organizations can issue a bond.");

            // we force the issuer to the caller
            Party issuer = getOurIdentity();
            bond.setIssuer(issuer);

            // validate that the bond with this is doesn't exists.
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<BondState> stateAndRef : getServiceHub().getVaultService().queryBy(BondState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(bond.getId())) throw new FlowException(String.format("Specified id %s already exists. Can't issue bond.", bond.getId()));
            }

            // we create the bond type
            SignedTransaction signedTransaction = subFlow(new IssueType(this.bond));

            // now we issue the tokens
            TokenPointer tokenPointer = bond.toPointer(bond.getClass());
            IssuedTokenType issuedTokenType = new IssuedTokenType(bond.getIssuer(), tokenPointer);
            Amount<IssuedTokenType> amount = new Amount<>(bond.getDealSize(), BigDecimal.ONE, issuedTokenType);
            FungibleToken fungibleToken = new FungibleToken(amount,bond.getIssuer(), null);

            // bond is issued
            subFlow(new IssueTokens(Arrays.asList(fungibleToken)));

            // we create the offer.
            OfferState offer = new OfferState(new UniqueIdentifier(),issuer,bond,bond.getIssuerTicker(),100,100, bond.getDealSize(),false, new Date());
            subFlow(new OfferFlow.Create(offer));

            // we inform the custodian
            subFlow(new CustodianFlows.SendBond(bond.getId()));

            return bond.getId();
        }
    }


    /**
     * sets the Available For Sale state of the bonds.
     */
    @StartableByRPC
    @InitiatingFlow
    public static class setAFS extends FlowLogic<SignedTransaction>{
        private String bondId;
        private boolean afs;

        public setAFS(String bondId, boolean afs) {
            this.bondId = bondId;
            this.afs = afs;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // we must have the bond in our vault
            StateAndRef<BondState> input = null;
            BondState bond = null;
            for (StateAndRef<BondState> stateAndRef : getServiceHub().getVaultService().queryBy(BondState.class).getStates()){
                if (stateAndRef.getState().getData().getId().equals(bondId)){
                    input = stateAndRef;
                    bond = stateAndRef.getState().getData();
                    break;
                }
            }

            if (input == null || bond == null) throw new FlowException(String.format("Specified bond %s doesn't exists.", bondId.toString()));

            // we must have token balance of this bond too
            TokenPointer tokenPointer = bond.toPointer(bond.getClass());
            Amount balance = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(), tokenPointer);

            if (balance.getQuantity() == 0 && afs == true) throw new FlowException(String.format("You don't have balance to set bond %s Available for Sale.", bondId.toString()));


            // we will add all buyers of the business network as observers
            // With this we are allowing any buyer to see issued bonds.
            List<PartyAndMembershipMetadata> partyAndMembershipMetadataList = subFlow(new MembershipFlows.GetAllMemberships());
            List<Party> observers = getBuyers(partyAndMembershipMetadataList);
            // we will remove ourselves from the list.
            if (observers.contains(bond.getIssuer())) observers.remove(bond.getIssuer());
            SignedTransaction updatedObservers = subFlow(new UpdateEvolvableToken(input,bond,observers));
            subFlow(new UpdateDistributionListFlow(updatedObservers));
            return updatedObservers;
        }
    }

    /**
     * gets all the buyers from the business network
     * @return
     */
    private static List<Party> getBuyers(List<PartyAndMembershipMetadata> list){
        List<Party> buyers = new ArrayList<>();
        for (PartyAndMembershipMetadata partyAndMembershipMetadata : list){
            ShieldMetadata metadata = (ShieldMetadata) partyAndMembershipMetadata.getMembershipMetadata();
            if (metadata.getOrgTypes().contains(ShieldMetadata.OrgType.BOND_PARTICIPANT)){
                if (metadata.getBondRoles().contains(ShieldMetadata.BondRole.BUYER)){
                    buyers.add(partyAndMembershipMetadata.getParty());
                }
            }
        }
        return buyers;
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
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<BondState> stateAndRefs : getServiceHub().getVaultService().queryBy(BondState.class, criteria).getStates()){
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
