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
import org.shield.flows.arrangement.ArrangementFlow.*;
import org.shield.flows.init.BrokerDealerInit;
import org.shield.flows.init.IssuerInit;
import org.shield.states.ArrangementState;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.shield.TestHelper.*;

public class ArrangementFlowTests {
    private final Date offeringDate = new Date(2020,12,12);
    @Before
    public void setUp() {
        TestHelper.setupNetwork();
    }

    @Test
    public void preIssueWithoutInitTest() throws ExecutionException, InterruptedException {

        // we issue arrangement from issuer to broker1
        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, 70, offeringDate));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);

        int size = issuerNode.getServices().getVaultService().queryBy(ArrangementState.class).getStates().size();
        ArrangementState issuerState = issuerNode.getServices().getVaultService().queryBy(ArrangementState.class).getStates().get(0).getState().getData();

        StateAndRef<ArrangementState> stateAndRef = broker1Node.getServices().getVaultService().queryBy(ArrangementState.class).getStates().get(0);
        ArrangementState brokerDealerState = stateAndRef.getState().getData();

        // boths nodes must have the same arrangement state
        Assert.assertEquals(id, issuerState.getId());
        Assert.assertEquals(id, brokerDealerState.getId());
        //Assert.assertEquals(issuerState, brokerDealerState);

        SecureHash txId = issuerNode.getServices().getVaultService().queryBy(ArrangementState.class).getStates().get(0).getRef().getTxhash();
        SignedTransaction tx = issuerNode.getServices().getValidatedTransactions().getTransaction(txId);

        // transaction contains both issuer and broker dealer signature
        Assert.assertTrue(tx.getRequiredSigningKeys().contains(issuer.getOwningKey()));
        Assert.assertTrue(tx.getRequiredSigningKeys().contains(broker1.getOwningKey()));

    }

    @Test
    public void preIssueWithValidInitTest() throws ExecutionException, InterruptedException {
        // we add issuer as valid issuer for broker1
        CordaFuture<Void> initFuture = broker1Node.startFlow(new BrokerDealerInit(Arrays.asList(issuer)));
        mockNet.runNetwork();
        initFuture.get();

        preIssueWithoutInitTest();
    }

    @Test(expected = ExecutionException.class)
    public void preIssueWithInvalidInitTest() throws ExecutionException, InterruptedException {
        // we add broker2 as valid issuer for broker1
        CordaFuture<Void> initFuture = broker1Node.startFlow(new BrokerDealerInit(Arrays.asList(broker2)));
        mockNet.runNetwork();
        initFuture.get();

        // we issue from issuer an arrangement to broker1
        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, 100, offeringDate));
        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get(); // this should trigger an exception
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

        for (StateAndRef<ArrangementState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(ArrangementState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(ArrangementState.State.CANCELLED));
            }
        }
    }

    @Test
    public void cancelFlowWithInitTest() throws ExecutionException, InterruptedException {
        CordaFuture<Void> issuerInitFuture = issuerNode.startFlow(new IssuerInit(Arrays.asList(broker1)));
        mockNet.runNetwork();
        issuerInitFuture.get();

        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, 100, offeringDate));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);

        CordaFuture<Void> cancelFuture = broker1Node.startFlow(new Cancel(id));
        mockNet.runNetwork();
        cancelFuture.get();

        for (StateAndRef<ArrangementState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(ArrangementState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(ArrangementState.State.CANCELLED));
            }
        }
    }

    @Test
    public void AcceptFlowWithInitTest() throws ExecutionException, InterruptedException {
        CordaFuture<Void> issuerInitFuture = issuerNode.startFlow(new IssuerInit(Arrays.asList(broker1)));
        mockNet.runNetwork();
        issuerInitFuture.get();

        int initialSize = 100;
        int finalSize = 50;
        CordaFuture<UniqueIdentifier> preIssueFuture = issuerNode.startFlow(new PreIssue(broker1, initialSize, offeringDate));

        mockNet.runNetwork();
        UniqueIdentifier id = preIssueFuture.get();
        Assert.assertNotNull(id);

        CordaFuture<Void> cancelFuture = broker1Node.startFlow(new Accept(id, finalSize));
        mockNet.runNetwork();
        cancelFuture.get();


        for (StateAndRef<ArrangementState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(ArrangementState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(ArrangementState.State.ACCEPTED));
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


        for (StateAndRef<ArrangementState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(ArrangementState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(ArrangementState.State.ACCEPTED));
                Assert.assertTrue(stateAndRef.getState().getData().getSize() == 50);
            }
        }

        CordaFuture<UniqueIdentifier> paperFuture = issuerNode.startFlow(new Issue(id));

        mockNet.runNetwork();
        UniqueIdentifier paperId = paperFuture.get();
        Assert.assertNotNull(paperId);

        // we will get the arrangemenState and validate it has the commercialPaper assigned.
        ArrangementState arrangementState = broker1Node.getServices().getVaultService().queryBy(ArrangementState.class).getStates().get(0).getState().getData();
        Assert.assertEquals(arrangementState.getPaperId(), paperId);
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


        for (StateAndRef<ArrangementState> stateAndRef : issuerNode.getServices().getVaultService().queryBy(ArrangementState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(id)){
                Assert.assertTrue(stateAndRef.getState().getData().getState().equals(ArrangementState.State.ACCEPTED));
                Assert.assertTrue(stateAndRef.getState().getData().getSize() == 50);
            }
        }

        CordaFuture<UniqueIdentifier> paperFuture = issuerNode.startFlow(new Issue(id));

        mockNet.runNetwork();
        UniqueIdentifier paperId = paperFuture.get();

        Assert.assertNotNull(paperId);

        // we will get the arrangemenState and validate it has the commercialPaper assigned.
        ArrangementState arrangementState = broker1Node.getServices().getVaultService().queryBy(ArrangementState.class).getStates().get(0).getState().getData();
        Assert.assertEquals(arrangementState.getPaperId(), paperId);

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

        ArrangementState broker2arrangementState = broker2Node.getServices().getVaultService().queryBy(ArrangementState.class).getStates().get(0).getState().getData();
        Assert.assertEquals(broker2arrangementState.getPaperId(), paperId);

        // commercial paper at issuer should have sum of sizes
    }

    @After
    public void cleanUp() {
        mockNet.stopNodes();
    }
}


