package org.shield.webserver.bond;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.messaging.CordaRPCOps;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.bond.DealType;
import org.shield.flows.bond.BondFlow;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Controller
@RequestMapping("/bond")
public class BondController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @PostMapping("/")
    public ResponseEntity<String> issueBond (@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException, IOException, TimeoutException {
        ObjectMapper objectMapper = new ObjectMapper();
        User user = objectMapper.readValue(body.get("user").toString(),User.class);
        BondState bond = objectMapper.readValue(body.get("bond").toString(),BondState.class);
        generateConnection(user);
        bond.setId(new UniqueIdentifier());
        bond.setLinearId(new UniqueIdentifier());

        CordaFuture<UniqueIdentifier> cordaFuture = proxy.startFlowDynamic(BondFlow.Issue.class, bond).getReturnValue();
        UniqueIdentifier uniqueIdentifier = cordaFuture.get(10, TimeUnit.SECONDS);
        return new ResponseEntity<>(uniqueIdentifier.toString(), HttpStatus.OK);
    }

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }

    @GetMapping("/")
    public ResponseEntity<List<BondState>> getBonds(){

        return null;
    }
}
