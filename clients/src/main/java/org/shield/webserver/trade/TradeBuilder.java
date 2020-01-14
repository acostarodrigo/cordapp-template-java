package org.shield.webserver.trade;

import com.fasterxml.jackson.databind.JsonNode;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import org.shield.bond.BondState;
import org.shield.offer.OfferState;
import org.shield.trade.State;
import org.shield.trade.TradeState;

import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;

public class TradeBuilder {
    private JsonNode body;
    private CordaRPCOps proxy;

    public TradeBuilder(JsonNode body, CordaRPCOps proxy) {
        this.body = body;
        this.proxy = proxy;
    }

    public TradeState getTrade() throws Exception {
        UniqueIdentifier id = new UniqueIdentifier();
        UniqueIdentifier offerId = UniqueIdentifier.Companion.fromString(body.get("offerId").asText());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date tradeDate = dateFormat.parse(body.get("tradeDate").asText());
        Date settleDate = dateFormat.parse(body.get("settleDate").asText());
        String buyerString = body.get("buyer").textValue();
        String sellerString = body.get("seller").textValue();
        float price = body.get("price").floatValue();
        float yield = body.get("yield").floatValue();
        long size = body.get("size").asLong();
        long proceeds = body.get("proceeds").asLong();
        Currency currency = Currency.getInstance(body.get("currency").textValue());
        State state = State.PROPOSED;

        // we get the offer
        OfferState offer = null;
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        for (StateAndRef<OfferState> stateAndRef : proxy.vaultQueryByCriteria(criteria,OfferState.class).getStates()){
            if (stateAndRef.getState().getData().getOfferId().equals(offerId)){
                offer = stateAndRef.getState().getData();
                break;
            }
        }

        if (offer == null) throw new Exception(String.format("Provided offerId %s does not exists.", offerId.toString()));

        // we get the buyer
        CordaX500Name buyerName = CordaX500Name.parse(buyerString);
        Party buyer = proxy.wellKnownPartyFromX500Name(buyerName);

        // we get the seller
        CordaX500Name sellerName = CordaX500Name.parse(sellerString);
        Party seller = proxy.wellKnownPartyFromX500Name(sellerName);



        // we generate the trade
        TradeState trade = new TradeState(id,offer, tradeDate,settleDate,buyer,seller, price,yield,size,proceeds, currency,state);
        return trade;
    }

}
