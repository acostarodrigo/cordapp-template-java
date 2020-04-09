package org.shield.fiat;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;
import org.shield.trade.State;
import org.shield.trade.TradeContract;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class FiatContract implements Contract {
    public static final String ID = "org.shield.fiat.FiatContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final FiatContract.Commands command = tx.findCommand(FiatContract.Commands.class, cmd -> true).getValue();
        final List<PublicKey> signers = tx.findCommand(FiatContract.Commands.class, cmd -> true).getSigners();
        FiatState fiatState = tx.outputsOfType(FiatState.class).get(0);
        if (command instanceof FiatContract.Commands.newTransaction) {
            requireThat(require -> {
                // only 1 Fiat state is allowed
                require.using("Only one Fiat state output is allowed.", tx.outputsOfType(FiatState.class).size() == 1);
                // only 1 signer is required
                require.using("Only one signature is required.", signers.size() == 1);
                // issuer must be the signer
                require.using("Issuer of Fiat state must be the signer.", fiatState.getIssuer().getOwningKey().equals(signers.get(0)));
                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class newTransaction implements FiatContract.Commands {}
    }
}
