package org.shield.webserver.custodian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import org.jetbrains.annotations.NotNull;
import org.shield.custodian.CustodianState;
import org.shield.offer.OfferState;
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
}
