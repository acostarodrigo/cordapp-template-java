package org.shield.custodian;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.fiat.FiatState;
import org.shield.fiat.FiatTransaction;
import org.shield.offer.OfferState;
import org.shield.trade.TradeState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@BelongsToContract(CustodianContract.class)
@CordaSerializable
public class CustodianState implements Serializable,ContractState {
    private List<Party> custodians;
    private Party issuer;
    private List<BondState> bonds;
    private List<TradeState> trades;
    private List<OfferState> offers;
    private FiatState fiatState;
    private Date lastUpdated;

    @ConstructorForDeserialization
    public CustodianState() {
    }

    public CustodianState(Party issuer, List<Party> custodians) {
        this.custodians = custodians;
        this.issuer = issuer;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> participants = new ArrayList<>(custodians);
        participants.add(issuer);
        return participants;
    }

    public List<Party> getCustodians() {
        return custodians;
    }

    public void setCustodians(List<Party> custodians) {
        this.custodians = custodians;
    }

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public List<BondState> getBonds() {
        return bonds;
    }

    public void setBonds(List<BondState> bonds) {
        this.bonds = bonds;
    }

    public List<TradeState> getTrades() {
        return trades;
    }

    public void setTrades(List<TradeState> trades) {
        this.trades = trades;
    }

    public List<OfferState> getOffers() {
        return offers;
    }

    public void setOffers(List<OfferState> offers) {
        this.offers = offers;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public FiatState getFiatState() {
        return fiatState;
    }

    public void setFiatState(FiatState fiatState) {
        this.fiatState = fiatState;
    }

    public JsonObject toJson(){
        JsonObject result = new JsonObject();
        result.addProperty("issuer", issuer.getName().toString());

        // we are adding the bonds
        if (getBonds() != null) {
            JsonArray bonds = new JsonArray();
            for (BondState bond : getBonds()){
                bonds.add(bond.toJson());
            }
            result.add("bonds", bonds);
        }


        // we are adding the trades
        if (getTrades() != null) {
            JsonArray trades = new JsonArray();
            for (TradeState trade : getTrades()){
                trades.add(trade.toJson());
            }
            result.add("trades", trades);
        }

        // we are adding the offers
        if (getOffers() != null){
            JsonArray offers = new JsonArray();
            for (OfferState offer : getOffers()){
                offers.add(offer.toJson());
            }
            result.add("offers", offers);
        }

        // we are adding the fiat transactions
        if (getFiatState() != null){
            JsonArray fiatTransactions = new JsonArray();
            for (FiatTransaction transaction : fiatState.getFiatTransactionList()){
                fiatTransactions.add(transaction.toJson());
            }
            result.add("fiats", fiatTransactions);
        }

        // all ready to return JSON
        return result;
    }
}
