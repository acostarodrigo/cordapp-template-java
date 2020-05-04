package org.shield.webserver.custodian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kotlin.Pair;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.custodian.CustodianState;
import org.shield.fiat.FiatTransaction;
import org.shield.trade.State;
import org.shield.trade.TradeState;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.shield.webserver.response.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

import static org.shield.webserver.response.Response.getConnectionErrorResponse;
import static org.shield.webserver.response.Response.getValidResponse;

@RestController
@RequestMapping("/custodian")
@Component
public class CustodianController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }

    @GetMapping
    public ResponseEntity<Response> getMyCustodian(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray jsonArray = new JsonArray();
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQuery(CustodianState.class).getStates()){
            CustodianState custodianState = stateAndRef.getState().getData();
            jsonArray.add(custodianState.toJson());
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("custodian", jsonArray);
        return getValidResponse(jsonObject);
    }
    @GetMapping("/settlementBlotter")
    public ResponseEntity<Response> getSettlementBlotter(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray jsonArray = new JsonArray();
        // for each custodian state, we get the list of trades, only settled and pending ones
        Set<TradeState> tradeStateSet = new HashSet<>();
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQuery(CustodianState.class).getStates()){
            CustodianState custodianState = stateAndRef.getState().getData();
            if (custodianState.getTrades() != null){
                for (TradeState tradeState : custodianState.getTrades()){
                    final UniqueIdentifier tradeId = tradeState.getId();
                    tradeStateSet.removeIf(new Predicate<TradeState>() {
                        @Override
                        public boolean test(TradeState tradeState) {
                            if (tradeState.getId().equals(tradeId))
                                return true;
                            else
                                return false;
                        }
                    });
                    // we are only showing pending and settled trades.
                    if (tradeState.getState().equals(State.PENDING) || tradeState.getState().equals(State.SETTLED))
                        tradeStateSet.add(tradeState);
                }
            }

        }

        if (tradeStateSet.size() > 0) {
            for (TradeState tradeState : tradeStateSet) {
                // we add them to the json result
                jsonArray.add(tradeState.toJson());
            }
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("settlementBlotter", jsonArray);
        return getValidResponse(jsonObject);
    }

    @GetMapping("/settlementBlotterDetail")
    public ResponseEntity<Response> getSettlementBlotterDetail(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        String tradeId = "";
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            tradeId = body.get("tradeId").textValue();
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        // in search of an specific trade
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        List<TradeState> tradeStateList = new ArrayList<>();
        // this might return too many results, we will add paging.
        PageSpecification pageSpecification = new PageSpecification(1,20000);
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQueryByWithPagingSpec(CustodianState.class, criteria, pageSpecification).getStates()){
            CustodianState custodianState = stateAndRef.getState().getData();
            if (custodianState.getTrades() != null){
                for (TradeState trade : custodianState.getTrades()){
                    if (trade.getId().getId().toString().equals(tradeId)){
                        tradeStateList.add(trade);
                    }
                }
            }
        }

        List<TradeState> uniqueTrades = new ArrayList<>(new HashSet<>(tradeStateList));
        DetailedTradeBlotter detailedTradeBlotter = new DetailedTradeBlotter();
        for (TradeState tradeState : uniqueTrades){
            detailedTradeBlotter.setTrade(tradeState);
            detailedTradeBlotter.addState(tradeState.getState(), tradeState.getStateUpdate());
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("detail", detailedTradeBlotter.toJson());
        return getValidResponse(jsonObject);
    }

    @GetMapping("/masterFile")
    public ResponseEntity<Response> getMasterSecurityHolderFile(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray jsonArray = new JsonArray();
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQuery(CustodianState.class).getStates()){
            CustodianState custodianState = stateAndRef.getState().getData();
            if (custodianState.getBonds() != null) {
                for (BondState bondState : custodianState.getBonds()){
                    String bondId = bondState.getId();
                    Party issuer = bondState.getIssuer();

                    Map<Party, Pair<Long, Date>> aggregatedTraders = new HashMap<>();
                    if (custodianState.getTrades() != null){
                        for (TradeState tradeState : custodianState.getTrades()){
                            if (tradeState.getState().equals(State.PENDING) || tradeState.getState().equals(State.SETTLED)){
                                if (tradeState.getOffer().getBond().getId().equals(bondId)){
                                    if (aggregatedTraders.containsKey(tradeState.getBuyer())){
                                        // we have a buyer already, so lets sum the size
                                        long currentSize = aggregatedTraders.get(tradeState.getBuyer()).getFirst();
                                        aggregatedTraders.replace(tradeState.getBuyer(),new Pair<>(tradeState.getSize() + currentSize, tradeState.getTradeDate()));
                                    } else {
                                        aggregatedTraders.put(tradeState.getBuyer(), new Pair<>(tradeState.getSize(), tradeState.getTradeDate()));
                                    }
                                    if (aggregatedTraders.containsKey(tradeState.getSeller())){
                                        // we have a seller already, so lets substract the size
                                        long currentSize = aggregatedTraders.get(tradeState.getBuyer()).getFirst();
                                        aggregatedTraders.replace(tradeState.getBuyer(),new Pair<>(tradeState.getSize() - currentSize, tradeState.getTradeDate()));
                                    }
                                }
                            }
                        }
                    }

                    // at this point we have the bond, issuer and a map with all traders
                    for (Map.Entry<Party, Pair<Long, Date>> entry : aggregatedTraders.entrySet()){
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.addProperty("bond", bondId);
                            jsonObject.addProperty("issue", bondState.getTicker());
                            jsonObject.addProperty("issuer", issuer.getName().toString());
                            jsonObject.addProperty("holder", entry.getKey().getName().toString());
                            jsonObject.addProperty("size", entry.getValue().getFirst());
                            jsonObject.addProperty("lastTradeDate", entry.getValue().getSecond().toString());
                            jsonObject.addProperty("currency", bondState.getDenomination().getCurrencyCode());

                            jsonArray.add(jsonObject);
                    }

                }
            }
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("book", jsonArray);
        return getValidResponse(jsonObject);
    }


    @GetMapping("/investorList")
    public ResponseEntity<Response> getInvestorList(@NotNull @RequestBody JsonNode body){
        return getMasterSecurityHolderFile(body);
    }


    @GetMapping("/controlBookBonds")
    public ResponseEntity<Response> getControlBookBonds(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        JsonArray result = new JsonArray();
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQueryByCriteria(criteria, CustodianState.class).getStates()){
            CustodianState custodianState = stateAndRef.getState().getData();
            if (custodianState.getBonds() != null){

                JsonObject issuerJson = new JsonObject();
                issuerJson.addProperty("issuer",  custodianState.getIssuer().getName().getOrganisation().toString());
                JsonArray issuerBonds = new JsonArray();
                for (BondState bondState : custodianState.getBonds()){
                    JsonObject bondJson = new JsonObject();
                    bondJson.addProperty("id", bondState.getId());
                    bondJson.addProperty("ticker", bondState.getTicker());

                    issuerBonds.add(bondJson);
                }
                issuerJson.add("bonds", issuerBonds);
                result.add(issuerJson);
            }

        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("controlBook", result);
        return getValidResponse(jsonObject);
    }

    @GetMapping("/controlBookAttributes")
    public ResponseEntity<Response> getControlBookAttributes(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        String bondId = "";
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            bondId = body.get("bondId").asText();
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        JsonObject result = new JsonObject();
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQueryByCriteria(criteria, CustodianState.class).getStates()){
            CustodianState custodianState = stateAndRef.getState().getData();
            if (custodianState.getBonds() != null){
                for (BondState bondState : custodianState.getBonds()){
                    if (bondState.getId().equals(bondId)){
                        result = bondState.toJson();
                        break;
                    }
                }
            }

        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("attributes", result);
        return getValidResponse(jsonObject);
    }

    @GetMapping("/obligations")
    public ResponseEntity<Response> getObligations(@NotNull @RequestBody JsonNode body) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(), User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray transactions = new JsonArray();
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQueryByCriteria(criteria, CustodianState.class).getStates()){
            CustodianState custodianState = stateAndRef.getState().getData();
            for (TradeState tradeState : custodianState.getTrades()){
                if (tradeState.getState().equals(State.PENDING)){
                    JsonObject upcomingJson = new JsonObject();
                    upcomingJson.addProperty("tradeId", tradeState.getId().getId().toString());
                    upcomingJson.addProperty("buyer", tradeState.getBuyer().getName().toString());
                    upcomingJson.addProperty("seller", tradeState.getSeller().getName().toString());
                    upcomingJson.addProperty("description", tradeState.getOffer().getBond().getTicker());
                    upcomingJson.addProperty("arranger", tradeState.getArranger());
                    upcomingJson.addProperty("amount", tradeState.getSize());
                    upcomingJson.addProperty("currency", tradeState.getCurrency().getCurrencyCode());

                    // if the obligation doesn't exists, we add it.
                    if (!transactions.contains(upcomingJson))
                        transactions.add(upcomingJson);
                }
            }

        }

        JsonObject response = new JsonObject();
        response.add("obligations", transactions);
        return getValidResponse(response);

    }

    @GetMapping("/transactions")
    public ResponseEntity<Response> getTransactions(@NotNull @RequestBody JsonNode body) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(), User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray transactions = new JsonArray();
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQueryByCriteria(criteria, CustodianState.class).getStates()){
            CustodianState custodianState = stateAndRef.getState().getData();
            if (custodianState.getFiatState() != null){
                for (FiatTransaction fiatTransaction : custodianState.getFiatState().getFiatTransactionList()){
                    JsonObject tx = fiatTransaction.toJson();
                    tx.addProperty("issuer", custodianState.getFiatState().getIssuer().getName().toString());
                    transactions.add(tx);
                }
            }
        }
        JsonObject response = new JsonObject();
        response.add("transactions", transactions);
        return getValidResponse(response);

    }
}
