package org.shield.webserver.balance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;
import org.shield.flows.treasurer.USDFiatTokenFlow;
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
@RequestMapping("/balance")
public class BalanceController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @GetMapping
    public ResponseEntity<List<String>> getBalances(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        List<String> tokens = new ArrayList<>();
        for (StateAndRef<FungibleToken> stateAndRef : proxy.vaultQuery(FungibleToken.class).getStates()){
            FungibleToken token = stateAndRef.getState().getData();
            tokens.add(token.toString());
        }

        return new ResponseEntity<>(tokens,HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<String> issueUSD(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        String owner;
        long amount;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            owner = body.get("owner").textValue();
            amount = body.get("amount").asLong();
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>("Unable to parse user to establish a connection.", HttpStatus.BAD_REQUEST);
        }

        CordaX500Name ownerName = CordaX500Name.parse(owner);
        CordaFuture<SignedTransaction> cordaFuture = proxy.startFlowDynamic(USDFiatTokenFlow.Issue.class, proxy.wellKnownPartyFromX500Name(ownerName), amount).getReturnValue();
        SignedTransaction signedTransaction = cordaFuture.get();
        return new ResponseEntity<>("Issued " + amount + " at tx " + signedTransaction.toString(), HttpStatus.OK);
    }

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }
}
