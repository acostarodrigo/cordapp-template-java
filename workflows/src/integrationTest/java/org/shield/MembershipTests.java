package org.shield;

import com.r3.businessnetworks.membership.flows.bno.ActivateMembershipFlow;
import com.r3.businessnetworks.membership.flows.bno.SuspendMembershipFlow;
import com.r3.businessnetworks.membership.flows.member.GetMembershipsFlow;
import com.r3.businessnetworks.membership.flows.member.RequestMembershipFlow;
import com.r3.businessnetworks.membership.states.MembershipState;
import com.r3.businessnetworks.membership.states.SimpleMembershipMetadata;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.membership.ShieldMetadata;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.shield.TestHelper.*;

public class MembershipTests {
    private Party bno;
    @Before
    public void setupNetwork(){
        TestHelper.setupNetwork();
        // we get the bno name from the configuration file
        String bnoString = "O=BNO,L=New York,C=US";
        CordaX500Name bnoName = CordaX500Name.parse(bnoString);
        bno = issuerNode.getServices().getNetworkMapCache().getPeerByLegalName(bnoName);
    }


    @Test
    public void IssuerRequestJoiningTest() throws ExecutionException, InterruptedException {
        // generate a simple request with role issuer
        SimpleMembershipMetadata simpleMembershipMetadata = new SimpleMembershipMetadata("role", "issuer");
        CordaFuture<SignedTransaction> membershipFuture = issuerNode.startFlow(new RequestMembershipFlow(bno,simpleMembershipMetadata));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = membershipFuture.get();

        // bno approves
        CordaFuture<SignedTransaction> bnoFuture = bnoNode.startFlow(new ActivateMembershipFlow(signedTransaction.getCoreTransaction().outRef(0)));
        mockNet.runNetwork();
        bnoFuture.get();

        CordaFuture<Map<Party, ? extends StateAndRef<? extends MembershipState<? extends Object>>>> membershipsFuture = issuerNode.startFlow(new GetMembershipsFlow(bno,false,true));
        mockNet.runNetwork();
        Map<Party, ? extends StateAndRef<? extends MembershipState<? extends Object>>> memberships = membershipsFuture.get();

        // we get validations from bno
        assertTrue(memberships.get(issuer).getState().getData().isActive());
    }

    @Test
    public void issuerRejectedTest() throws ExecutionException, InterruptedException {
        // generate a simple request with role issuer
        SimpleMembershipMetadata simpleMembershipMetadata = new SimpleMembershipMetadata("role", "issuer");
        CordaFuture<SignedTransaction> membershipFuture = issuerNode.startFlow(new RequestMembershipFlow(bno,simpleMembershipMetadata));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = membershipFuture.get();

        // bno approves
        CordaFuture<SignedTransaction> bnoFuture = bnoNode.startFlow(new ActivateMembershipFlow(signedTransaction.getCoreTransaction().outRef(0)));
        mockNet.runNetwork();
        bnoFuture.get();

        membershipFuture = broker1Node.startFlow(new RequestMembershipFlow(bno,simpleMembershipMetadata));
        mockNet.runNetwork();
        signedTransaction = membershipFuture.get();

        // bno approves
        bnoFuture = bnoNode.startFlow(new ActivateMembershipFlow(signedTransaction.getCoreTransaction().outRef(0)));
        mockNet.runNetwork();
        SignedTransaction broker1Transaction = bnoFuture.get();

        CordaFuture<Map<Party, ? extends StateAndRef<? extends MembershipState<? extends Object>>>> membershipsFuture = broker1Node.startFlow(new GetMembershipsFlow(bno,false,false));
        mockNet.runNetwork();
        Map<Party, ? extends StateAndRef<? extends MembershipState<? extends Object>>> memberships = membershipsFuture.get();

        // we get validations from bno
        assertTrue(memberships.get(broker1).getState().getData().isActive());
        assertTrue(memberships.get(issuer).getState().getData().isActive());

        // bno suspends broker
        bnoFuture = bnoNode.startFlow(new SuspendMembershipFlow(broker1Transaction.getCoreTransaction().outRef(0)));
        mockNet.runNetwork();
        bnoFuture.get();

        membershipsFuture = issuerNode.startFlow(new GetMembershipsFlow(bno,false,false));
        mockNet.runNetwork();
        memberships = membershipsFuture.get();
        assertNull(memberships.get(broker1));
        assertTrue(memberships.get(issuer).getState().getData().isActive());

    }

    @Test
    public void membershipFlowRequestTest() throws ExecutionException, InterruptedException {
        // we will issue a request with incorrect email
        ShieldMetadata metadata = new ShieldMetadata("Rodrigo", Arrays.asList(ShieldMetadata.OrgType.BOND_PARTICIPANT), "rodrigo@hotmail.com", Arrays.asList());

        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(new RequestMembershipFlow(bno,metadata));
        mockNet.runNetwork();
        SignedTransaction requestTransaction = cordaFuture.get();
        assertNotNull(requestTransaction);

        for (StateAndRef<MembershipState> stateAndRefs : bnoNode.getServices().getVaultService().queryBy(MembershipState.class).getStates()){
            System.out.println(stateAndRefs.getState().getData().toString());
        }
        System.out.println("State: " + requestTransaction.getCoreTransaction().outRef(0).getState().getData().toString());
        Future<SignedTransaction> aproveFuture = bnoNode.startFlow(new ActivateMembershipFlow(requestTransaction.getCoreTransaction().outRef(0)));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = aproveFuture.get();
        assertNotNull(signedTransaction);

        Future<Map<Party, ? extends StateAndRef<? extends MembershipState<? extends Object>>>> future = issuerNode.startFlow(new GetMembershipsFlow(bno,false,true));
        mockNet.runNetwork();
        Map<Party, ? extends StateAndRef<? extends MembershipState<? extends Object>>> memberships = future.get();
        assertNotNull(memberships);
    }


    @After
    public void closeNetwork(){
        TestHelper.cleanUpNetwork();
    }
}
