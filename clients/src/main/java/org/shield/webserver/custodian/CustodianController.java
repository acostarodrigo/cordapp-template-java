package org.shield.webserver.custodian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import org.jetbrains.annotations.NotNull;
import org.shield.custodian.CustodianState;
import org.shield.offer.OfferState;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        // for each custodian state, we get the list of trades
        Set<TradeState> tradeStateSet = new HashSet<>();
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQuery(CustodianState.class).getStates()){
            CustodianState custodianState = stateAndRef.getState().getData();
            if (custodianState.getTrades() != null){
                Set<TradeState> noDuplicates = new HashSet<>(custodianState.getTrades());
                tradeStateSet.addAll(noDuplicates);
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

        // todo this is not good performance. We loop each custodianState, consumed or not
        // in search of an specific trade
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        List<TradeState> tradeStateList = new ArrayList<>();
        for (StateAndRef<CustodianState> stateAndRef : proxy.vaultQueryByCriteria(criteria, CustodianState.class).getStates()){
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
}
