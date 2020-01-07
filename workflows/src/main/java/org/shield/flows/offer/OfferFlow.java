package org.shield.flows.offer;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.transactions.SignedTransaction;
import org.shield.offer.OfferState;

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
    public static class create extends FlowLogic<UniqueIdentifier>{
        private OfferState offer;

        public create(OfferState offer) {
            this.offer = offer;
        }

        @Override
        @Suspendable
        public UniqueIdentifier call() throws FlowException {
            //must be issuer or buyer to create an offer.

            // must have enought balance to create the offer

            // will validate data of the offer

            // if afs is set to true, we will notify all
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
    private static class notifyBuyers extends FlowLogic<SignedTransaction>{
        private StateAndRef<OfferState> input;

        public notifyBuyers(StateAndRef<OfferState> input) {
            this.input = input;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return null;
        }
    }
}
