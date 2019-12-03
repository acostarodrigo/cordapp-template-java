package org.shield.flows.membership;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.businessnetworks.membership.flows.member.GetMembershipsFlow;
import com.r3.businessnetworks.membership.states.MembershipState;
import com.sun.tools.javac.comp.Flow;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.cordapp.CordappConfig;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import org.shield.flows.arrangement.ArrangementFlow;
import org.shield.membership.ShieldMetadata;

/**
 * Deals with all role and membership validations
 */
public class MembershipFlows {
    /**
     * Validates if the caller is a member of an Issuer organization.
     * Will return true if:
     *  is an active member
     *  is a OrgType.BOND_PARTICIPANT
     *  is a BondRole.ISSUER
     */
    public static class isIssuer extends FlowLogic<Boolean>{
        @Override
        @Suspendable
        public Boolean call() throws FlowException {

            MembershipState membershipState = subFlow(new getMembership());

            // we will validate is an active member of the organization
            if (membershipState == null || !membershipState.isActive()) return false;

            // node must be bond participant and bond issuer
            ShieldMetadata metadata = (ShieldMetadata) membershipState.getMembershipMetadata();
            if (metadata.getOrgTypes().contains(ShieldMetadata.OrgType.BOND_PARTICIPANT) && metadata.getBondRoles().contains(ShieldMetadata.BondRole.ISSUER)) return true;

            return false;
        }
    }

    /**
     * Retrieves the membership state from the BNO node.
     * Will fail if caller is not a member
     */
    private static class getMembership extends FlowLogic<MembershipState<? extends Object>>{
        @Override
        public MembershipState<? extends Object> call() throws FlowException {
            Party caller = getOurIdentity();

            Party bno = subFlow(new getBNO());

            GetMembershipsFlow getMembershipsFlow = new GetMembershipsFlow(bno, false, false);
            StateAndRef<? extends MembershipState<? extends Object>> stateAndRef = subFlow(getMembershipsFlow).get(caller);

            return stateAndRef.getState().getData();
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

            String bnoString = config.getString("bno");
            if (bnoString.isEmpty()) throw new FlowException("Configuration file doesn't include BNO data.");

            CordaX500Name bnoName = CordaX500Name.parse(bnoString);
            Party bno = getServiceHub().getNetworkMapCache().getPeerByLegalName(bnoName);
            return bno;
        }
    }
}
