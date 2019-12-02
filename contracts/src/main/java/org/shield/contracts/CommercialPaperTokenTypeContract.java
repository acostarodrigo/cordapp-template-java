package org.shield.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;
import org.shield.states.CommercialPaperTokenState;

import java.util.Date;

import static net.corda.core.contracts.ContractsDSL.requireThat;


public class CommercialPaperTokenTypeContract extends EvolvableTokenContract implements Contract {
    public static final String ID = "org.shield.contracts.CommercialPaperTokenTypeContract";
    public CommercialPaperTokenTypeContract() {
        super();
    }

    @Override
    public void verify(@NotNull LedgerTransaction tx) {
        super.verify(tx);
    }

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx){
        CommercialPaperTokenState output = (CommercialPaperTokenState) tx.getOutput(0);
        Date now = new Date();
        requireThat(require -> {
            require.using("Valuation must be zero during creation.", output.getValuation() == 0);
            require.using("Digits supported is zero.", output.getFractionDigits() == 0);
            require.using("Offering date can't be in the past", output.getofferingDate().after(now));
            return null;
        });
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        CommercialPaperTokenState input = (CommercialPaperTokenState) tx.getInput(0);
        CommercialPaperTokenState output = (CommercialPaperTokenState) tx.getOutput(0);

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
