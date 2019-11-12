package org.shield;

import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.node.*;
import org.junit.Before;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;

public class TestHelper {
    // we avoid instantiation
    private TestHelper(){}

    private static MockNetwork mockNet;
    public static StartedMockNode issuerNode;
    public static StartedMockNode broker1Node;
    public static StartedMockNode broker2Node;
    public static Party issuer;
    public static Party broker1;
    public static Party broker2;

    /**
     * creates a mock network with three nodes. An issuer and 2 broker dealers.
     * This method is supposed to be called on test with @Before annotation
     */
    public static void setupNetwork() {
        HashSet<TestCordapp> cordapps = new HashSet<>(asList( TestCordapp.findCordapp("com.r3.corda.lib.tokens.money"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
            TestCordapp.findCordapp("org.shield.token")));
        List<String> packages = Arrays.asList("org.shield.token");

        DriverParameters driverParameters = new DriverParameters().withIsDebug(true).withCordappsForAllNodes(cordapps);
        NetworkParameters networkParameters = driverParameters.getNetworkParameters();
        NetworkParameters parameters = networkParameters.copy(4, networkParameters.getNotaries(), networkParameters.getMaxMessageSize(), networkParameters.getMaxTransactionSize(), networkParameters.getModifiedTime(), networkParameters.getEpoch(), networkParameters.getWhitelistedContractImplementations());

        InMemoryMessagingNetwork.ServicePeerAllocationStrategy servicePeerAllocationStrategy = new InMemoryMessagingNetwork.ServicePeerAllocationStrategy.Random();
        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters(cordapps);
        MockNetworkNotarySpec mockNetworkNotarySpec = new MockNetworkNotarySpec(new CordaX500Name("Notary", "London", "GB"), false);
        mockNet =  new MockNetwork(packages,mockNetworkParameters,false,false,servicePeerAllocationStrategy,Arrays.asList(mockNetworkNotarySpec),parameters);
        issuerNode = mockNet.createNode(new CordaX500Name("Issuer", "London", "GB"));
        broker1Node = mockNet.createNode(new CordaX500Name("Broker1", "London", "GB"));
        broker2Node = mockNet.createNode(new CordaX500Name("Broker2", "London", "GB"));

        issuer = issuerNode.getInfo().getLegalIdentities().get(0);
        broker1 = broker1Node.getInfo().getLegalIdentities().get(0);
        broker2 = broker2Node.getInfo().getLegalIdentities().get(0);
    }

    /**
     * cleans up the network.
     * This method is supposed to be called on test with @After annotation
     */
    public static void cleanUpNetwork() {
        mockNet.stopNodes();
    }
}
