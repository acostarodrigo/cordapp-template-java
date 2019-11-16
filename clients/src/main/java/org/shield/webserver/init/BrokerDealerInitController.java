package org.shield.webserver.init;

import net.corda.core.contracts.StateAndRef;
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
import org.springframework.web.bind.annotation.*;

import javax.persistence.Entity;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
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

    @GetMapping(value = "/get")
    public BrokerDealerInitState getBrokerDealerInit(@Valid @RequestBody User user){
        logger.debug("Getting broker dealer  init for " + user.toString());
        generateConnection(user);
        QueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        List<StateAndRef<BrokerDealerInitState>> result = proxy.vaultQueryByCriteria(queryCriteria, BrokerDealerInitState.class).getStates();
        if (result.isEmpty()) return null;
        BrokerDealerInitState brokerDealerInitState = result.get(0).getState().getData();
        return brokerDealerInitState;
    }

    @Entity
    private class InitState{
        @NotEmpty(message = "Please provide a valid username")
        private User user;
        @NotEmpty(message = "Please provide a valid issuer")
        private Party issuer;
    }

    @PostMapping(value = "/post")
    public BrokerDealerInitState submitBrokerDealerInit(@Valid @RequestBody User user) throws ExecutionException, InterruptedException {
        logger.debug("Submitting init " + user.toString() + " with issuer " );
        generateConnection(user);
        Party issuer = proxy.getNetworkParameters().getNotaries().get(0).component1();
        List<Party> issuers = new ArrayList<>();
        issuers.add(issuer);

        proxy.startFlowDynamic(BrokerDealerInitFlow.Issue.class, issuers).getReturnValue().get();
        return getBrokerDealerInit(user);
    }

    private void generateConnection(User user){
        if (proxy == null) {
            connection = new Connection(user);
            proxyEntry = connection.login();
            proxy = proxyEntry.getProxy();
        }
    }
}
