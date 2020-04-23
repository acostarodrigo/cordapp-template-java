package org.shield.flows.treasurer;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlowHandler;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.shield.fiat.FiatTransaction;
import org.shield.flows.custodian.CustodianFlows;
import org.shield.flows.fiat.FiatFlow;
import org.shield.flows.membership.MembershipFlows;

import java.time.Instant;
import java.util.Arrays;


public class USDFiatTokenFlow {

    private USDFiatTokenFlow(){
        // no instantiation allowed.
    }

    @InitiatingFlow
    @StartableByRPC
    /**
     * issues USD token. (Startable by RPC to avoid using signature Bank for every test.
     * TODO remove @StartableByRPC in production.)
     */
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
            if (!subFlow(new MembershipFlows.imYourTreasurer(owner))) throw new FlowException(String.format("We are not the treasurer of the node %s", owner.getName().toString()));

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
            SignedTransaction signedTransaction = subFlow(new ReceiveFinalityFlow(flowSession));

            // we will store this transaction into the FiatState
            Party me = getOurIdentity();
            for (FungibleToken fungibleToken : signedTransaction.getCoreTransaction().outputsOfType(FungibleToken.class)){
                if (fungibleToken.getHolder().equals(me)){
                    // for every output that is sending me money, we generate a new transaction
                    Amount currentBalance = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(),fungibleToken.getTokenType());
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Fiat token issued at ");
                    stringBuilder.append(Instant.now().toString());
                    stringBuilder.append(" by ");
                    stringBuilder.append(flowSession.getCounterparty().getName().getOrganisation());
                    FiatTransaction fiatTransaction = new FiatTransaction(Instant.now().getEpochSecond(),stringBuilder.toString(), FiatTransaction.Type.DEPOSIT, fungibleToken.getAmount(),currentBalance.getQuantity(), FiatTransaction.Action.IN);

                    subFlow(new FiatFlow.NewTransaction(fiatTransaction));

                    // we inform the custodians
                    subFlow(new CustodianFlows.SendFiatTransaction());
                }
            }

            return null;
        }
    }
}
