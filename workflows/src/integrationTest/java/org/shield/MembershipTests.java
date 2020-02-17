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

/**
 * Include tests to add, remove and change membership to Shield network.
 * No configuration of network is executed because most of this tests are executed from other tests
 * that already configure network
 */
public class MembershipTests {
    // Makes issuer node an issuer member
    @Test
    public void configIssuerTest() throws ExecutionException, InterruptedException {
        // the mocked BNO
        CordaX500Name bnoName = CordaX500Name.parse("O=BNO,L=New York,C=US");
        Party bno = issuerNode.getServices().getNetworkMapCache().getPeerByLegalName(bnoName);

        // we execute the request
        ShieldMetadata metadata = new ShieldMetadata("Issuer", Arrays.asList(ShieldMetadata.OrgType.BOND_PARTICIPANT, ShieldMetadata.OrgType.NETWORK_TREASURER), "rodrigocontact.com", Arrays.asList(ShieldMetadata.BondRole.ISSUER, ShieldMetadata.BondRole.BUYER, ShieldMetadata.BondRole.SELLER), null, null);
        Future<SignedTransaction> signedTransactionFuture = issuerNode.startFlow(new RequestMembershipFlow(bno,metadata));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = signedTransactionFuture.get();
        assertNotNull(signedTransaction);

        // BNO approves
        signedTransactionFuture = bnoNode.startFlow(new ActivateMembershipFlow(signedTransaction.getCoreTransaction().outRef(0)));
        mockNet.runNetwork();
        signedTransaction = signedTransactionFuture.get();
        assertNotNull(signedTransaction);

        CordaFuture<Map<Party, ? extends StateAndRef<? extends MembershipState<? extends Object>>>> membershipsFuture = issuerNode.startFlow(new GetMembershipsFlow(bno,false,false));
        mockNet.runNetwork();
        MembershipState<ShieldMetadata> membershipState = (MembershipState) membershipsFuture.get().get(issuer).getState().getData();
        assertNotNull(membershipState);
        assertTrue(membershipState.isActive());
    }

    @Test
    public void configBuyerTest() throws ExecutionException, InterruptedException {
        // the mocked BNO
        CordaX500Name bnoName = CordaX500Name.parse("O=BNO,L=New York,C=US");
        Party bno =  broker1Node.getServices().getNetworkMapCache().getPeerByLegalName(bnoName);

        // we execute the request
        ShieldMetadata metadata = new ShieldMetadata("Buyer", Arrays.asList(ShieldMetadata.OrgType.BOND_PARTICIPANT), "rodrigo@contact.com", Arrays.asList(ShieldMetadata.BondRole.BUYER), null, null);
        Future<SignedTransaction> signedTransactionFuture = broker1Node.startFlow(new RequestMembershipFlow(bno,metadata));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = signedTransactionFuture.get();
        assertNotNull(signedTransaction);

        // BNO approves
        signedTransactionFuture = bnoNode.startFlow(new ActivateMembershipFlow(signedTransaction.getCoreTransaction().outRef(0)));
        mockNet.runNetwork();
        signedTransaction = signedTransactionFuture.get();
        assertNotNull(signedTransaction);

        CordaFuture<Map<Party, ? extends StateAndRef<? extends MembershipState<? extends Object>>>> membershipsFuture = broker1Node.startFlow(new GetMembershipsFlow(bno,false,false));
        mockNet.runNetwork();
        MembershipState<ShieldMetadata> membershipState = (MembershipState) membershipsFuture.get().get(broker1).getState().getData();
        assertNotNull(membershipState);
        assertTrue(membershipState.isActive());
    }

    @Test
    public void configTreasurerTest() throws ExecutionException, InterruptedException {
        // the mocked BNO
        CordaX500Name bnoName = CordaX500Name.parse("O=BNO,L=New York,C=US");
        Party bno = issuerNode.getServices().getNetworkMapCache().getPeerByLegalName(bnoName);

        // we execute the request
        ShieldMetadata metadata = new ShieldMetadata("Issuer", Arrays.asList(ShieldMetadata.OrgType.NETWORK_TREASURER, ShieldMetadata.OrgType.BOND_PARTICIPANT), "rodrigocontact.com", Arrays.asList(ShieldMetadata.BondRole.ISSUER, ShieldMetadata.BondRole.BUYER), null, null);
        Future<SignedTransaction> signedTransactionFuture = broker2Node.startFlow(new RequestMembershipFlow(bno,metadata));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = signedTransactionFuture.get();
        assertNotNull(signedTransaction);

        // BNO approves
        signedTransactionFuture = bnoNode.startFlow(new ActivateMembershipFlow(signedTransaction.getCoreTransaction().outRef(0)));
        mockNet.runNetwork();
        signedTransaction = signedTransactionFuture.get();
        assertNotNull(signedTransaction);

        CordaFuture<Map<Party, ? extends StateAndRef<? extends MembershipState<? extends Object>>>> membershipsFuture = broker2Node.startFlow(new GetMembershipsFlow(bno,false,false));
        mockNet.runNetwork();
        MembershipState<ShieldMetadata> membershipState = (MembershipState) membershipsFuture.get().get(broker2).getState().getData();
        assertNotNull(membershipState);
        assertTrue(membershipState.isActive());
    }

}
