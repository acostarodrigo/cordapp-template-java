package org.shield.webserver.signet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import org.jetbrains.annotations.NotNull;
import org.shield.flows.treasurer.signet.SignetFlow;
import org.shield.offer.OfferState;
import org.shield.signet.SignetAccountState;
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

@RestController
@RequestMapping("/signet")
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

    @PostMapping
    public ResponseEntity<String> TransferToEscrowAndIssue(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        SignetAccountState signetAccountState = null;
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            signetAccountState = objectMapper.readValue(body.get("signet").toString(),SignetAccountState.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        proxy.startFlowDynamic(SignetFlow.TransferToEscrowAndIssue.class, signetAccountState);

        return new ResponseEntity<>(signetAccountState.toString(), HttpStatus.OK);

    }
}
