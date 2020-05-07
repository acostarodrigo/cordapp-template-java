package org.shield.webserver.offer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.flows.offer.OfferFlow;
import org.shield.offer.OfferState;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.shield.webserver.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import rx.Observable;
import rx.internal.reactivestreams.PublisherAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.shield.webserver.response.Response.*;

@RestController
@RequestMapping("/offer")
@Component
public class OfferController {
    private static final Logger logger = LoggerFactory.getLogger(OfferController.class);

    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }

    @GetMapping
    public ResponseEntity<Response> getMyOffers(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray offers = new JsonArray();
        for (StateAndRef<OfferState> stateAndRef : proxy.vaultQuery(OfferState.class).getStates()){
            OfferState offer = stateAndRef.getState().getData();
            // if the node we are connected issued the offer, then we add it
            if (proxy.nodeInfo().getLegalIdentities().contains(offer.getIssuer()))
                offers.add(offer.toJson());
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("offers", offers);
        return getValidResponse(jsonObject);

    }

    /**
     * Generates the Bond Monitor view, which is the list of all the AFS Offers.
     * @param body
     * @return
     */
    @GetMapping("/bondMonitor")
    public ResponseEntity<Response> getBondMonitor(@NotNull @RequestBody JsonNode body){
        // we parse the user from the body and establish the connection to the node
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        // we query the vault for Offers.
        JsonArray offers = new JsonArray();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        Party myNode = proxy.nodeInfo().getLegalIdentities().get(0);

        // we get the data feed from the vault.
        for (StateAndRef<OfferState> stateAndRef : proxy.vaultQueryByCriteria(criteria,OfferState.class).getStates()){
            OfferState offer = stateAndRef.getState().getData();
            // we are getting the list of offers that are not ours and are AFS
            if (!myNode.equals(offer.getIssuer()) && offer.isAfs()){
                BondState bond = offer.getBond();
                BondMonitor bondMonitor = new BondMonitor(offer.getOfferId(),bond.getId(),
                    bond.getIssuerTicker(),
                    offer.getOfferPrice(),
                    offer.getOfferYield(),
                    bond.getMaturityDate(),
                    bond.getCouponRate(),
                    bond.getCouponFrequency(),
                    "Vanila", // hardcoding for now
                    "Primary", // hardcoding for now
                    bond.getDealSize(),
                    bond.getDealType(),
                    bond.getDenomination(),
                    offer.getIssuer(),
                    offer.getIssuer().getName().getOrganisation(),
                    offer.getAfsSize());

                offers.add(bondMonitor.toJson());
            }
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("offers",offers);
        return getValidResponse(jsonObject);

    }

    /**
     * subcribe to updates on bondMonitor
     * @param body
     * @return
     */
    @PostMapping("/subscribeBondMonitor")
    public Observable<Vault.Update<OfferState>> subscribeBondMonitor(@NotNull @RequestBody JsonNode body){
        // we validate user sent in body and stablish connection
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            //
        }

        // lets create the data feed with all unconsumed offer states
        List<BondMonitor> offers = new ArrayList<>();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        DataFeed<Vault.Page<OfferState>, Vault.Update<OfferState>> dataFeed = proxy.vaultTrackByCriteria(OfferState.class, criteria);
        Observable<Vault.Update<OfferState>> offerUpdates = dataFeed.getUpdates();

        PublisherAdapter publisherAdapter = new PublisherAdapter(offerUpdates);


        //return Flux.from(publisherAdapter).toString();
        return offerUpdates;
    }

    @PostMapping("/modify")
    public ResponseEntity<Response> modifyOffer(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        String offerId = body.get("offerId").textValue();
        // lets search for the offer
        OfferState offer = null;
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        for (StateAndRef<OfferState> stateAndRef : proxy.vaultQueryByCriteria(criteria, OfferState.class).getStates()){
            if (stateAndRef.getState().getData().getOfferId().getId().toString().equals(offerId)){
                offer = stateAndRef.getState().getData();
                break;
            }
        }

        // can't find it. won't go on
        if (offer == null){
            getErrorResponse(String.format("Offer %s doesn't exists.", offerId), new Exception("Offer doesn't exists on this node."));
        }

        if (body.has("offerPrice"))
            offer.setOfferPrice(body.get("offerPrice").floatValue());

        if (body.has("offerYield"))
            offer.setOfferYield(body.get("offerYield").floatValue());

        if (body.has("afsSize"))
            offer.setAfsSize(body.get("afsSize").asInt());

        if (body.has("afs"))
            offer.setAfs(body.get("afs").asBoolean());

        CordaFuture cordaFuture = proxy.startFlowDynamic(OfferFlow.Modify.class,offer).getReturnValue();
        cordaFuture.get();

        return getValidResponse(offer.toJson());
    }



    @PostMapping("/afs")
    public ResponseEntity<Response> setAFS(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        UniqueIdentifier offerId = UniqueIdentifier.Companion.fromString(body.get("offerId").asText());
        boolean afs = body.get("afs").asBoolean();

        CordaFuture<SignedTransaction> cordaFuture = proxy.startFlowDynamic(OfferFlow.setAFS.class,offerId,afs).getReturnValue();
        SignedTransaction signedTransaction = cordaFuture.get();

        // prepare the response
        JsonObject response = new JsonObject();
        response.addProperty("transaction", signedTransaction.getId().toString());
        return getValidResponse(response);
    }

    @GetMapping("/myInventory")
    public ResponseEntity<Response> myInventory(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(), User.class);
            generateConnection(user);
            System.out.println("MyInventory - Connected with " + body.toString());
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        try{
            JsonArray inventory = new JsonArray();
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            List<StateAndRef<OfferState>> result = proxy.vaultQueryByCriteria(criteria, OfferState.class).getStates();
            System.out.println("MyInventory: got offer result: " + result.size());
            for (StateAndRef<OfferState> stateAndRef : result) {
                OfferState offer = stateAndRef.getState().getData();
                System.out.println("|--MyInventory: got offer with bond " + offer.getBond().getId());
                long aggregatedTradeSize = 0L;
                // if the node we are connected issued the offer, then we create the inventory
                if (proxy.nodeInfo().getLegalIdentities().contains(offer.getIssuer())) {
                    // all data of My inventory comes from the offer, except the aggregated trade size,
                    // which we obtain from the balance.
                    for (StateAndRef<FungibleToken> tokenStateAndRef : proxy.vaultQueryByCriteria(criteria, FungibleToken.class).getStates()) {
                        FungibleToken token = tokenStateAndRef.getState().getData();
                        String tokenIdentifier = token.getTokenType().getTokenIdentifier();
                        if (tokenIdentifier.equals(offer.getBond().getLinearId().getId().toString())) {
                            aggregatedTradeSize = aggregatedTradeSize + token.getAmount().getQuantity();
                        }
                    }
                    MyInventory myInventory = new MyInventory(
                        offer.getBond().getId(),
                        offer.getTicker(),
                        offer.getOfferPrice(),
                        offer.getOfferYield(),
                        aggregatedTradeSize,
                        offer.getAfsSize(),
                        offer.getBond().getMaturityDate(),
                        offer.getBond().getBondType().toString(),
                        offer.getBond().getCouponRate(),
                        offer.getBond().getCouponFrequency(),
                        offer.getBond().getDealSize(),
                        offer.getBond().getDealType(),
                        offer.getBond().getDenomination(),
                        offer.isAfs(),
                        offer.getOfferId(),
                        offer.getBond().getLinearId(),
                        offer.getIssuer()
                    );
                    inventory.add(myInventory.toJson());
                }
            }
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("myInventory", inventory);

            // we close the connection
            proxyEntry.getConnection().close();

            System.out.println(String.format("MyInventory: response: %s", inventory.size()));
            return getValidResponse(jsonObject);
        }  catch (Exception e){
            System.out.println("Error getting MyInventory data. " + e.toString());
            return getErrorResponse("Error getting MyInventory data.", e);
        }

    }

}
