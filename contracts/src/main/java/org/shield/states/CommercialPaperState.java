package org.shield.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;
import org.shield.contracts.CommercialPaperContract;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@BelongsToContract(CommercialPaperContract.class)
public class CommercialPaperState implements ContractState, Serializable {
    public static final String externalKey = "org.shield.commercialPaper";
    private UniqueIdentifier id;
    private Party issuer;
    private Date offeringDate;
    private int size;

    public CommercialPaperState(UniqueIdentifier id, Party issuer, int size, Date offeringDate) {
        this.id = id;
        this.issuer = issuer;
        this.size = size;
        this.offeringDate = offeringDate;
    }

    // for serialization
    public CommercialPaperState() {
    }

    public UniqueIdentifier getId() {
        return id;
    }

    public Party getIssuer() {
        return issuer;
    }


    public Date getOfferingDate() {
        return offeringDate;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(issuer);
    }
}
