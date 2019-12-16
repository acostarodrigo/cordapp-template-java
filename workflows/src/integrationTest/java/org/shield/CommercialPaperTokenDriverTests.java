package org.shield;

import com.google.common.collect.ImmutableList;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NetworkParameters;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.TestCordapp;
import net.corda.testing.node.User;
import org.junit.Assert;
import org.junit.Test;
import org.shield.flows.trade.TradeFlow;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static net.corda.testing.driver.Driver.driver;


public class CommercialPaperTokenDriverTests {
    private CordaRPCOps issuer;
    private CordaRPCOps broker1;
    private CordaRPCOps broker2;

    private CordaRPCClient clientIssuer;

    private NodeHandle issuerNode;
    private NodeHandle broker1Node;
    private NodeHandle broker2Node;


    @Test
    public void startNodes() {
        TestIdentity issuerIdentity = new TestIdentity(new CordaX500Name("Issuer", "", "US"));
        TestIdentity broker1Identity = new TestIdentity(new CordaX500Name("Broker1", "", "US"));
        TestIdentity broker2Identity = new TestIdentity(new CordaX500Name("Broker2", "", "US"));
        System.out.println("Rodrigo starting nodes...");
        HashSet<TestCordapp> cordapps = new HashSet<>(asList( TestCordapp.findCordapp("com.r3.corda.lib.tokens.money"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
                TestCordapp.findCordapp("com.r3.businessnetworks.membership"),
                TestCordapp.findCordapp("org.shield.token")));

        // in order to support token flows, network must be version 4 compatible, so we are copying default parameters
        // and creating new ones with minimum platform version set to 4.
        DriverParameters driverParameters = new DriverParameters().withIsDebug(true);
        NetworkParameters networkParameters = driverParameters.getNetworkParameters();
        NetworkParameters newNetworkParameters = networkParameters.copy(4, networkParameters.getNotaries(), networkParameters.getMaxMessageSize(), networkParameters.getMaxTransactionSize(), networkParameters.getModifiedTime(), networkParameters.getEpoch(), networkParameters.getWhitelistedContractImplementations(), networkParameters.getEventHorizon());
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true).withNetworkParameters(newNetworkParameters).withCordappsForAllNodes(cordapps), dsl -> {

            // the user and password we will use in all nodes.
            User user1 = new User("user1", "test", new HashSet<>(asList("InvokeRpc.startFlowDynamic", "ALL", "StartFlow.org.shield.flows.commercialPaper.CommercialPaperTokenFlow$IssueFungibleToken", "StartFlow.org.shield.flows.commercialPaper.CommercialPaperTokenFlow$IssueType")));

            // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(issuerIdentity.getName()).withRpcUsers(Arrays.asList(user1))),
                    dsl.startNode(new NodeParameters().withProvidedName(broker1Identity.getName()).withRpcUsers(Arrays.asList(user1))),
                    dsl.startNode(new NodeParameters().withProvidedName(broker2Identity.getName()).withRpcUsers(Arrays.asList(user1)))
            );

            try {
                issuerNode = handleFutures.get(0).get();
                broker1Node = handleFutures.get(1).get();
                broker2Node = handleFutures.get(2).get();


                clientIssuer = new CordaRPCClient(issuerNode.getRpcAddress());
                issuer = clientIssuer.start("user1", "test").getProxy();

                CordaRPCClient clientBroker1 = new CordaRPCClient(broker1Node.getRpcAddress());
                broker1 = clientBroker1.start("user1", "test").getProxy();

                CordaRPCClient clientBroker2 = new CordaRPCClient(broker2Node.getRpcAddress());
                broker2 = clientBroker2.start("user1", "test").getProxy();
                System.out.println("Rodrigo node started...");
                // make sure everything is started and ok to continue
                Assert.assertNotNull(issuerNode);
                Assert.assertNotNull(broker1Node);
                Assert.assertNotNull(broker2Node);

                Assert.assertNotNull(issuer);
                Assert.assertNotNull(broker1);
                Assert.assertNotNull(broker2);

                System.out.println("Rodrigo starting test.");

                Date offeringDate = new Date();
                BigDecimal fungibleAmount = BigDecimal.ONE;

                Party holder = broker1Node.getNodeInfo().getLegalIdentities().get(0);
                UniqueIdentifier id = issuer.startFlowDynamic(TradeFlow.PreIssue.class,offeringDate,fungibleAmount,holder).getReturnValue().get();
                Assert.assertNotNull(id);
            } catch (Exception e) {
                System.out.println("Exception during node initialization:" + e.toString());
            }

            return  null;
        });
    }
}
