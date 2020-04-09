package org.shield;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.node.Corda;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.bond.BondState;
import org.shield.bond.BondType;
import org.shield.bond.DealType;
import org.shield.fiat.FiatState;
import org.shield.flows.bond.BondFlow;
import org.shield.flows.offer.OfferFlow;
import org.shield.flows.trade.OfflineTrade;
import org.shield.flows.trade.TradeFlow;
import org.shield.flows.treasurer.USDFiatTokenFlow;
import org.shield.offer.OfferState;
import org.shield.trade.State;
import org.shield.trade.TradeState;

import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.shield.TestHelper.*;

public class TradeTests {
    private BondState bond;
    private TradeState trade;
    private OfferState offer;

    @Before
    public void setNetwork() throws ExecutionException, InterruptedException {
        OfferFlowTests offerFlowTests = new OfferFlowTests();
        offerFlowTests.setOfferAFSTest();
        offer = offerFlowTests.getOffer();
        bond = offer.getBond();
    }

    @Test
    public void generateTradeTest() throws ExecutionException, InterruptedException {
        // Issuer sends trade
        trade = new TradeState(new UniqueIdentifier(), offer, new Date(), new Date(),broker1, broker1, issuer, "arranger",100, 1,100, 123, Currency.getInstance("USD"), State.PROPOSED);
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
        BondState bond = trade.getOffer().getBond();
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
        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(new TradeFlow.AcceptSeller(trade.getId()));
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

        // buyer should have a new offer
        OfferState buyerOffer = broker1Node.getServices().getVaultService().queryBy(OfferState.class).getStates().get(1).getState().getData();
        assertNotNull(buyerOffer);
        assertTrue(buyerOffer.getBond().equals(bond));
        assertTrue(buyerOffer.getAfsSize() == trade.getSize());


        // Fiat States validatin
        FiatState issuerFiatState = issuerNode.getServices().getVaultService().queryBy(FiatState.class).getStates().get(0).getState().getData();
        assertNotNull(issuerFiatState);
        FiatState broker1FiatState = broker1Node.getServices().getVaultService().queryBy(FiatState.class).getStates().get(0).getState().getData();
        assertNotNull(issuerFiatState);
    }

    @Test
    public void AcceptTradeWithoutFiat() throws ExecutionException, InterruptedException {
        generateTradeTest();
        if (trade == null) throw new InterruptedException("Trade not generated");

        // lets get current balances of bond and fiat
        BondState bond = trade.getOffer().getBond();
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

        // Trade is in state pending in both nodes
        TradeState buyerState = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertTrue(buyerState.getState().equals(State.PROPOSED));
        TradeState issuerState = issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertEquals(buyerState.getState(), issuerState.getState());

        // buyer accepts trade
        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(new TradeFlow.AcceptSeller(trade.getId()));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // Trade is in state pending in both nodes
        buyerState = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertTrue(buyerState.getState().equals(State.PENDING));
        issuerState = issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertEquals(buyerState.getState(), issuerState.getState());

        assertTrue(broker2Node.getServices().getVaultService().queryBy(TradeState.class).getStates().size() == 0);
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
        CordaFuture<Void> cordaFuture = issuerNode.startFlow(new TradeFlow.CancelSeller(trade.getId()));
        mockNet.runNetwork();
        cordaFuture.get();

        // trade must be cancelled in both nodes.
        assertEquals(State.CANCELLED, issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData().getState());
        assertEquals(State.CANCELLED, broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData().getState());

    }

    @Test (expected = ExecutionException.class)
    public void cancelAndAcceptedTrade() throws ExecutionException, InterruptedException {
        acceptTradeWithBalance();
        CordaFuture<Void> cordaFuture = issuerNode.startFlow(new TradeFlow.CancelSeller(trade.getId()));
        mockNet.runNetwork();
        cordaFuture.get();
        assertEquals(State.SETTLED, broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData().getState());
        assertEquals(State.SETTLED, issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData().getState());
    }

