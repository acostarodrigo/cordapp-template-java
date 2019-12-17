package org.shield;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.shield.bond.BondState;
import org.shield.bond.DealType;
import org.shield.flows.bond.BondFlow;
import org.shield.flows.trade.TradeFlow.Accept;
import org.shield.flows.trade.TradeFlow.Cancel;
import org.shield.trade.TradeState;

import javax.annotation.Signed;
import java.util.Currency;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.shield.TestHelper.*;

public class TradeFlowTests {
    private final Date offeringDate = new Date(2020,12,12);
    @Before
    public void setUp() {
        TestHelper.setupNetwork();
    }


    /**
     * Creates a Trade.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void preIssueTest() throws ExecutionException, InterruptedException {
        MembershipTests membershipTests = new MembershipTests();
        membershipTests.configIssuerTest();

        // we create the bond
        BondState bond = new BondState(issuer, "Rodrigo", Currency.getInstance("USD"), new Date(), 1,1,1, DealType.REG_S, 1,10000,99,new Date(),99.8,1);

        // and we issue it.
        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new BondFlow.Issue(bond));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);
    }

    @After
    public void cleanUp() {
        mockNet.stopNodes();
    }
}


