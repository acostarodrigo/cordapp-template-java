package org.shield;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.bond.BondState;
import org.shield.bond.DealType;
import org.shield.flows.bond.BondFlow;

import java.util.Currency;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.shield.TestHelper.*;

public class BondFlowTests {
    private BondState bond;

    @Before
    public void setUp() {
        // we set up the network
        TestHelper.setupNetwork();

        // we create the bond
        bond = new BondState(new UniqueIdentifier(), issuer, "Rodrigo", Currency.getInstance("USD"), new Date(), 1,1,1, DealType.REG_S, 1,10000,99,new Date(),99.8,1, new UniqueIdentifier());

    }


    /**
     * Creates a bond .
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void issueBondTest() throws ExecutionException, InterruptedException {
        // we add the issuer as an Issuer
        MembershipTests membershipTests = new MembershipTests();
        membershipTests.configIssuerTest();



        // we issue the bond
        CordaFuture<UniqueIdentifier> issueFuture = issuerNode.startFlow(new BondFlow.Issue(bond));

        mockNet.runNetwork();
        UniqueIdentifier id = issueFuture.get();
        assertNotNull(id);

        // we will get it from the vault
        BondState storedBond = null;
        for (StateAndRef<BondState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(BondState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)) storedBond = stateAndRef.getState().getData();
        }
        assertNotNull(storedBond);
        assertEquals(storedBond, bond);

        // issuer must have the bond token 100% assigned to himself.
        FungibleToken token = issuerNode.getServices().getVaultService().queryBy(FungibleToken.class).getStates().get(0).getState().getData();
        assertEquals(token.getAmount().getQuantity(), bond.getDealSize());

    }

    @After
    public void cleanUp() {
        mockNet.stopNodes();
    }
}


