package org.shield;

import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.bond.BondState;
import org.shield.custodian.CustodianState;
import org.shield.fiat.FiatTransaction;
import org.shield.trade.State;
import org.shield.trade.TradeState;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.shield.TestHelper.*;

public class CustodianTests {
    private BondFlowTests bondFlowTests;
    private TradeTests tradeTests;

    @Before
    public void configNetwork() throws ExecutionException, InterruptedException {
        bondFlowTests = new BondFlowTests();
        tradeTests = new TradeTests();

    }

    @Test
    public void sendBondTest() throws ExecutionException, InterruptedException {
        CustodianState custodianState = null;
        // we issue the bond
        bondFlowTests.setUp();
        bondFlowTests.issueBondTest();

        custodianState = issuerNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertNotNull(custodianState);
        // and validate custodian has it
        custodianState = custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertNotNull(custodianState);

        JsonObject custodianResult = custodianState.toJson();
        assertNotNull(custodianResult);
    }

    @Test
    public void sendTradeTest() throws ExecutionException, InterruptedException {
        tradeTests.setNetwork();
        tradeTests.generateTradeTest();

        CustodianState custodianState = null;
        TradeState tradeState = null;
        custodianState = issuerNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertNotNull(custodianState);
        tradeState = issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertNotNull(tradeState);

        custodianState = broker1Node.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertNotNull(custodianState);
        tradeState = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        assertNotNull(tradeState);

        custodianState = custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertNotNull(custodianState);
        custodianState = custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(1).getState().getData();
        assertNotNull(custodianState);
    }

    @Test
    public void acceptOfflineTradeTest() throws ExecutionException, InterruptedException {
        tradeTests.setNetwork();
        tradeTests.acceptOfflineTradeTest();

        CustodianState custodianState = null;
        custodianState = custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertNotNull(custodianState);
        TradeState trade = custodianState.getTrades().get(0);
        assertNotNull(trade);
        assertEquals(State.PENDING, trade.getState());

        // custodian doesn't have any state
        assertTrue(custodianNode.getServices().getVaultService().queryBy(TradeState.class).getStates().size() == 0);
        assertTrue(custodianNode.getServices().getVaultService().queryBy(BondState.class).getStates().size() == 0);

        CustodianState traderCustodianState = null;
        traderCustodianState = broker1Node.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertEquals(traderCustodianState.getTrades().get(0), custodianState.getTrades().get(0));
    }

    @Test
    public void SettleTradeCustodianTest() throws ExecutionException, InterruptedException {
        tradeTests.setNetwork();
        tradeTests.settleTradeTest();

        // we must have to custodianStates, from issuer and trader
        assertTrue(custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().size() == 2);

        CustodianState custodianState = null;
        custodianState = custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(1).getState().getData();
        assertNotNull(custodianState);
        TradeState trade = custodianState.getTrades().get(0);
        assertNotNull(trade);
        assertEquals(State.SETTLED, trade.getState());

        // custodian doesn't have any state
        assertTrue(custodianNode.getServices().getVaultService().queryBy(TradeState.class).getStates().size() == 0);
        assertTrue(custodianNode.getServices().getVaultService().queryBy(BondState.class).getStates().size() == 0);

        CustodianState traderCustodianState = null;
        traderCustodianState = broker1Node.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertEquals(traderCustodianState.getTrades().get(0), custodianState.getTrades().get(0));
    }

    @Test
    public void settleTradeWithCustodianTest() throws ExecutionException, InterruptedException {
        tradeTests.setNetwork();
        tradeTests.acceptTradeWithBalance();

        // we must have to custodianStates, from issuer and trader
        assertTrue(custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().size() == 2);

        CustodianState custodianState = null;
        custodianState = custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(1).getState().getData();
        assertNotNull(custodianState);
        TradeState trade = custodianState.getTrades().get(0);
        assertNotNull(trade);
        assertEquals(State.SETTLED, trade.getState());

        custodianState = custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertNotNull(custodianState);
        trade = custodianState.getTrades().get(0);
        assertNotNull(trade);
        assertEquals(State.SETTLED, trade.getState());

        // custodian doesn't have any state
        assertTrue(custodianNode.getServices().getVaultService().queryBy(TradeState.class).getStates().size() == 0);
        assertTrue(custodianNode.getServices().getVaultService().queryBy(BondState.class).getStates().size() == 0);

        CustodianState traderCustodianState = null;
        traderCustodianState = broker1Node.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertEquals(traderCustodianState.getTrades().get(0), custodianState.getTrades().get(0));


        // we must have the FiatStates with money movement
        assertNotNull(custodianState.getFiatState());
    }

    @Test
    public void fiatStateTests() throws ExecutionException, InterruptedException {
        // settle a trade between issuer and brokerDealer1
        settleTradeWithCustodianTest();

        CustodianState issuerState =  issuerNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        CustodianState brokerState =  broker1Node.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();

        CustodianState custodian0State =  custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        CustodianState custodian1State =  custodianNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(1).getState().getData();

        // all states must have FiatStates on them
        assertNotNull(issuerState.getFiatState());
        assertNotNull(brokerState.getFiatState());
        assertNotNull(custodian0State.getFiatState());
        assertNotNull(custodian1State.getFiatState());

        // issuer only 1 transaction in the state
        assertTrue(issuerState.getFiatState().getFiatTransactionList().size() == 1);
        // trader has 3 transactions. x2 Issuing and paying
        assertTrue(brokerState.getFiatState().getFiatTransactionList().size() == 3);
        // and the same on the custodians
        assertTrue(custodian0State.getFiatState().getFiatTransactionList().size() == 1);
        assertTrue(custodian1State.getFiatState().getFiatTransactionList().size() == 3);

        // lets try to toJson request
        assertNotNull(issuerState.toJson());
        assertNotNull(brokerState.toJson());
        assertNotNull(custodian0State.toJson());
        assertNotNull(custodian1State.toJson());

        for (FiatTransaction fiatTransaction : custodian0State.getFiatState().getFiatTransactionList()){
            assertNotNull(fiatTransaction.toJson());
        }

        System.out.println(custodian0State.toJson());

    }


    @After
    public void cleanUp(){
        TestHelper.cleanUpNetwork();
    }
}
