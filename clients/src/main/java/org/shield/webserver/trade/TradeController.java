package org.shield.webserver.trade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    public ResponseEntity<List<String>> getTrades(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>(Arrays.asList("Unable to parse user to establish a connection."), HttpStatus.BAD_REQUEST);
        }

        List<String> trades = new ArrayList<>();
        for (StateAndRef<TradeState> stateAndRef : proxy.vaultQuery(TradeState.class).getStates()){
            trades.add(stateAndRef.getState().getData().toString());
        }
        return new ResponseEntity<>(trades, HttpStatus.OK);
    }

    @PostMapping("/issue")
    public ResponseEntity<String> issueTrade(@NotNull @RequestBody JsonNode body) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode tradeBody = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            tradeBody = body.get("trade");
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>("Unable to parse user or trade.", HttpStatus.BAD_REQUEST);
        }

        // we will generate the trade
        TradeBuilder tradeBuilder = new TradeBuilder(tradeBody, proxy);
        TradeState trade = tradeBuilder.getTrade();

        CordaFuture<SignedTransaction> cordaFuture = proxy.startFlowDynamic(TradeFlow.SendToBuyer.class, trade).getReturnValue();
        SignedTransaction signedTransaction = cordaFuture.get();
        return new ResponseEntity<>(signedTransaction.toString(), HttpStatus.OK);
    }

    @PostMapping("/accept")
    public ResponseEntity<String> acceptTrade(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        UniqueIdentifier tradeId = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            tradeId = objectMapper.readValue(body.get("tradeId").toString(),UniqueIdentifier.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>("Unable to parse user or tradeId.", HttpStatus.BAD_REQUEST);
        }

        CordaFuture<SignedTransaction> cordaFuture = proxy.startFlowDynamic(TradeFlow.Accept.class, tradeId).getReturnValue();
        SignedTransaction signedTransaction = cordaFuture.get();
        return new ResponseEntity<>(signedTransaction.toString(), HttpStatus.OK);
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelTrade(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        UniqueIdentifier tradeId = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            tradeId = objectMapper.readValue(body.get("tradeId").toString(),UniqueIdentifier.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>("Unable to parse user or tradeId.", HttpStatus.BAD_REQUEST);
        }

        CordaFuture<Void> cordaFuture = proxy.startFlowDynamic(TradeFlow.Cancel.class, tradeId).getReturnValue();
        cordaFuture.get();
        return new ResponseEntity<>(tradeId.toString(), HttpStatus.OK);
    }

    @PostMapping("/settle")
    public ResponseEntity<String> settleTrade(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        UniqueIdentifier tradeId = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            tradeId = objectMapper.readValue(body.get("tradeId").toString(),UniqueIdentifier.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>("Unable to parse user or tradeId.", HttpStatus.BAD_REQUEST);
        }

        CordaFuture<SignedTransaction> cordaFuture = proxy.startFlowDynamic(TradeFlow.Settle.class, tradeId).getReturnValue();
        SignedTransaction signedTransaction = cordaFuture.get();
        return new ResponseEntity<>(signedTransaction.toString(), HttpStatus.OK);
    }

}
