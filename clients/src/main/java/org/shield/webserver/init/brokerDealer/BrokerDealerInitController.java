package org.shield.webserver.init.brokerDealer;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import org.shield.flows.init.BrokerDealerInitFlow;
import org.shield.states.BrokerDealerInitState;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/brokerDealerConf") // The paths for HTTP requests are relative to this base path.
public class BrokerDealerInitController {
    private static final Logger logger = LoggerFactory.getLogger(BrokerDealerInitController.class);

    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @GetMapping(value = "")
    public ResponseEntity<ResponseWrapper> getBrokerDealerInit(@Valid @RequestBody User user){
        logger.debug("Getting broker dealer  init for " + user.toString());
        generateConnection(user);
        QueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        List<StateAndRef<BrokerDealerInitState>> result = proxy.vaultQueryByCriteria(queryCriteria, BrokerDealerInitState.class).getStates();
        if (result.isEmpty()) return null;
        BrokerDealerInitState brokerDealerInitState = result.get(0).getState().getData();
        return new ResponseEntity<>(new ResponseWrapper(brokerDealerInitState), HttpStatus.OK);
    }



    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<ResponseWrapper> submitBrokerDealerInit(@Valid @RequestBody RequestWrapper body) throws ExecutionException, InterruptedException {
        User user = body.getUser();
        CordaX500Name issuerName = CordaX500Name.parse(body.getIssuer());

        generateConnection(body.getUser());
        Party issuer = proxy.wellKnownPartyFromX500Name(issuerName);
        List<Party> issuers = new ArrayList<>();
        issuers.add(issuer);

        proxy.startFlowDynamic(BrokerDealerInitFlow.Update.class, issuers).getReturnValue().get();
        return getBrokerDealerInit(body.getUser());
    }

    private void generateConnection(User user){
        if (proxy == null) {
            connection = new Connection(user);
            proxyEntry = connection.login();
            proxy = proxyEntry.getProxy();
        }
    }
}
