package org.shield.webserver.bond;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.messaging.CordaRPCOps;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.flows.bond.BondFlow;
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
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/bond") // The paths for HTTP requests are relative to this base path.
public class BondController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @PostMapping
    public ResponseEntity<String> issueBond (@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException, TimeoutException {
        ObjectMapper objectMapper = new ObjectMapper();
        User user = null;
        BondState bond = null;
        try {
            user = objectMapper.readValue(body.get("user").toString(),User.class);
            bond = objectMapper.readValue(body.get("bond").toString(),BondState.class);
        } catch (IOException e) {
            return new ResponseEntity<>("Incorrect parameters. Can't parse into user and bond object.", HttpStatus.BAD_REQUEST);
        }

        // we initiaite the connection
        generateConnection(user);

        // manually defined ids of bond.
        bond.setId(new UniqueIdentifier());
        bond.setLinearId(new UniqueIdentifier());

        // make the call to the node
        CordaFuture<UniqueIdentifier> cordaFuture = proxy.startFlowDynamic(BondFlow.Issue.class, bond).getReturnValue();
        UniqueIdentifier uniqueIdentifier = cordaFuture.get();
        return new ResponseEntity<>(uniqueIdentifier.toString(), HttpStatus.OK);
    }


    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }

    @GetMapping
    public ResponseEntity<List<String>> getBonds(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>(Arrays.asList("Unable to parse user to stablish connection."), HttpStatus.BAD_REQUEST);
        }

        List<String> bondStates = new ArrayList<>();
        for (StateAndRef<BondState> stateAndRef : proxy.vaultQuery(BondState.class).getStates()){
            bondStates.add(stateAndRef.getState().getData().toString());
        }
        return new ResponseEntity<>(bondStates, HttpStatus.OK);
    }
}
