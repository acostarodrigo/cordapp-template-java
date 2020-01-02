package org.shield.webserver.balance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import org.jetbrains.annotations.NotNull;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/balance")
public class BalanceController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @GetMapping("/")
    public ResponseEntity<List<String>> getBalances(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return new ResponseEntity<>(Arrays.asList("Unable to parse user to establish a connection."), HttpStatus.BAD_REQUEST);
        }

        return null;

    }

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }
}
