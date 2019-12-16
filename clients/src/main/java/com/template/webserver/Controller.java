package com.template.webserver;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import org.shield.flows.bond.BondFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @GetMapping(value = "/templateendpoint", produces = "text/plain")
    private String templateendpoint() throws ExecutionException, InterruptedException {
        Date offeringDate = new Date(2020,12,12);
        long fungibleAmount = 100;
        Party holder = proxy.networkMapSnapshot().get(1).getLegalIdentities().get(0);

        UniqueIdentifier id = proxy.startFlowDynamic(BondFlow.IssueFungibleToken.class,offeringDate,fungibleAmount,holder).getReturnValue().get();
        return id.toString();
    }
}
