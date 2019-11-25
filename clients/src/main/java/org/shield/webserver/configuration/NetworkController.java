package org.shield.webserver.configuration;

import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.NodeInfo;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/network") // The paths for HTTP requests are relative to this base path.
public class NetworkController {
    // todo this values must come from configuration file
    private final String serverName = "23.99.231.7:10003";
    private final String userName = "rpcuser";
    private final String password = "ChangeTh1sPa$$w0rd";

    @GetMapping(value = "")
    public ResponseEntity<ResponseWrapper> getNetworkConfiguration(){
        User admin = new User(serverName, userName, password);
        Connection connection = new Connection(admin);
        ProxyEntry proxyEntry = connection.login();
        CordaRPCOps proxy = proxyEntry.getProxy();

        ResponseWrapper responseWrapper = new ResponseWrapper();


        responseWrapper.setNotary(proxy.getNetworkParameters().getNotaries().get(0).toString());
        for (NodeInfo nodeInfo : proxy.networkMapSnapshot()){
            responseWrapper.addNode(nodeInfo.toString());
        }

        return new ResponseEntity<>(responseWrapper, HttpStatus.OK);
    }
}
