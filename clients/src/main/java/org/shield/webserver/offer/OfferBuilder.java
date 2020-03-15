package org.shield.webserver.offer;

import com.fasterxml.jackson.databind.JsonNode;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import org.shield.bond.BondState;
import org.shield.offer.OfferState;

import java.util.Date;

public class OfferBuilder {
    private CordaRPCOps proxy;
    private JsonNode body;

    public OfferBuilder(CordaRPCOps proxy, JsonNode body) {
        this.proxy = proxy;
        this.body = body;
    }

    public OfferState getOffer(){
        // we create the id, or get it if provided
        String offerId = body.get("offerId").textValue();
        UniqueIdentifier id = null;
        if (offerId == null || offerId == "")
             id = new UniqueIdentifier();
        else
             id = UniqueIdentifier.Companion.fromString(offerId);

        // we get the issuer
        Party issuer = proxy.nodeInfo().getLegalIdentities().get(0);

        // we get the bond
        String bondId = body.get("bondId").textValue();
        BondState bond = null;
        for (StateAndRef<BondState> stateAndRef : proxy.vaultQuery(BondState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(bondId)){
                bond = stateAndRef.getState().getData();
                break;
            }
        }


        // we get the rest
        String ticker = bond.getIssuerTicker();
        float offerPrice = body.get("offerPrice").floatValue();
        float offerYield = body.get("offerYield").floatValue();
        long aggregatedTradeSize = body.get("aggregatedTradeSize").asLong();
        long afsSize = body.get("afsSize").asLong();
        boolean afs = body.get("afs").asBoolean();
        OfferState offer = new OfferState(id,issuer,bond,ticker,offerPrice,offerYield,aggregatedTradeSize,afsSize,afs,new Date());

        return offer;
    }
}
