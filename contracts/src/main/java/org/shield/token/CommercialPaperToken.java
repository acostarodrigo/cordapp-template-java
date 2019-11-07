package org.shield.token;

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@BelongsToContract(CommercialPaperTokenContract.class)
public class CommercialPaperToken extends EvolvableTokenType {
    private int fractionDigits;
    private List<Party> maintainers;
    private UniqueIdentifier linearId;
    private Amount<FiatCurrency> value;

    public CommercialPaperToken(int fractionDigits, List<Party> maintainers, UniqueIdentifier linearId, Amount<FiatCurrency> value) {
        this.fractionDigits = fractionDigits;
        this.maintainers = maintainers;
        this.linearId = linearId;
        this.value = value;
    }

    @Override
    public int getFractionDigits() {
        return fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return maintainers;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}
