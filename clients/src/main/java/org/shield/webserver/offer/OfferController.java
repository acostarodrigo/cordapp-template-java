package org.shield.webserver.offer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.flows.offer.OfferFlow;
import org.shield.offer.OfferState;
import org.shield.webserver.Starter;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rx.Observable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@RestController
@RequestMapping("/offer")
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
    public ResponseEntity<List<OfferState>> getMyOffers(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        List<OfferState> offers = new ArrayList<>();
        for (StateAndRef<OfferState> stateAndRef : proxy.vaultQuery(OfferState.class).getStates()){
            OfferState offer = stateAndRef.getState().getData();
            // if the node we are connected issued the offer, then we add it
            if (proxy.nodeInfo().getLegalIdentities().contains(offer.getIssuer()))
                offers.add(offer);
        }
        return new ResponseEntity<>(offers, HttpStatus.OK);

    }

    @GetMapping("/bondMonitor")
    public ResponseEntity<List<BondMonitor>> getBondMonitor(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        List<BondMonitor> offers = new ArrayList<>();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        Party myNode = proxy.nodeInfo().getLegalIdentities().get(0);
        // we get the data feed from the vault.
        DataFeed<Vault.Page<OfferState>, Vault.Update<OfferState>> dataFeed = proxy.vaultTrackByCriteria(OfferState.class, criteria);

        for (StateAndRef<OfferState> stateAndRef : dataFeed.getSnapshot().getStates()){
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

                offers.add(bondMonitor);
            }
        }

        Observable<Vault.Update<OfferState>> offerUpdates = dataFeed.getUpdates();
        offerUpdates.toBlocking().subscribe(offerStateUpdate -> offerStateUpdate.getProduced().forEach(new Consumer<StateAndRef<OfferState>>() {
            @Override
            public void accept(StateAndRef<OfferState> offerStateStateAndRef) {
                System.out.println("Rodrigo:" + offerStateStateAndRef.getState().getData().toString());
                logger.info("{}", offerStateStateAndRef.getState().getData());
            }
        }));

        return new ResponseEntity<>(offers, HttpStatus.OK);

    }



    @PostMapping()
    public ResponseEntity<UniqueIdentifier> createOffer(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);

            OfferBuilder offerBuilder = new OfferBuilder(proxy, body.get("offer"));
            OfferState offer = offerBuilder.getOffer();
            CordaFuture cordaFuture = proxy.startFlowDynamic(OfferFlow.Create.class,offer).getReturnValue();
            UniqueIdentifier uniqueIdentifier = (UniqueIdentifier) cordaFuture.get();
            return new ResponseEntity<>(uniqueIdentifier, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

}
