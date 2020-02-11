package org.shield.webserver.trade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;
import org.shield.flows.trade.TradeFlow;
import org.shield.trade.TradeState;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.shield.webserver.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.shield.webserver.response.Response.getConnectionErrorResponse;
import static org.shield.webserver.response.Response.getValidResponse;

@RestController
@RequestMapping("/trade")
public class TradeController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }

    @GetMapping
    public ResponseEntity<Response> getTrades(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray trades = new JsonArray();
        for (StateAndRef<TradeState> stateAndRef : proxy.vaultQuery(TradeState.class).getStates()){
            TradeState trade = stateAndRef.getState().getData();
            trades.add(trade.toJson());
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("trades", trades);
        return getValidResponse(jsonObject);
    }

    @PostMapping("/issue")
    public ResponseEntity<Response> issueTrade(@NotNull @RequestBody JsonNode body) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode tradeBody = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            tradeBody = body.get("trade");
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        // we will generate the trade
        TradeBuilder tradeBuilder = new TradeBuilder(tradeBody, proxy);
        TradeState trade = tradeBuilder.getTrade();

        CordaFuture<UniqueIdentifier> cordaFuture = proxy.startFlowDynamic(TradeFlow.Create.class, trade).getReturnValue();
        UniqueIdentifier id = cordaFuture.get();
        trade.setId(id);

        return getValidResponse(trade.toJson());
    }

    @PostMapping("/accept")
    public ResponseEntity<Response> acceptTrade(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        UniqueIdentifier tradeId = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            tradeId = UniqueIdentifier.Companion.fromString(body.get("tradeId").asText());
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        CordaFuture<SignedTransaction> cordaFuture = proxy.startFlowDynamic(TradeFlow.Accept.class, tradeId).getReturnValue();
        SignedTransaction signedTransaction = cordaFuture.get();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("transaction", signedTransaction.getId().toString());
        return getValidResponse(jsonObject);
    }

    @PostMapping("/cancel")
    public ResponseEntity<Response> cancelTrade(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        UniqueIdentifier tradeId = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            tradeId = objectMapper.readValue(body.get("tradeId").toString(),UniqueIdentifier.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        CordaFuture<Void> cordaFuture = proxy.startFlowDynamic(TradeFlow.Cancel.class, tradeId).getReturnValue();
        cordaFuture.get();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", tradeId.getId().toString());
        return getValidResponse(jsonObject);
    }

    @PostMapping("/settle")
    public ResponseEntity<Response> settleTrade(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        UniqueIdentifier tradeId = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            tradeId = UniqueIdentifier.Companion.fromString(body.get("tradeId").asText());
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        CordaFuture<SignedTransaction> cordaFuture = proxy.startFlowDynamic(TradeFlow.Settle.class, tradeId).getReturnValue();
        SignedTransaction signedTransaction = cordaFuture.get();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("transaction", signedTransaction.getId().toString());
        return getValidResponse(jsonObject);
    }

}
