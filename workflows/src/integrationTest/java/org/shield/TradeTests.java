package org.shield;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.bond.BondState;
import org.shield.bond.DealType;
import org.shield.flows.trade.TradeFlow;
import org.shield.trade.State;
import org.shield.trade.TradeState;

import java.util.Currency;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.shield.TestHelper.*;

public class TradeTests {
    private BondState bond;
    private BondFlowTests bondFlowTests = new BondFlowTests();
    private TradeState trade;

    @Before
    public void setNetwork() throws ExecutionException, InterruptedException {
        // we set up the network
        bondFlowTests.setUp();
        // and instantiate a new bond
        bond = bondFlowTests.getBond();

        // we issue the bond
        bondFlowTests.issueBondTest();
    }

    @Test
    public void generateTradeTest() throws ExecutionException, InterruptedException {
        // we make broker1 a buyer
        MembershipTests membershipTests = new MembershipTests();
        membershipTests.configBuyerTest();

        // Issuer sends trade
        trade = new TradeState(new UniqueIdentifier(), bond,new Date(), new Date(), broker1, issuer, 100, 1,1233, 123, Currency.getInstance("USD"), State.SENT);
        CordaFuture<SignedTransaction> signedTransactionCordaFuture = issuerNode.startFlow(new TradeFlow.SendToBuyer(trade));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = signedTransactionCordaFuture.get();
        assertNotNull(signedTransaction);
    }

    @Test
    public void acceptTradeWithBalance() throws ExecutionException, InterruptedException {
        generateTradeTest();
        if (trade == null) throw new InterruptedException("Trade not generated");

        // we send the buyer some fiat tokens US 2000
        USDFiatTokenTests usdFiatTokenTests = new USDFiatTokenTests();
        usdFiatTokenTests.issueUSDTokenTest();

        // buyer accepts trade and since it has enought balance, operation is completed.
        CordaFuture<SignedTransaction> cordaFuture = broker1Node.startFlow(new TradeFlow.Accept(trade.getId()));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

    }

    @After
    public void cleanUp(){
        TestHelper.cleanUpNetwork();
    }
}
