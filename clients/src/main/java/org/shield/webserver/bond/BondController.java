package org.shield.webserver.bond;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.flows.bond.BondFlow;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.shield.webserver.response.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.shield.webserver.response.Response.getConnectionErrorResponse;
import static org.shield.webserver.response.Response.getValidResponse;

@RestController
@RequestMapping("/bond") // The paths for HTTP requests are relative to this base path.
public class BondController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @PostMapping
    public ResponseEntity<Response> issueBond (@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException, TimeoutException {
        ObjectMapper objectMapper = new ObjectMapper();
        User user = null;
        BondState bond = null;
        try {
            user = objectMapper.readValue(body.get("user").toString(),User.class);
            bond = objectMapper.readValue(body.get("bond").toString(),BondState.class);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        // we initiaite the connection
        generateConnection(user);

        // manually defined ids of bond.
        bond.setId(new UniqueIdentifier());
        bond.setLinearId(new UniqueIdentifier());

        // make the call to the node
        CordaFuture<UniqueIdentifier> cordaFuture = proxy.startFlowDynamic(BondFlow.Issue.class, bond).getReturnValue();
        UniqueIdentifier uniqueIdentifier = cordaFuture.get();

        // we set the issuer to provide the response
        Party caller = proxy.nodeInfo().getLegalIdentities().get(0);
        bond.setIssuer(caller);

        // we prepare the response
        return getValidResponse(bond.toJson());
    }


    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }

    @GetMapping
    public ResponseEntity<Response> getBonds(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray response = new JsonArray();
        for (StateAndRef<BondState> stateAndRef : proxy.vaultQuery(BondState.class).getStates()){
            BondState bond = stateAndRef.getState().getData();
            response.add(bond.toJson());
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("bonds", response);
        return getValidResponse(jsonObject);
    }
}
