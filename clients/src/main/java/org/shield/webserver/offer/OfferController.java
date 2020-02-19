package org.shield.webserver.offer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import static org.shield.webserver.response.Response.getConnectionErrorResponse;
import static org.shield.webserver.response.Response.getValidResponse;

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
        DataFeed<Vault.Page<OfferState>, Vault.Update<OfferState>> dataFeed = proxy.vaultTrackByCriteria(OfferState.class, criteria);
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
                    "Zero", // hardcoding for now
                    "Zero", // hardcoding for now
                    "Vanila", // hardcoding for now
                    "Primary", // hardcoding for now
                    offer.getAfsSize(),
                    bond.getDealType(),
                    bond.getDenomination());

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



    @PostMapping()
    public ResponseEntity<Response> createOffer(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        OfferBuilder offerBuilder = new OfferBuilder(proxy, body.get("offer"));
        OfferState offer = offerBuilder.getOffer();
        CordaFuture cordaFuture = proxy.startFlowDynamic(OfferFlow.Create.class,offer).getReturnValue();
        cordaFuture.get();

        // we get the issuer to return the offer as json
        Party issuer = proxy.nodeInfo().getLegalIdentities().get(0);
        offer.setIssuer(issuer);

        return getValidResponse(offer.toJson());
    }
}
