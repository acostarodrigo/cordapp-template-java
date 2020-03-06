package org.shield;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.custodian.CustodianState;
import org.shield.trade.TradeState;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;
import static org.shield.TestHelper.*;

public class CustodianTests {
    private BondFlowTests bondFlowTests;
    private TradeTests tradeTests;

    @Before
    public void configNetwork() throws ExecutionException, InterruptedException {
        bondFlowTests = new BondFlowTests();
        tradeTests = new TradeTests();
        tradeTests.setNetwork();
    }

    @Test
    public void sendBondTest() throws ExecutionException, InterruptedException {
        CustodianState custodianState = null;
        bondFlowTests.issueBondTest();

        custodianState = issuerNode.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertNotNull(custodianState);

        custodianState = broker2Node.getServices().getVaultService().queryBy(CustodianState.class).getStates().get(0).getState().getData();
        assertNotNull(custodianState);
    }

    @Test
    public void sendTradeTest() throws ExecutionException, InterruptedException {
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

    @After
    public void cleanUp(){
        TestHelper.cleanUpNetwork();
    }
}
