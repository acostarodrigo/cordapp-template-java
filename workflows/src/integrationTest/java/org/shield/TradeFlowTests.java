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
import org.shield.flows.trade.TradeFlow.Accept;
import org.shield.flows.trade.TradeFlow.Cancel;
import org.shield.flows.trade.TradeFlow.Issue;
import org.shield.flows.trade.TradeFlow.PreIssue;
import org.shield.trade.TradeState;

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

        // we issue arrangement from issuer to broker1
        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, 70, offeringDate));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);

        int size = issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().size();
        TradeState issuerState = issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();

        StateAndRef<TradeState> stateAndRef = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0);
        TradeState brokerDealerState = stateAndRef.getState().getData();

        // boths nodes must have the same arrangement state
        Assert.assertEquals(id, issuerState.getId());
        Assert.assertEquals(id, brokerDealerState.getId());
        //Assert.assertEquals(issuerState, brokerDealerState);

        SecureHash txId = issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getRef().getTxhash();
        SignedTransaction tx = issuerNode.getServices().getValidatedTransactions().getTransaction(txId);

        // transaction contains both issuer and broker dealer signature
        Assert.assertTrue(tx.getRequiredSigningKeys().contains(issuer.getOwningKey()));
        Assert.assertTrue(tx.getRequiredSigningKeys().contains(broker1.getOwningKey()));

    }


    @Test
    public void cancelFlowWithoutInitTest() throws ExecutionException, InterruptedException {
        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, 100, offeringDate));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);

        CordaFuture<Void> cancelFuture = broker1Node.startFlow(new Cancel(id));
        mockNet.runNetwork();
        cancelFuture.get();

        for (StateAndRef<TradeState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(TradeState.State.CANCELLED));
            }
        }
    }

    @Test
    public void cancelFlowWithInitTest() throws ExecutionException, InterruptedException {

        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, 100, offeringDate));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);

        CordaFuture<Void> cancelFuture = broker1Node.startFlow(new Cancel(id));
        mockNet.runNetwork();
        cancelFuture.get();

        for (StateAndRef<TradeState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(TradeState.State.CANCELLED));
            }
        }
    }

    @Test
    public void AcceptFlowWithInitTest() throws ExecutionException, InterruptedException {
        int initialSize = 100;
        int finalSize = 50;
        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, initialSize, offeringDate));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);

        CordaFuture<Void> cancelFuture = broker1Node.startFlow(new Accept(id, finalSize));
        mockNet.runNetwork();
        cancelFuture.get();


        for (StateAndRef<TradeState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(TradeState.State.ACCEPTED));
                Assert.assertTrue(stateAndRef.getState().getData().getSize() == finalSize);
            }
        }
    }

    @Test
    public void issueFlowTest() throws ExecutionException, InterruptedException {
        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, 100, offeringDate));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);

        CordaFuture<Void> cancelFuture = broker1Node.startFlow(new Accept(id, 50));
        mockNet.runNetwork();
        cancelFuture.get();


        for (StateAndRef<TradeState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(TradeState.State.ACCEPTED));
                Assert.assertTrue(stateAndRef.getState().getData().getSize() == 50);
            }
        }

        CordaFuture<UniqueIdentifier> paperFuture = issuerNode.startFlow(new Issue(id));

        mockNet.runNetwork();
        UniqueIdentifier paperId = paperFuture.get();
        Assert.assertNotNull(paperId);

        // we will get the arrangemenState and validate it has the commercialPaper assigned.
        TradeState tradeState = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        Assert.assertEquals(tradeState.getPaperId(), paperId);
    }

    @Test
    public void issueFlowExistingCommercialPaperTest() throws ExecutionException, InterruptedException {
        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, 100, offeringDate));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);

        CordaFuture<Void> acceptFuture = broker1Node.startFlow(new Accept(id, 50));
        mockNet.runNetwork();
        acceptFuture.get();


        for (StateAndRef<TradeState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(TradeState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(TradeState.State.ACCEPTED));
                Assert.assertTrue(stateAndRef.getState().getData().getSize() == 50);
            }
        }

        CordaFuture<UniqueIdentifier> paperFuture = issuerNode.startFlow(new Issue(id));

        mockNet.runNetwork();
        UniqueIdentifier paperId = paperFuture.get();

        Assert.assertNotNull(paperId);

        // we will get the arrangemenState and validate it has the commercialPaper assigned.
        TradeState tradeState = broker1Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        Assert.assertEquals(tradeState.getPaperId(), paperId);

        // we will issue another arrangement from broker2
        preIssueFuture = issuerNode.startFlow(new PreIssue(broker2, 30, offeringDate));
        mockNet.runNetwork();
        id = preIssueFuture.get();
        Assert.assertNotNull(id);

        acceptFuture = broker2Node.startFlow(new Accept(id, 50));
        mockNet.runNetwork();
        acceptFuture.get();

        paperFuture = issuerNode.startFlow(new Issue(id));

        mockNet.runNetwork();
        UniqueIdentifier broker2PaperId = paperFuture.get();
        Assert.assertEquals(paperId, broker2PaperId);

        TradeState broker2TradeState = broker2Node.getServices().getVaultService().queryBy(TradeState.class).getStates().get(0).getState().getData();
        Assert.assertEquals(broker2TradeState.getPaperId(), paperId);

        // commercial paper at issuer should have sum of sizes
    }

    @After
    public void cleanUp() {
        mockNet.stopNodes();
    }
}


