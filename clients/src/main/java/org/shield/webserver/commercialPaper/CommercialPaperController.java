package org.shield.webserver.commercialPaper;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.sdk.token.contracts.types.TokenType;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import org.shield.token.CommercialPaperTokenType;
import org.shield.webserver.arrangement.RequestWrapper;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/paper") // The paths for HTTP requests are relative to this base path.
public class CommercialPaperController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }

    @GetMapping(value = "")
    public ResponseEntity<List<ResponseWrapper>> getCommercialPaperToken(@Valid @RequestBody User user) throws ExecutionException, InterruptedException {
        // we connect to the passed node
        generateConnection(user);

        List<ResponseWrapper> result = new ArrayList<>();
        for (StateAndRef<CommercialPaperTokenType> stateAndRef : proxy.vaultQuery(CommercialPaperTokenType.class).getStates()){
            CommercialPaperTokenType commercialPaperTokenType = stateAndRef.getState().getData();
            ResponseWrapper responseWrapper = new ResponseWrapper(commercialPaperTokenType.getIssuer().getName().toString(), commercialPaperTokenType.getValuation());
            for (StateAndRef<FungibleToken> tokenStateAndRef : proxy.vaultQuery(FungibleToken.class).getStates()){
                responseWrapper.setTokenQuantity(tokenStateAndRef.getState().getData().getAmount().getQuantity());
            }
            result.add(responseWrapper);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
