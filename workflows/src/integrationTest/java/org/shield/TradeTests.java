package org.shield;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.bond.BondState;
import org.shield.bond.DealType;
import org.shield.flows.trade.TradeFlow;
import org.shield.offer.OfferState;
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
    private OfferState offer;

    @Before
    public void setNetwork() throws ExecutionException, InterruptedException {
        OfferFlowTests offerFlowTests = new OfferFlowTests();
        offerFlowTests.setUp();
        offerFlowTests.createOfferTest();
        offer = offerFlowTests.getOffer();
        bond = offer.getBond();
    }

    @Test
    public void generateTradeTest() throws ExecutionException, InterruptedException {
        // Issuer sends trade
        trade = new TradeState(new UniqueIdentifier(), offer,bond,new Date(), new Date(), broker1, issuer, 100, 1,100, 123, Currency.getInstance("USD"), State.PROPOSED);
        CordaFuture<UniqueIdentifier> signedTransactionCordaFuture = broker1Node.startFlow(new TradeFlow.Create(trade));
        mockNet.runNetwork();
        UniqueIdentifier id = signedTransactionCordaFuture.get();
        assertNotNull(id);
    }

    @Test
    public void acceptTradeWithBalance() throws ExecutionException, InterruptedException {
        generateTradeTest();
        if (trade == null) throw new InterruptedException("Trade not generated");

        // we send the buyer some fiat tokens US 2000000
        USDFiatTokenTests usdFiatTokenTests = new USDFiatTokenTests();
        usdFiatTokenTests.issueUSDTokenTest();

        // lets get current balances of bond and fiat
        BondState bond = trade.getBond();
        TokenPointer tokenPointer = bond.toPointer(bond.getClass());
        TokenType usd = FiatCurrency.Companion.getInstance("USD");
        // issuer bond balance should be the entire amount tof tokens of the bond
        Amount currentBondBalanceIssuer = QueryUtilitiesKt.tokenBalance(issuerNode.getServices().getVaultService(), tokenPointer);
        assertEquals(currentBondBalanceIssuer.getQuantity(), bond.getDealSize());

        // issuer fiat balance should be zero
        Amount currentFiatBalanceIssuer = QueryUtilitiesKt.tokenBalance(issuerNode.getServices().getVaultService(), usd);
        assertEquals(currentFiatBalanceIssuer.getQuantity(), 0);

        // buyer bond balance should be zero
        Amount currentBondBalanceBuyer = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), tokenPointer);
        assertEquals(currentBondBalanceBuyer.getQuantity(),0);
        // buyer fiat balance should be enought to pay the trade
        Amount currentFiatBalanceBuyer = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), usd);
        assertTrue(currentFiatBalanceBuyer.getQuantity()>=bond.getDealSize());


        // buyer accepts trade and since it has enought balance, operation is completed.
        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(new TradeFlow.Accept(trade.getId()));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // lets validate balances in each node wallets are correct.
        // issuer bond balance is reduced
        assertEquals(currentBondBalanceIssuer.getQuantity(),QueryUtilitiesKt.tokenBalance(issuerNode.getServices().getVaultService(), tokenPointer).getQuantity()+trade.getSize());
        // while token balance is increased
        assertEquals(QueryUtilitiesKt.tokenBalance(issuerNode.getServices().getVaultService(), usd).getQuantity(), trade.getSize());

        // buyer bond balance is increased
        assertEquals(QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), tokenPointer).getQuantity(), trade.getSize());
        // while fiat balance is reduced.
        assertEquals(QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), usd).getQuantity(),currentFiatBalanceBuyer.getQuantity()-trade.getSize());

        // Trade is in state payed in both nodes
        TradeState buyerState = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertTrue(buyerState.getState().equals(State.SETTLED));
        TradeState issuerState = issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertEquals(buyerState.getState(), issuerState.getState());
    }

    @Test
    public void AcceptTradeWithoutFiat() throws ExecutionException, InterruptedException {
        generateTradeTest();
        if (trade == null) throw new InterruptedException("Trade not generated");

        // lets get current balances of bond and fiat
        BondState bond = trade.getBond();
        TokenPointer tokenPointer = bond.toPointer(bond.getClass());
        TokenType usd = FiatCurrency.Companion.getInstance("USD");
        // issuer bond balance should be the entire amount tof tokens of the bond
        Amount currentBondBalanceIssuer = QueryUtilitiesKt.tokenBalance(issuerNode.getServices().getVaultService(), tokenPointer);
        assertEquals(currentBondBalanceIssuer.getQuantity(), bond.getDealSize());

        // issuer fiat balance should be zero
        Amount currentFiatBalanceIssuer = QueryUtilitiesKt.tokenBalance(issuerNode.getServices().getVaultService(), usd);
        assertEquals(currentFiatBalanceIssuer.getQuantity(), 0);

        // buyer bond balance should be zero
        Amount currentBondBalanceBuyer = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), tokenPointer);
        assertEquals(currentBondBalanceBuyer.getQuantity(),0);
        // buyer fiat balance should be enought to pay the trade
        Amount currentFiatBalanceBuyer = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), usd);
        assertEquals(currentFiatBalanceBuyer.getQuantity(), 0);


        // buyer accepts trade and since it has enought balance, operation is completed.
        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(new TradeFlow.Accept(trade.getId()));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // lets validate balances in each node wallets are correct.
        // issuer bond balance is reduced
        assertEquals(currentBondBalanceIssuer.getQuantity(),QueryUtilitiesKt.tokenBalance(issuerNode.getServices().getVaultService(), tokenPointer).getQuantity());
        // while token balance is increased
        assertEquals(QueryUtilitiesKt.tokenBalance(issuerNode.getServices().getVaultService(), usd).getQuantity(), 0);

        // buyer bond balance is increased
        assertEquals(QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), tokenPointer).getQuantity(),0);
        // while fiat balance is reduced.
        assertEquals(QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), usd).getQuantity(),currentFiatBalanceBuyer.getQuantity());

        // Trade is in state payed in both nodes
        TradeState buyerState = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertTrue(buyerState.getState().equals(State.PENDING));
        TradeState issuerState = issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertEquals(buyerState.getState(), issuerState.getState());
    }

    @Test
    public void settleTradeTest() throws ExecutionException, InterruptedException {
        // we generate and accept the trade.
        AcceptTradeWithoutFiat();

        // we send the buyer some fiat tokens US 2000000
        USDFiatTokenTests usdFiatTokenTests = new USDFiatTokenTests();
        usdFiatTokenTests.issueUSDTokenTest();

        trade = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();

        // we validate the new state
        assertEquals(trade.getState(), State.PENDING);

        // and settle the trade.
        CordaFuture<SignedTransaction> cordaFuture = broker1Node.startFlow(new TradeFlow.Settle(trade.getId()));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        trade = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertEquals(State.SETTLED, trade.getState());

        // we validate bond balance at buyer
        TokenPointer tokenPointer = bond.toPointer(bond.getClass());
        Amount currentBondBalanceBuyer = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), tokenPointer);
        assertEquals(currentBondBalanceBuyer.getQuantity(),trade.getSize());
    }

    @Test
    public void cancelTradeTest() throws ExecutionException, InterruptedException {
        generateTradeTest();
        CordaFuture<Void> cordaFuture = issuerNode.startFlow(new TradeFlow.Cancel(trade.getId()));
        mockNet.runNetwork();
        cordaFuture.get();

        // trade must be cancelled in both nodes.
        assertEquals(State.CANCELLED, issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData().getState());
        assertEquals(State.CANCELLED, broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData().getState());

    }

    @Test (expected = ExecutionException.class)
    public void cancelAndAcceptedTrade() throws ExecutionException, InterruptedException {
        acceptTradeWithBalance();
        CordaFuture<Void> cordaFuture = issuerNode.startFlow(new TradeFlow.Cancel(trade.getId()));
        mockNet.runNetwork();
        cordaFuture.get();
        assertEquals(State.SETTLED, broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData().getState());
        assertEquals(State.SETTLED, issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData().getState());
    }
    @After
    public void cleanUp(){
        TestHelper.cleanUpNetwork();
    }
}
