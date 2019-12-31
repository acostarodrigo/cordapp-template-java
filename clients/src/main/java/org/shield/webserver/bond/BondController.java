package org.shield.webserver.bond;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/bond")
public class BondController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @PostMapping("/issue")
    public ResponseEntity<String> issueBond (@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        User user = objectMapper.readValue(body.get("user").toString(),User.class);
        BondState bond = objectMapper.readValue(body.get("bond").toString(),BondState.class);

        generateConnection(user);
        UniqueIdentifier uniqueIdentifier = proxy.startFlowDynamic(BondFlow.Issue.class, bond).getReturnValue().get();
        return new ResponseEntity<>(uniqueIdentifier.toString(), HttpStatus.OK);
    }

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }
}
