package org.shield.token;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;


public class CommercialPaperTokenContract extends EvolvableTokenContract implements Contract {
    public CommercialPaperTokenContract() {
        super();
    }

    @Override
    public void verify(@NotNull LedgerTransaction tx) {
        super.verify(tx);
    }

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {

    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {

    }
}
