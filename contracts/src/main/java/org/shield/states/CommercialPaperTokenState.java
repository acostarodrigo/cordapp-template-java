package org.shield.states;

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;
import org.shield.contracts.CommercialPaperTokenTypeContract;

import java.util.Date;
import java.util.List;

@BelongsToContract(CommercialPaperTokenTypeContract.class)
public class CommercialPaperTokenState extends EvolvableTokenType {
    private long valuation;
    private Party issuer;
    private Date offeringDate;
    private int fractionDigits;
    private List<Party> maintainers;
    private UniqueIdentifier linearId;


    public CommercialPaperTokenState(long valuation, Party issuer, Date offeringDate, int fractionDigits, List<Party> maintainers, UniqueIdentifier linearId) {
        this.valuation = valuation;
        this.issuer = issuer;
        this.offeringDate = offeringDate;
        this.fractionDigits = fractionDigits;
        this.maintainers = maintainers;
        this.linearId = linearId;
    }

    public long getValuation() {
        return valuation;
    }

    public void setValuation(long valuation) {
        this.valuation = valuation;
    }

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }


    public Date getofferingDate() {
        return offeringDate;
    }

    public void setofferingDate(Date offeringDate) {
        this.offeringDate = offeringDate;
    }

    public void setFractionDigits(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    public void setMaintainers(List<Party> maintainers) {
        this.maintainers = maintainers;
    }

    public void addMaintainer(Party maintainer){
        this.maintainers.add(maintainer);
    }

    public void removeMaintainer(Party maintainer){
        this.maintainers.remove(maintainer);
    }

    public void setLinearId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    @Override
    public int getFractionDigits() {
        return this.fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return this.maintainers;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.linearId;
    }
}
