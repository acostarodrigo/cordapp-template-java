package org.shield.token;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;


public class CommercialPaperTokenTypeContract extends EvolvableTokenContract implements Contract {
    public static final String ID = "org.shield.token.CommercialPaperTokenTypeContract";
    public CommercialPaperTokenTypeContract() {
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
        CommercialPaperTokenType input = (CommercialPaperTokenType) tx.getInput(0);
        CommercialPaperTokenType output = (CommercialPaperTokenType) tx.getOutput(0);

        // we make sure only some values change. Only valuation and maintainers for now.
        requireThat(require -> {
            require.using("offering date can't change", input.getofferingDate().equals(output.getofferingDate()));
            require.using("digits can't change", input.getFractionDigits()==output.getFractionDigits());
            require.using("issuer can't change",input.getIssuer().equals(output.getIssuer()));
            require.using("Linear Id can't change", input.getLinearId().equals(output.getLinearId()));
                return null;
        });

    }
}
