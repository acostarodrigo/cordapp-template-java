package org.shield;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.junit.Before;
import org.junit.Test;
import org.shield.bond.BondState;
import org.shield.flows.offer.OfferFlow;
import org.shield.offer.OfferState;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OfferFlowTests {
    private OfferState offer;
    private BondState bond;
    @Before
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



        offer = new OfferState(new UniqueIdentifier(),TestHelper.issuer,bond,"ISSUER",99,99,1000,1000,true, new Date());
    }

    public OfferState getOffer() {
        return offer;
    }

    @Test
    public void createOfferTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> cordaFuture = TestHelper.issuerNode.startFlow(new OfferFlow.Create(offer));
        TestHelper.mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // issuer, broker1 and broker2 should have the offer.
        assertEquals(TestHelper.issuerNode.getServices().getVaultService().queryBy(OfferState.class).getStates().get(0).getState().getData(),offer);
        assertEquals(TestHelper.broker1Node.getServices().getVaultService().queryBy(OfferState.class).getStates().get(0).getState().getData(),offer);
        assertEquals(TestHelper.broker2Node.getServices().getVaultService().queryBy(OfferState.class).getStates().get(0).getState().getData(),offer);


    }
}
