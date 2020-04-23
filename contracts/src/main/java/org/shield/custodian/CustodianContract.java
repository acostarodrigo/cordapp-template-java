package org.shield.custodian;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class CustodianContract implements Contract {
    public static final String ID = "org.shield.custodian.CustodianContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

    }

    public interface Commands extends CommandData {
        class notifyBond implements CustodianContract.Commands {}
        class notifyOffer implements CustodianContract.Commands {}
        class notifyTrade implements CustodianContract.Commands {}
        class notifyWalletTransaction implements CustodianContract.Commands {}
        class updateAll implements CustodianContract.Commands {}
    }
}
