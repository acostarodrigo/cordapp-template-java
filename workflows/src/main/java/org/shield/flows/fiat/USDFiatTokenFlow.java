package org.shield.flows.fiat;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.businessnetworks.membership.flows.member.GetMembershipsFlow;
import com.r3.businessnetworks.membership.states.MembershipState;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.money.DigitalCurrency;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlowHandler;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.shield.membership.ShieldMetadata;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;


public class USDFiatTokenFlow {

    @StartableByRPC
    @InitiatingFlow
    public static class Issue extends FlowLogic<SignedTransaction>{
        private Party owner;
        private long amount;

        public Issue(Party owner, long amount) {
            this.owner = owner;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            Party treasurer = getOurIdentity();


            // todo this needs to be retrieved from conf
            String bnoString = "O=BNO,L=New York,C=US";
            CordaX500Name bnoName = CordaX500Name.parse(bnoString);
            Party bno = getServiceHub().getNetworkMapCache().getPeerByLegalName(bnoName);
            GetMembershipsFlow getMembershipsFlow = new GetMembershipsFlow(bno, false, true);
            StateAndRef<? extends MembershipState<? extends Object>> membershipState = subFlow(getMembershipsFlow).get(treasurer);

            // we will validate is an active member of the organization
            if (membershipState == null || !membershipState.getState().getData().isActive()){
                throw new FlowException("This action is only allowed to Shidl business network owners.");
            }

            // node must be bond issuer to continue
            ShieldMetadata metadata = (ShieldMetadata) membershipState.getState().getData().getMembershipMetadata();
            if (!metadata.getOrgTypes().contains(ShieldMetadata.OrgType.NETWORK_TREASURER)){
                throw new FlowException("Only a treasurer can issue fiat currency tokens.");
            }

            TokenType usd = FiatCurrency.Companion.getInstance("USD");
            IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), usd);
            Amount<IssuedTokenType> usdAmount = new Amount<>(amount, issuedTokenType);

            FlowSession ownerSession = initiateFlow(owner);

            FungibleToken fungibleToken = new FungibleToken(usdAmount, owner, TransactionUtilitiesKt.getAttachmentIdForGenericParam(usd) );
            SignedTransaction signedTransaction = subFlow(new IssueTokensFlow(fungibleToken, Arrays.asList(ownerSession)));

            subFlow(new FinalityFlow(signedTransaction, ownerSession));
            return signedTransaction;
        }
    }

    @InitiatedBy(Issue.class)
    public static class IssueResponder extends FlowLogic<Void>{
        private FlowSession flowSession;

        public IssueResponder(FlowSession flowSession) {
            this.flowSession = flowSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new IssueTokensFlowHandler(flowSession));
            subFlow(new ReceiveFinalityFlow(flowSession));
            return null;
        }
    }








}
