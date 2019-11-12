package org.shield;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.shield.flows.arrangement.PreIssueFlow;
import org.shield.flows.commercialPaper.CommercialPaperTokenFlow;
import org.shield.token.CommercialPaperTokenType;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class CommercialPaperTokenTests {
    //private final MockNetwork mockNet = new MockNetwork(new MockNetworkParameters(singletonList(findCordapp("org.shield.contracts"))));
    private MockNetwork mockNet;
    private StartedMockNode issuerNode;
    private StartedMockNode broker1Node;
    private StartedMockNode broker2Node;
    private Party issuer;
    private Party broker1;
    private Party broker2;

    @Before
    public void nodesSetUp() {
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

    @Test
    public void testIssueFungibleToken() throws ExecutionException, InterruptedException {
        Date offeringDate = new Date();
        CordaFuture<UniqueIdentifier> issueTokensBroker1 = issuerNode.startFlow(new CommercialPaperTokenFlow.IssueFungibleToken(offeringDate, 1L, broker1));

        mockNet.runNetwork();
        UniqueIdentifier id = issueTokensBroker1.get();
        assertNotNull(id);

        CommercialPaperTokenType tokenType = issuerNode.getServices().getVaultService().queryBy(CommercialPaperTokenType.class).component1().get(0).getState().getData();
        assertNotNull(tokenType);
        assertEquals(tokenType.getValuation(), 1L);
        // issuer only has one tokenType
        assertTrue(issuerNode.getServices().getVaultService().queryBy(CommercialPaperTokenType.class).component1().size() == 1);

        CommercialPaperTokenType tokenTypeBroker1 = broker1Node.getServices().getVaultService().queryBy(CommercialPaperTokenType.class).component1().get(0).getState().getData();
        assertNotNull(tokenTypeBroker1);
        assertEquals(tokenTypeBroker1.getValuation(), tokenType.getValuation());


        CordaFuture<UniqueIdentifier> issueTokensBroker2 = issuerNode.startFlow(new CommercialPaperTokenFlow.IssueFungibleToken(offeringDate, 10L, broker2));
        mockNet.runNetwork();
        issueTokensBroker2.get();

        CommercialPaperTokenType tokenTypeBroker2 = broker2Node.getServices().getVaultService().queryBy(CommercialPaperTokenType.class).component1().get(0).getState().getData();
        assertNotNull(tokenTypeBroker2);
        assertEquals(tokenTypeBroker2.getValuation(), 11);

        // now we compare the tokens
        FungibleToken fungibleTokenBroker1 = broker1Node.getServices().getVaultService().queryBy(FungibleToken.class).getStates().get(0).getState().getData();
        assertNotNull(fungibleTokenBroker1);

        FungibleToken fungibleTokenBroker2 = broker2Node.getServices().getVaultService().queryBy(FungibleToken.class).getStates().get(0).getState().getData();
        assertNotNull(fungibleTokenBroker2);

        assertEquals(tokenTypeBroker2.getValuation(), fungibleTokenBroker1.getAmount().getQuantity() + fungibleTokenBroker2.getAmount().getQuantity());
    }

    @After
    public void cleanUp() {
        mockNet.stopNodes();
    }
}
