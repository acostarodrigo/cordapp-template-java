package org.shield.webserver.init.issuer;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import org.shield.flows.init.BrokerDealerInitFlow;
import org.shield.flows.init.IssuerInitFlow;
import org.shield.states.BrokerDealerInitState;
import org.shield.states.IssuerInitState;
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
@RequestMapping("/issuerConf") // The paths for HTTP requests are relative to this base path.
public class IssuerInitController {
    private static final Logger logger = LoggerFactory.getLogger(IssuerInitController.class);

    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @GetMapping(value = "")
    public ResponseEntity<ResponseWrapper> getIssuerInit(@Valid @RequestBody User user){
        logger.debug("Getting issuer  init for " + user.toString());
        generateConnection(user);
        QueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        List<StateAndRef<IssuerInitState>> result = proxy.vaultQueryByCriteria(queryCriteria, IssuerInitState.class).getStates();

        if (result.isEmpty()) return null;
        IssuerInitState issuerInitState = result.get(0).getState().getData();
        return new ResponseEntity<>(new ResponseWrapper(issuerInitState), HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<ResponseWrapper> submitIssuerInit(@Valid @RequestBody RequestWrapper body) throws ExecutionException, InterruptedException {
        User user = body.getUser();
        CordaX500Name brokerDealerName = CordaX500Name.parse(body.getBrokerDealer());

        // stablish connection
        generateConnection(user);

        // lets search for the state, to determine if we issue or update
        QueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<IssuerInitState>> result = proxy.vaultQueryByCriteria(queryCriteria, IssuerInitState.class).getStates();

        // we create the party from the string
        Party brokerDealer = proxy.wellKnownPartyFromX500Name(brokerDealerName);
        List<Party> brokerDealers = new ArrayList<>();
        brokerDealers.add(brokerDealer);

        if (result.isEmpty())
            proxy.startFlowDynamic(IssuerInitFlow.Issue.class, brokerDealers).getReturnValue().get();
        else
            proxy.startFlowDynamic(IssuerInitFlow.Update.class, brokerDealers).getReturnValue().get();

        return getIssuerInit(user);
    }

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }
}
