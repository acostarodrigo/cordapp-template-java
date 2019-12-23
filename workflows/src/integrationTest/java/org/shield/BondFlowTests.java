package org.shield;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.bond.BondState;
import org.shield.bond.DealType;
import org.shield.flows.bond.BondFlow;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.shield.TestHelper.*;

public class BondFlowTests {
    private BondState bond;

    public BondState getBond() {
        return bond;
    }

    @Before
    public void setUp() {
        // we set up the network
        TestHelper.setupNetwork();

        // we create the bond
        bond = new BondState(new UniqueIdentifier(), issuer, "Rodrigo", Currency.getInstance("USD"), new Date(), 0,500000,1, DealType.REG_S, 100,10000000,99,new Date(),99.8,0, new UniqueIdentifier());

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

        TokenPointer tokenPointer = bond.toPointer(bond.getClass());
        Amount balance = QueryUtilitiesKt.tokenBalance(issuerNode.getServices().getVaultService(),tokenPointer);
        assertEquals(balance.getQuantity(), bond.getDealSize());
    }

    @Test(expected = ExecutionException.class)
    public void issueBondWithoutBeingMemberTest() throws ExecutionException, InterruptedException {
        // we issue the bond
        CordaFuture<UniqueIdentifier> issueFuture = issuerNode.startFlow(new BondFlow.Issue(bond));

        mockNet.runNetwork();
        UniqueIdentifier id = issueFuture.get();
        // this should never be reached
        assertNotNull(null);
    }

    @Test(expected = ExecutionException.class)
    public void issueBondWithoutBeingIssuerTest() throws ExecutionException, InterruptedException {
        MembershipTests membershipTests = new MembershipTests();
        membershipTests.configBuyerTest();
        // we issue the bond
        CordaFuture<UniqueIdentifier> issueFuture = broker1Node.startFlow(new BondFlow.Issue(bond));

        mockNet.runNetwork();
        UniqueIdentifier id = issueFuture.get();
        // this should never be reached
        assertNotNull(null);
    }

    @After
    public void cleanUp() {
        mockNet.stopNodes();
    }

    @Test
    public void sellBondTest() throws ExecutionException, InterruptedException {
        // we issue the bond
        issueBondTest();

        // we are making broker1 a buyer
        MembershipTests membershipTests = new MembershipTests();
        membershipTests.configBuyerTest();

        TokenPointer tokenPointer = bond.toPointer(bond.getClass());

        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(new BondFlow.Sell(bond,100,broker1));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);
    }
}


