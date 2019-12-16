package org.shield;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.flows.bond.BondFlow;
import org.shield.bond.BondState;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.shield.TestHelper.*;

public class CommercialPaperTokenTests {
    @Before
    public void nodesSetUp() {
        TestHelper.setupNetwork();
    }

    @Test
    /**
     * issues a fungible token from issuer to broker 1 and validates token exists in both parties.
     * Only broker1 has fungible tokens.
     * Then issues tokens to broker2 and validated token gets updated in all participants and broker 2 has fungible tokens.
     */
    public void testIssueFungibleToken() throws ExecutionException, InterruptedException {
        Date offeringDate = new Date(2020,12,12);
        CordaFuture<UniqueIdentifier> issueTokensBroker1 = issuerNode.startFlow(new BondFlow.IssueFungibleToken(offeringDate, 1L, broker1));

        mockNet.runNetwork();
        UniqueIdentifier id = issueTokensBroker1.get();
        assertNotNull(id);

        BondState tokenType = issuerNode.getServices().getVaultService().queryBy(BondState.class).component1().get(0).getState().getData();
        assertNotNull(tokenType);
        assertEquals(tokenType.getValuation(), 1L);
        // issuer only has one tokenType
        assertTrue(issuerNode.getServices().getVaultService().queryBy(BondState.class).component1().size() == 1);

        BondState tokenTypeBroker1 = broker1Node.getServices().getVaultService().queryBy(BondState.class).component1().get(0).getState().getData();
        assertNotNull(tokenTypeBroker1);
        assertEquals(tokenTypeBroker1.getValuation(), tokenType.getValuation());


        CordaFuture<UniqueIdentifier> issueTokensBroker2 = issuerNode.startFlow(new BondFlow.IssueFungibleToken(offeringDate, 10L, broker2));
        mockNet.runNetwork();
        issueTokensBroker2.get();

        BondState tokenTypeBroker2 = broker2Node.getServices().getVaultService().queryBy(BondState.class).component1().get(0).getState().getData();
        assertNotNull(tokenTypeBroker2);
        assertEquals(tokenTypeBroker2.getValuation(), 11);

        // now we compare the tokens
        FungibleToken fungibleTokenBroker1 = broker1Node.getServices().getVaultService().queryBy(FungibleToken.class).getStates().get(0).getState().getData();
        assertNotNull(fungibleTokenBroker1);

        FungibleToken fungibleTokenBroker2 = broker2Node.getServices().getVaultService().queryBy(FungibleToken.class).getStates().get(0).getState().getData();
        assertNotNull(fungibleTokenBroker2);

        assertEquals(tokenTypeBroker2.getValuation(), fungibleTokenBroker1.getAmount().getQuantity() + fungibleTokenBroker2.getAmount().getQuantity());
    }

    @Test (expected = java.util.concurrent.ExecutionException.class)
    public void issueTokenWithInvalidDate() throws ExecutionException, InterruptedException {
        Date offeringDate = new Date();
        CordaFuture<UniqueIdentifier> issueTokensBroker1 = issuerNode.startFlow(new BondFlow.IssueFungibleToken(offeringDate, 1L, broker1));
        // should fail, offering date not in the future.

        mockNet.runNetwork();
        UniqueIdentifier id = issueTokensBroker1.get();
    }

    @After
    public void cleanUp() {
       TestHelper.cleanUpNetwork();
    }
}
