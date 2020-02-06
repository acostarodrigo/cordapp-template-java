package org.shield.flows.treasurer;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlowHandler;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.shield.flows.membership.MembershipFlows;

import java.math.BigDecimal;
import java.util.Arrays;


public class USDFiatTokenFlow {

    private USDFiatTokenFlow(){
        // no instantiation allowed.
    }

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
            // only treasurer organizations can issue fiat tokens
            if (!subFlow(new MembershipFlows.isTreasure())) throw new FlowException("Only an active treasurer organization can issue fiat tokens");

            Party treasurer = getOurIdentity();

            TokenType usd = FiatCurrency.Companion.getInstance("USD");
            IssuedTokenType issuedTokenType = new IssuedTokenType(treasurer, usd);
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
