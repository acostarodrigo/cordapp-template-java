package org.shield.flows.membership;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.businessnetworks.membership.flows.member.AmendMembershipMetadataFlow;
import com.r3.businessnetworks.membership.flows.member.AmendMembershipMetadataRequest;
import com.r3.businessnetworks.membership.flows.member.GetMembershipsFlow;
import com.r3.businessnetworks.membership.flows.member.PartyAndMembershipMetadata;
import com.r3.businessnetworks.membership.states.MembershipState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.cordapp.Cordapp;
import net.corda.core.cordapp.CordappConfig;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.shield.membership.ShieldMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deals with all role and membership validations
 */
public class MembershipFlows {
    // we don't allow instantiation
    private MembershipFlows(){};

    /**
     * Validates if the caller is a member of an Issuer organization.
     * Will return true if:
     *  is an active member
     *  is a OrgType.BOND_PARTICIPANT
     *  is a BondRole.ISSUER
     */
    public static class isIssuer extends FlowLogic<Boolean>{
        private Party issuer;

        public isIssuer(Party issuer) {
            this.issuer = issuer;
        }

        public isIssuer() {
            this.issuer = null;
        }

        @Override
        @Suspendable
        public Boolean call() throws FlowException {
            MembershipState membershipState = null;

            if (this.issuer == null)
             membershipState = subFlow(new getMembership());
            else
                membershipState = subFlow(new getMembership(issuer));

            // we will validate if issuer is an active member of the organization
            if (membershipState == null || !membershipState.isActive()) return false;

            // node must be bond participant and bond issuer
            ShieldMetadata metadata = (ShieldMetadata) membershipState.getMembershipMetadata();
            if (metadata.getOrgTypes().contains(ShieldMetadata.OrgType.BOND_PARTICIPANT) && metadata.getBondRoles().contains(ShieldMetadata.BondRole.ISSUER)) return true;

            return false;
        }
    }

    /**
     * Validates if the caller (or passed Party) is a member of an Buyer organization.
     * Will return true if:
     *  is an active member
     *  is a OrgType.BOND_PARTICIPANT
     *  is a BondRole.BUYER
     */
    public static class isBuyer extends FlowLogic<Boolean>{
        private Party buyer;

        public isBuyer(Party buyer) {
            this.buyer = buyer;
        }

        public isBuyer() {
            this.buyer = null;
        }

        @Override
        @Suspendable
        public Boolean call() throws FlowException {
            MembershipState membershipState = null;
            if (this.buyer == null)
                membershipState = subFlow(new getMembership());
            else
                membershipState = subFlow(new getMembership(buyer));

            // we will validate is an active member of the organization
            if (membershipState == null || !membershipState.isActive()) return false;

            // node must be bond participant and bond issuer
            ShieldMetadata metadata = (ShieldMetadata) membershipState.getMembershipMetadata();
            if (metadata.getOrgTypes().contains(ShieldMetadata.OrgType.BOND_PARTICIPANT) && metadata.getBondRoles().contains(ShieldMetadata.BondRole.BUYER)) return true;

            return false;
        }
    }

    /**
     * Validates if the caller is a member of an Issuer organization.
     * Will return true if:
     *  is an active member
     *  is a OrgType.BOND_PARTICIPANT
     *  is a BondRole.SELLER
     */
    public static class isSeller extends FlowLogic<Boolean>{
        private Party seller;

        public isSeller(Party seller) {
            this.seller = seller;
        }

        public isSeller() {
            this.seller = null;
        }

        @Override
        @Suspendable
        public Boolean call() throws FlowException {

            MembershipState membershipState = subFlow(new getMembership(seller));

            // we will validate is an active member of the organization
            if (membershipState == null || !membershipState.isActive()) return false;

            // node must be bond participant and bond issuer
            ShieldMetadata metadata = (ShieldMetadata) membershipState.getMembershipMetadata();
            if (metadata.getOrgTypes().contains(ShieldMetadata.OrgType.BOND_PARTICIPANT) && metadata.getBondRoles().contains(ShieldMetadata.BondRole.SELLER)) return true;

            return false;
        }
    }

    /**
     * Validates if the caller is a member of a custodian organization.
     * Will return true if:
     *  is an active member
     *  is a OrgType.CUSTODIAN
     *
     */
    public static class isCustodian extends FlowLogic<Boolean>{
        @Override
        @Suspendable
        public Boolean call() throws FlowException {

            MembershipState membershipState = subFlow(new getMembership());

            // we will validate is an active member of the organization
            if (membershipState == null || !membershipState.isActive()) return false;

            // node must be bond participant and bond issuer
            ShieldMetadata metadata = (ShieldMetadata) membershipState.getMembershipMetadata();
            if (metadata.getOrgTypes().contains(ShieldMetadata.OrgType.CUSTODIAN)) return true;

            return false;
        }
    }

    /**
     * Validates if the caller is a member of a network treasurer organization.
     * Will return true if:
     *  is an active member
     *  is a OrgType.NETWORK_TREASURER
     *
     */
    public static class isTreasure extends FlowLogic<Boolean>{
        @Override
        @Suspendable
        public Boolean call() throws FlowException {

            MembershipState membershipState = subFlow(new getMembership());

            // we will validate is an active member of the organization
            if (membershipState == null || !membershipState.isActive()) return false;

            // node must be bond participant and bond issuer
            ShieldMetadata metadata = (ShieldMetadata) membershipState.getMembershipMetadata();
            if (metadata.getOrgTypes().contains(ShieldMetadata.OrgType.NETWORK_TREASURER)) return true;

            return false;
        }
    }

