package org.shield;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.flows.treasurer.USDFiatTokenFlow;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.shield.TestHelper.*;

public class USDFiatTokenTests {
    @Before
    public void setUp(){
        TestHelper.setupNetwork();
    }

    @Test
    public void issueUSDTokenTest() throws ExecutionException, InterruptedException {
        MembershipTests membershipTests = new MembershipTests();
        membershipTests.configTreasurerTest();

        CordaFuture<SignedTransaction> cordaFuture = broker2Node.startFlow(new USDFiatTokenFlow.Issue(broker1, 1000));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // we re issue more tokens
        cordaFuture = broker2Node.startFlow(new USDFiatTokenFlow.Issue(broker1, 1000));
        mockNet.runNetwork();
        signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // lets get usd token balance of broker1
        TokenType usd = FiatCurrency.Companion.getInstance("USD");
        Amount tokenBalance = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), usd);

        // balance check
        assertEquals(tokenBalance.getQuantity(), 2000);
    }

    @Test (expected = ExecutionException.class)
    public void issueUSDTokenWithoutBeingTreasurerTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(new USDFiatTokenFlow.Issue(broker1, 1000));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(null); // this should never be triggered

    }

    @After
    public void cleanUp(){
        TestHelper.cleanUpNetwork();
    }
}
