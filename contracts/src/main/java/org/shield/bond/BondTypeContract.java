package org.shield.bond;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import static net.corda.core.contracts.ContractsDSL.requireThat;


public class BondTypeContract extends EvolvableTokenContract implements Contract {
    public static final String ID = "org.shield.bond.BondTypeContract";
    public BondTypeContract() {
        super();
    }

    @Override
    public void verify(@NotNull LedgerTransaction tx) {
        super.verify(tx);
    }

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx){
        BondState output = (BondState) tx.getOutput(0);
        Date now = new Date();
        requireThat(require -> {
            require.using("Deal size can't less than zero.", output.getDealSize() > 0);
            require.using("Digits supported is zero.", output.getFractionDigits() == 0);
            //require.using("Start date can't be in the past", output.getStartDate().after(now));
            return null;
        });
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        BondState input = (BondState) tx.getInput(0);
        BondState output = (BondState) tx.getOutput(0);

        // we make sure only some values change.
        requireThat(require -> {
            require.using("offering date can't change", input.getStartDate().equals(output.getStartDate()));
            require.using("digits can't change", input.getFractionDigits()==output.getFractionDigits());
            require.using("issuer can't change",input.getIssuer().equals(output.getIssuer()));
            return null;
        });

    }
}
