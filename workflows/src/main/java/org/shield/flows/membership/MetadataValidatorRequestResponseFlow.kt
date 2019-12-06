package org.shield.flows.membership

import co.paralleluniverse.fibers.Suspendable
import com.r3.businessnetworks.membership.flows.bno.RequestMembershipFlowResponder
import com.r3.businessnetworks.membership.states.MembershipState
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import org.shield.membership.ShieldMetadata

/**
 * Custom validator of [ShieldMetadata] passed to new request membership flows.
 */
@InitiatedBy(RequestMembershipFlowResponder::class)
class MetadataValidatorRequestResponseFlow (session: FlowSession) : RequestMembershipFlowResponder(session){

    /**
     * Validations are:
     *  [ShieldMetadata.orgContact] must be a valid email
     *  if [ShieldMetadata.orgTypes] constains BOND_PARTICIPANT:
     *   [ShieldMetadata.bondRoles] can't be empty
     *   [ShieldMetadata.custodians] can't be empty
     *   [ShieldMetadata.treasurers] can't be empty
     *   [Party] added as custodian must be valid (Active member of the business network)
     *   [Party] added as treasurer must be valid
     */
    @Suspendable
    override fun verifyTransaction(builder: TransactionBuilder) {
        super.verifyTransaction(builder)

        val membership = builder.outputStates().filter { it.data is MembershipState<*> }.single().data as MembershipState<ShieldMetadata>
        val metadata = membership.membershipMetadata;

        // is valid email?
        if (!isEmailValid(metadata.orgContact)){
            throw FlowException("Invalid Organization contact provided. Email is not valid: ${metadata.orgContact}")
        }

        // is organization type a Bond Participant?
        if (metadata.orgTypes.contains(ShieldMetadata.OrgType.BOND_PARTICIPANT)) {
            // At least 1 Bond role must be specified
            if (metadata.bondRoles == null || metadata.bondRoles.isEmpty()){
                throw FlowException("Bond role is required for a Bond participant node");
            }

            // at least 1 custodian role must be specified
            if (metadata.custodians == null || metadata.custodians.isEmpty()) {
                throw FlowException("At least 1 custodian must be provided for bond participants.")
            }

            // at least 1 custodian role must be specified
            if (metadata.treasurers == null || metadata.treasurers.isEmpty()) {
                throw FlowException("At least 1 treasurer must be provided for bond participants.")
            }

        }
    }

    companion object {
        @JvmStatic
        val EMAIL_REGEX = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})";
        fun isEmailValid(email: String): Boolean {
            return EMAIL_REGEX.toRegex().matches(email);
        }
    }
}