    /**
     * Validates if caller is a member of the organization
     */
    public static class isMember extends FlowLogic<Boolean>{
        @Override
        @Suspendable
        public Boolean call() throws FlowException {
            MembershipState membershipState = subFlow(new getMembership());

            // we will validate is an active member of the organization
            if (membershipState == null || !membershipState.isActive()) return false;

            return true;
        }
    }

    /**
     * Retrieves the membership state from the BNO node.
     * Will fail if caller is not a member
     */
    public static class getMembership extends FlowLogic<MembershipState<? extends Object>>{
        private Party caller;

        /**
         * we allow getting membership of someone else than the caller.
         * @param caller
         */
        public getMembership(Party caller) {
            this.caller = caller;
        }

        /**
         * in case we want to know membership for caller identity
         */
        public getMembership() {
            this.caller = null;
        }

        @Override
        @Suspendable
        public MembershipState<? extends Object> call() throws FlowException {
            if (caller == null) caller = getOurIdentity();

            Party bno = subFlow(new getBNO());

            GetMembershipsFlow getMembershipsFlow = new GetMembershipsFlow(bno, false, false);
            StateAndRef<? extends MembershipState<? extends Object>> stateAndRef = subFlow(getMembershipsFlow).get(caller);

            // no membership found for specified caller
            if (stateAndRef == null) throw new FlowException(String.format("%s is not an organization member.",caller.toString()));
            return stateAndRef.getState().getData();
        }
    }

    /**
     * retrieves all memberships of the business network
     */
    @InitiatingFlow
    public static class GetAllMemberships extends FlowLogic<List<PartyAndMembershipMetadata>>{
        @Override
        @Suspendable
        public List<PartyAndMembershipMetadata> call() throws FlowException {
            Party bno = subFlow(new getBNO());

            GetMembershipsFlow getMembershipsFlow = new GetMembershipsFlow(bno, false, false);

            Map<Party,? extends StateAndRef<? extends MembershipState<? extends Object>>> memberships = subFlow(getMembershipsFlow);
            List<PartyAndMembershipMetadata> partyAndMembershipMetadataList = new ArrayList<>();
            for (Map.Entry<Party,? extends StateAndRef<? extends MembershipState<? extends Object>>> entry : memberships.entrySet()){
                ShieldMetadata shieldMetadata = (ShieldMetadata) entry.getValue().getState().getData().getMembershipMetadata();
                partyAndMembershipMetadataList.add(new PartyAndMembershipMetadata(entry.getKey(), shieldMetadata));
            }

            return partyAndMembershipMetadataList;
        }
    }


    /**
     * retrieves BNO data from config file and gets BNO Node from network.
     */
    private static class getBNO extends FlowLogic<Party>{
        @Override
        @Suspendable
        public Party call() throws FlowException {
            CordappConfig config = getServiceHub().getAppContext().getConfig();
            Cordapp cordapp = getServiceHub().getAppContext().getCordapp();
            String bnoString = config.getString("bno");

            if (bnoString == "" || bnoString.isEmpty()) {
                throw new FlowException("Configuration file doesn't include BNO data.");
            }

            CordaX500Name bnoName = CordaX500Name.parse(bnoString);
            Party bno = getServiceHub().getNetworkMapCache().getPeerByLegalName(bnoName);

            return bno;
        }
    }

    /**
     * Updates metadata. We have our own flow because AmendMembershipMetadataFlow
     * is not StartableByRPC
     */
    @StartableByRPC
    public static class updateMetadata extends FlowLogic<SignedTransaction>{
        private Party bno;
        private ShieldMetadata metadata;

        public updateMetadata(Party bno, ShieldMetadata metadata) {
            this.bno = bno;
            this.metadata = metadata;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new AmendMembershipMetadataFlow(bno, metadata));
            return signedTransaction;
        }
    }

    /**
     * Caller must be treasurer and returns true if node passed has chosen caller as treasurer
     */
    @StartableByRPC
    public static class imYourTreasurer extends FlowLogic<Boolean>{
        private Party node;

        public imYourTreasurer(Party node) {
            this.node = node;
        }

        @Override
        public Boolean call() throws FlowException {
            if (!subFlow(new isTreasure())) throw new FlowException("Only a valid active Treasurer organization can call this method.");
            Party treasurer = getOurIdentity();
            ShieldMetadata metadata = (ShieldMetadata) subFlow(new getMembership(node)).getMembershipMetadata();
            if (metadata.getTreasurers().contains(treasurer))
                return true;
            else
                return false;
        }
    }

    /**
     * Caller must be custodian and returns true if node passed has chosen caller as custodian
     */
    @StartableByRPC
    public static class imYourCustodian extends FlowLogic<Boolean>{
        private Party node;

        public imYourCustodian(Party node) {
            this.node = node;
        }

        @Override
        public Boolean call() throws FlowException {
            if (!subFlow(new isCustodian())) throw new FlowException("Only a valid active Custodian organization can call this method.");
            Party custodian = getOurIdentity();
            ShieldMetadata metadata = (ShieldMetadata) subFlow(new getMembership(node)).getMembershipMetadata();
            if (metadata.getCustodians().contains(custodian))
                return true;
            else
                return false;
        }
    }

}
