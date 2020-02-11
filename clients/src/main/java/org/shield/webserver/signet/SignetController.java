package org.shield.webserver.signet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import org.jetbrains.annotations.NotNull;
import org.shield.flows.treasurer.signet.SignetFlow;
import org.shield.offer.OfferState;
import org.shield.signet.SignetAccountState;
import org.shield.signet.SignetIssueTransactionState;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.shield.webserver.offer.OfferController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.shield.webserver.response.Response.getConnectionErrorResponse;
import static org.shield.webserver.response.Response.getValidResponse;

@RestController
@RequestMapping("/treasurer")
public class SignetController {
    private static final Logger logger = LoggerFactory.getLogger(OfferController.class);

    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }

    @PostMapping("/signetDeposit")
    public ResponseEntity<String> TransferToEscrowAndIssue(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        SignetIssueTransactionState signetIssueTransactionState = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);

            SignetTransactionBuilder builder = new SignetTransactionBuilder(body.get("signet"),proxy);
            signetIssueTransactionState = builder.build();
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        CordaFuture cordaFuture = proxy.startFlowDynamic(SignetFlow.DepositToEscrowAndIssue.class, signetIssueTransactionState).getReturnValue();
        UUID id = (UUID) cordaFuture.get();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id.toString());
        return getValidResponse(jsonObject);
    }
}
