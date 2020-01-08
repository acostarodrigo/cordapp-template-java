package org.shield.flows.offer;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.businessnetworks.membership.flows.member.PartyAndMembershipMetadata;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.*;
import net.corda.core.node.StatesToRecord;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.VaultService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.Nullable;
import org.shield.bond.BondState;
import org.shield.flows.membership.MembershipFlows;
import org.shield.membership.ShieldMetadata;
import org.shield.offer.OfferContract;
import org.shield.offer.OfferState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Offer flows are created by a bond holder to offer them to buyers.
 */
public class OfferFlow {

    // we don't allow instantiation
    private OfferFlow(){

    }

    /**
     * Creates a new offer.
     */
    @InitiatingFlow
    @StartableByRPC
    public static class Create extends FlowLogic<UniqueIdentifier>{
        private OfferState offer;

        public Create(OfferState offer) {
            this.offer = offer;
        }

        // progress tracker steps
        private static Step VALIDATE_BALANCE = new Step("Validating bond balance for offer.");
        private static Step VALIDATE_OFFER = new Step("Validating provided offer.");
        private static Step TX_BUILDING = new Step("Creating transaction.");
        private static Step SUBMIT_OFFER = new Step("Submitting transaction to create offer.");
        private static final Step NOTIFY_BUYERS = new Step("Validating bond balance for offer."){
            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return NotifyBuyers.progressTracker;
            }
        };
        private final static ProgressTracker progressTracker = new ProgressTracker(VALIDATE_BALANCE, VALIDATE_OFFER, TX_BUILDING, SUBMIT_OFFER, NOTIFY_BUYERS);

        @Override
        @Suspendable
        public UniqueIdentifier call() throws FlowException {
            //must be buyer to create an offer.
            if (!subFlow(new MembershipFlows.isBuyer())) throw new FlowException("Must be an active buyer organization to create an offer.");

            // must have enought balance to create the offer
            progressTracker.setCurrentStep(VALIDATE_BALANCE);
            VaultService vaultService = getServiceHub().getVaultService();
            BondState bond = this.offer.getBond();
            TokenPointer tokenPointer = bond.toPointer(bond.getClass());
            Amount balance = QueryUtilitiesKt.tokenBalance(vaultService,tokenPointer);
            if (balance.getQuantity() < offer.getAfsSize()) throw new FlowException(String.format("Not enought balance to submit offer. Current balance is %s", String.valueOf(balance.getQuantity())));

            // will validate data of the offer
            progressTracker.setCurrentStep(VALIDATE_OFFER);
            if (offer.getOfferId() == null) offer.setOfferId(new UniqueIdentifier());
            Party caller = getOurIdentity();
            if (!offer.getIssuer().equals(caller)) throw new FlowException("Can issue an offer of behalf of someone else.");


            // if afs is set to true, we will notify all
            progressTracker.setCurrentStep(TX_BUILDING);
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            Command command = new Command<>(new OfferContract.Commands.create(), caller.getOwningKey());
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(offer, OfferContract.ID)
                .addCommand(command);

            // we are ready to sign
            progressTracker.setCurrentStep(SUBMIT_OFFER);
            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
            subFlow(new FinalityFlow(signedTx));

            if (offer.isAfs()) {
                progressTracker.setCurrentStep(NOTIFY_BUYERS);
                subFlow(new NotifyBuyers(signedTx.getCoreTransaction().outRef(0),offer));
            }

            return offer.getOfferId();
        }
    }

    @InitiatedBy(Create.class)
    public static class CreateResponder extends FlowLogic<Void>{
        private FlowSession callerSession;

        public CreateResponder(FlowSession callerSession) {
            this.callerSession = callerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new ReceiveFinalityFlow(callerSession));
            return null;
        }
    }

    /**
     * Sets the Available For Sale property, and notify buyers if needed.
     */
    @InitiatingFlow
    @StartableByRPC
    public static class setAFS extends FlowLogic<SignedTransaction> {
        private UniqueIdentifier id;
        private boolean afs;

        public setAFS(UniqueIdentifier id, boolean afs) {
            this.id = id;
            this.afs = afs;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return null;
        }
    }

    /**
     * Modifies an existing Offer. Notifies buyers if needed.
     */
    @StartableByRPC
    @InitiatingFlow
    public static class modify extends FlowLogic<SignedTransaction> {
        private OfferState offer;

        public modify(OfferState offer) {
            this.offer = offer;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return null;
        }
    }

    /**
     * Notifies buyers of changes on the offer
     */
    @InitiatingFlow
    public static class NotifyBuyers extends FlowLogic<SignedTransaction>{
        private StateAndRef<OfferState> input;
        private OfferState output;

        // constructor
        public NotifyBuyers(StateAndRef<OfferState> input, OfferState output) {
            this.input = input;
            this.output = output;
        }

        // Progress tracker
        private static Step GET_BUYERS = new Step("Getting buyers from business network.");
        private static Step INIT_SESSIONS = new Step("Initializing flow sessions with all buyers.");
        private static Step TX_BUILDING = new Step("Creating and signing transaction.");
        private static final Step FINALISATION = new Step("Finalising Offer.NotifyBuyers flow.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };
        public static final ProgressTracker progressTracker = new ProgressTracker(GET_BUYERS,INIT_SESSIONS,TX_BUILDING,FINALISATION);


        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party caller = getOurIdentity();
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // we get the buyers from the network
            progressTracker.setCurrentStep(GET_BUYERS);
            List<PartyAndMembershipMetadata> partyAndMembershipMetadataList = subFlow(new MembershipFlows.GetAllMemberships());
            List<Party> observers = getBuyers(partyAndMembershipMetadataList);
            // we will remove ourselves from the list.
            if (observers.contains(caller)) observers.remove(caller);

            // we will initiate sessions with all buyers
            progressTracker.setCurrentStep(INIT_SESSIONS);
            Collection<FlowSession> flowSessions = new ArrayList<>();
            for (Party observer : observers){
                flowSessions.add(initiateFlow(observer));
            }

            // and create the transaction
            progressTracker.setCurrentStep(TX_BUILDING);
            Command command = new Command<>(new OfferContract.Commands.notifyBuyers(), caller.getOwningKey());
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(input)
                .addOutputState(output, OfferContract.ID)
                .addCommand(command);

            // we are ready to sign
            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            // all done
            progressTracker.setCurrentStep(FINALISATION);
            subFlow(new FinalityFlow(signedTx,flowSessions));
            return signedTx;
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
    }

    @InitiatedBy(OfferFlow.NotifyBuyers.class)
    public static class NotifyBuyersResponse extends FlowLogic<Void>{
        private FlowSession callerSession;

        public NotifyBuyersResponse(FlowSession callerSession) {
            this.callerSession = callerSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            // we are setting StatesToRecord to all visible to save the offer
            subFlow(new ReceiveFinalityFlow(callerSession,null, StatesToRecord.ALL_VISIBLE));
            return null;
        }
    }


}
