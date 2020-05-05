package org.shield;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Test;
import org.shield.bond.BondState;
import org.shield.flows.offer.OfferFlow;
import org.shield.offer.OfferState;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.shield.TestHelper.issuerNode;
import static org.shield.TestHelper.mockNet;

public class OfferFlowTests {
    private OfferState offer;
    private BondState bond;

    public void setUp() throws ExecutionException, InterruptedException {
        // we set up the network and issue the bond
        BondFlowTests bondFlowTests = new BondFlowTests();
        bondFlowTests.setUp();
        bondFlowTests.issueBondTest();

        bond = bondFlowTests.getBond();
        assertNotNull(bond);

        MembershipTests membershipTests = new MembershipTests();
        // we configure broker1 as buyer
        membershipTests.configBuyerTest();
        // we configure broker2 as buyer
        membershipTests.configTreasurerTest();
    }

    public OfferState getOffer() throws ExecutionException, InterruptedException {
        return TestHelper.issuerNode.getServices().getVaultService().queryBy(OfferState.class).getStates().get(0).getState().getData();
    }

    @Test
    public void createOfferTest() throws ExecutionException, InterruptedException {
        setUp();
        // offer was created when issuing.
        this.offer = TestHelper.issuerNode.getServices().getVaultService().queryBy(OfferState.class).getStates().get(0).getState().getData();
        assertNotNull(offer);
    }

    @Test
    public void setOfferAFSTest() throws ExecutionException, InterruptedException {
        setUp();
        CordaFuture<SignedTransaction> future = TestHelper.issuerNode.startFlow(new OfferFlow.setAFS(getOffer().getOfferId(), true));
        mockNet.runNetwork();
        future.get();

        OfferState offerState = getOffer();
        assertTrue(offerState.isAfs());
    }

    /**
     * This test should fail because we are updating the offer to a value which we have no balance.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test (expected = ExecutionException.class)
    public void modifyOfferWithIncorrectValueTest() throws ExecutionException, InterruptedException {
        setUp();
        OfferState offerState = getOffer();
        BondState bond = offerState.getBond();
        offerState.setAfsSize(bond.getDealSize() + 1000);
        CordaFuture<SignedTransaction> cordaFuture =  issuerNode.startFlow(new OfferFlow.Modify(offerState));
        mockNet.runNetwork();
        cordaFuture.get();
    }



    @Test
    public void updateOfferTest() throws ExecutionException, InterruptedException {
        setUp();
        OfferState offerState = getOffer();
        long currentAfsSize = offerState.getAfsSize();
        BondState bond = offerState.getBond();
        offerState.setAfsSize(bond.getDealSize() - 1000);
        offerState.setOfferYield(99);
        CordaFuture<SignedTransaction> cordaFuture =  issuerNode.startFlow(new OfferFlow.Modify(offerState));
        mockNet.runNetwork();
        cordaFuture.get();

        OfferState broker1Offer = TestHelper.broker1Node.getServices().getVaultService().queryBy(OfferState.class).getStates().get(0).getState().getData();
        OfferState broker2Offer = TestHelper.broker2Node.getServices().getVaultService().queryBy(OfferState.class).getStates().get(0).getState().getData();

        offerState = getOffer();
        assertTrue(currentAfsSize == offerState.getAfsSize() +1000);
        assertTrue(offerState.getOfferYield() == 99);

        assertEquals(broker1Offer, broker2Offer);
    }

    @After
    public void stopNetwork(){
        TestHelper.cleanUpNetwork();
    }
}