    @Test
    public void generateOffLineTrade() throws ExecutionException, InterruptedException {
        CordaFuture<UniqueIdentifier> cordaFuture = issuerNode.startFlow(new OfflineTrade.IssuerCreate(bond.getId(),broker1,1000, 99,99,new Date(),1000,"Rodrigo"));
        mockNet.runNetwork();
        UniqueIdentifier tradeId = cordaFuture.get();
        assertNotNull(tradeId);

        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        TradeState issuerTrade = issuerNode.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(issuerTrade);
        assertTrue(issuerTrade.getSize() == 1000);

        TradeState brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);
        assertTrue(brokerTrade.getSize() == 1000);
        assertTrue(issuerTrade.equals(brokerTrade));

        OfferState issuerOffer = issuerNode.getServices().getVaultService().queryBy(OfferState.class,criteria).getStates().get(0).getState().getData();
        assertNotNull(issuerOffer);
        assertTrue(issuerOffer.isAfs());

        assertTrue(issuerTrade.getState().equals(State.PROPOSED));
        assertTrue(brokerTrade.getState().equals(State.PROPOSED));
    }

    @Test
    public void generaterOfflineTradeWithNewOfferTest() throws ExecutionException, InterruptedException {
        // we issue a new bond
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        // manipulate date
        c.add(Calendar.YEAR, 1);
        Date startDate = c.getTime();
        BondState newBond = new BondState("ro", "rod",Currency.getInstance("USD"),startDate,1,1,1,DealType.REG_S,1,10000,99.1d,startDate,99.4d,0, BondType.VANILA);
        CordaFuture<String> cordaFuture = issuerNode.startFlow(new BondFlow.Issue(newBond));
        mockNet.runNetwork();
        String bondId = cordaFuture.get();
        assertNotNull(bondId);

        // we issue the offline trade
        CordaFuture<UniqueIdentifier> tradeCordaFuture = issuerNode.startFlow(new OfflineTrade.IssuerCreate(newBond.getId(),broker1,1000, 99,99,new Date(),1000,"Rodrigo"));
        mockNet.runNetwork();
        UniqueIdentifier tradeId = tradeCordaFuture.get();
        assertNotNull(tradeId);

        tradeCordaFuture = issuerNode.startFlow(new OfflineTrade.IssuerCreate(newBond.getId(),broker2,1000, 99,99,new Date(),1000,"Rodrigo"));
        mockNet.runNetwork();
        tradeId = tradeCordaFuture.get();
        assertNotNull(tradeId);

        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        TradeState issuerTrade = issuerNode.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(issuerTrade);
        assertTrue(issuerTrade.getSize() == 1000);

        TradeState brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);
        assertTrue(brokerTrade.getSize() == 1000);
        assertTrue(issuerTrade.equals(brokerTrade));

        brokerTrade = broker2Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);
        assertTrue(brokerTrade.getSize() == 1000);

        OfferState issuerOffer = issuerNode.getServices().getVaultService().queryBy(OfferState.class,criteria).getStates().get(1).getState().getData();
        assertNotNull(issuerOffer);
        assertTrue(!issuerOffer.isAfs());

        assertTrue(issuerTrade.getState().equals(State.PROPOSED));
        assertTrue(brokerTrade.getState().equals(State.PROPOSED));
    }

    @Test
    public void cancellOfflineTradeTest() throws ExecutionException, InterruptedException {
        // we generate the trade
        generaterOfflineTradeWithNewOfferTest();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        TradeState brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);

        // we cancell the trade from the buyer side
        CordaFuture<SignedTransaction> cordaFuture = broker1Node.startFlow(new TradeFlow.CancelBuyer(brokerTrade.getId()));
        mockNet.runNetwork();
        cordaFuture.get();

        // both trades must be cancelled
        brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);
        assertTrue(brokerTrade.getState().equals(State.CANCELLED));

        TradeState issuerTrade = issuerNode.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(1).getState().getData();
        assertTrue(issuerTrade.getState().equals(State.CANCELLED));
    }

    @Test
    public void acceptOfflineTradeTest() throws ExecutionException, InterruptedException {
        generaterOfflineTradeWithNewOfferTest();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        TradeState brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);

        OfferState offer = issuerNode.getServices().getVaultService().queryBy(OfferState.class, criteria).getStates().get(1).getState().getData();
        assertTrue(offer.getAfsSize() == 10000);

        // we accept the trade from the buyer side
        CordaFuture<SignedTransaction> cordaFuture = broker1Node.startFlow(new TradeFlow.AcceptBuyer(brokerTrade.getId()));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // both trades must be cancelled
        brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);
        assertTrue(brokerTrade.getState().equals(State.PENDING));

        TradeState issuerTrade = issuerNode.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(1).getState().getData();
        assertTrue(issuerTrade.getState().equals(State.PENDING));

        offer = issuerNode.getServices().getVaultService().queryBy(OfferState.class, criteria).getStates().get(1).getState().getData();
        assertTrue(offer.getAfsSize() == 9000);


        // we do the same with trader2
        brokerTrade = broker2Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);

        offer = issuerNode.getServices().getVaultService().queryBy(OfferState.class, criteria).getStates().get(1).getState().getData();
        assertTrue(offer.getAfsSize() == 9000);

        // we accept the trade from the buyer side
        cordaFuture = broker2Node.startFlow(new TradeFlow.AcceptBuyer(brokerTrade.getId()));
        mockNet.runNetwork();
        signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // both trades must be accepted
        brokerTrade = broker2Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);
        assertTrue(brokerTrade.getState().equals(State.PENDING));

        issuerTrade = issuerNode.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertTrue(issuerTrade.getState().equals(State.PENDING));

        offer = issuerNode.getServices().getVaultService().queryBy(OfferState.class, criteria).getStates().get(1).getState().getData();
        assertTrue(offer.getAfsSize() == 8000);

        assertEquals(1,broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().size());
        assertEquals(1,broker2Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().size());
    }

    @Test
    public void settleOfflineTradeTest() throws ExecutionException, InterruptedException {
        acceptOfflineTradeTest();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        CordaFuture cordaFuture = issuerNode.startFlow(new USDFiatTokenFlow.Issue(broker1, 1000000));
        mockNet.runNetwork();
        cordaFuture.get();

        TradeState brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        cordaFuture = broker1Node.startFlow(new TradeFlow.Settle(brokerTrade.getId()));
        mockNet.runNetwork();
        cordaFuture.get();

        brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);
        assertTrue(brokerTrade.getState().equals(State.SETTLED));

        TradeState issuerTrade = issuerNode.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(1).getState().getData();
        assertTrue(issuerTrade.getState().equals(State.SETTLED));
    }

    @Test
    public void setAFSTest() throws ExecutionException, InterruptedException {
        settleOfflineTradeTest();

        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        OfferState offer = issuerNode.getServices().getVaultService().queryBy(OfferState.class, criteria).getStates().get(1).getState().getData();
        CordaFuture cordaFuture = issuerNode.startFlow(new OfferFlow.setAFS(offer.getOfferId(), true));
        mockNet.runNetwork();
        cordaFuture.get();

        OfferState broker2Offer = broker2Node.getServices().getVaultService().queryBy(OfferState.class,criteria).getStates().get(1).getState().getData();
        assertTrue(broker2Offer.getAfsSize() == 8000);
    }

    @Test
    public void settleUponAcceptanceTest() throws ExecutionException, InterruptedException {
        // we issue tokens
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        CordaFuture cordaFuture = issuerNode.startFlow(new USDFiatTokenFlow.Issue(broker1, 1000000));
        mockNet.runNetwork();
        cordaFuture.get();

        // get the current balance
        TokenType fiat = FiatCurrency.Companion.getInstance("USD");
        Amount balance = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), fiat);
        assertTrue(balance.getQuantity() == 1000000 );

        // generate the trade
        generaterOfflineTradeWithNewOfferTest();

        TradeState brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();

        // accept it
        cordaFuture = broker1Node.startFlow(new TradeFlow.AcceptBuyer(brokerTrade.getId()));
        mockNet.runNetwork();
        cordaFuture.get();

        // should be settled
        brokerTrade = broker1Node.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(0).getState().getData();
        assertNotNull(brokerTrade);
        assertTrue(brokerTrade.getState().equals(State.SETTLED));

        TradeState issuerTrade = issuerNode.getServices().getVaultService().queryBy(TradeState.class, criteria).getStates().get(1).getState().getData();
        assertTrue(issuerTrade.getState().equals(State.SETTLED));

        balance = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), fiat);
        assertTrue(balance.getQuantity() == 999000);

    }
    @After
    public void cleanUp(){
        TestHelper.cleanUpNetwork();
    }
}
