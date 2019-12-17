package org.shield.flows.treasurer;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.math.BigDecimal;

public class SignetFlow {
    private SignetFlow(){
        // we don't allow instantiation
    }

    @InitiatingFlow
    public static class GetAccountBalance extends FlowLogic<Amount<FiatCurrency>>{
        private String accountId;

        public GetAccountBalance(String accountId) {
            this.accountId = accountId;
        }

        @Override
        @Suspendable
        public Amount<FiatCurrency> call() throws FlowException {
            FiatCurrency usd = new FiatCurrency();
            Amount<FiatCurrency> amount = new Amount<>(1000L, BigDecimal.ONE, usd);
            return amount;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class TransferToOmnibus extends FlowLogic<SignedTransaction>{
        private Party from;
        private long amount;

        public TransferToOmnibus(Party from, long amount) {
            this.from = from;
            this.amount = amount;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // TODO add validation to transfer fiat  money to omnibus account
            // issue USD tokens
            return subFlow(new USDFiatTokenFlow.Issue(from, amount));
        }
    }


}
