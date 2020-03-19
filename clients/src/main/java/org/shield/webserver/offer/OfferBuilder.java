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
        UniqueIdentifier id = null;
        if (body.has("offerId")) {
            String offerId = body.get("offerId").asText();
            id = UniqueIdentifier.Companion.fromString(offerId);
        } else
            id = new UniqueIdentifier();

        // we get the issuer
        Party issuer = null;
        if (body.has("issuer")){
            String issuerString = body.get("issuer").asText();
            CordaX500Name issuerName = CordaX500Name.parse(issuerString);
            issuer = proxy.wellKnownPartyFromX500Name(issuerName);
        } else
            issuer = proxy.nodeInfo().getLegalIdentities().get(0);

        // we get the bond
        String bondId = "";
        if (body.has("bondId")){
            bondId = body.get("bondId").textValue();
        }
        if (body.has("bond")){
            JsonNode bondNode = body.get("bond");
            bondId = bondNode.get("id").textValue();
        }

        // we get the bond from the vault.
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
        long afsSize = body.get("afsSize").asLong();
        boolean afs = body.get("afs").asBoolean();
        OfferState offer = new OfferState(id,issuer,bond,ticker,offerPrice,offerYield,afsSize,afs,new Date());

        return offer;
    }
}
