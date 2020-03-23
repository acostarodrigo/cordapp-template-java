package org.shield.webserver.balance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import io.swagger.v3.oas.annotations.Operation;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.shield.flows.treasurer.USDFiatTokenFlow;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.shield.webserver.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.shield.webserver.response.Response.*;

@RestController
@RequestMapping("/balance")
public class BalanceController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @GetMapping
    @Operation(summary = "Gets balances of all digital wallets.", description = "Use this method to get balances in Fiat Token and Bonds.")
    public ResponseEntity<Response> getBalances(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray tokens = new JsonArray();

        // we get unconsumed tokens from vault
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        for (StateAndRef<FungibleToken> stateAndRef : proxy.vaultQueryByCriteria(criteria, FungibleToken.class).getStates()){
            FungibleToken token = stateAndRef.getState().getData();
            // we generate the response on JSON
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("issuer", token.getIssuer().getName().toString());
            jsonObject.addProperty("tokenIdentifier",token.getTokenType().getTokenIdentifier());
            jsonObject.addProperty("quantity",token.getAmount().getQuantity());
            jsonObject.addProperty("holder", token.getHolder().nameOrNull().toString());
            jsonObject.addProperty("name", token.getTokenType().getTokenClass().getCanonicalName());

            tokens.add(jsonObject);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("tokens", tokens);
        return getValidResponse(jsonObject);
    }

    @PostMapping
    public ResponseEntity<Response> issueUSD(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        String owner;
        long amount;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            owner = body.get("owner").textValue();
            amount = body.get("amount").asLong();
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        CordaX500Name ownerName = CordaX500Name.parse(owner);
        Party ownerNode = proxy.wellKnownPartyFromX500Name(ownerName);
        CordaFuture<SignedTransaction> cordaFuture = proxy.startFlowDynamic(USDFiatTokenFlow.Issue.class, ownerNode, amount).getReturnValue();
        SignedTransaction signedTransaction = cordaFuture.get();

        JsonObject response = new JsonObject();
        response.addProperty("tx", signedTransaction.getId().toString());
        return getResponse(true, response);
    }

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }
}
