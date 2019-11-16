package org.shield.webserver.connection;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeRPCConnection {
    private static final Logger logger = LoggerFactory.getLogger(NodeRPCConnection.class);

    private final String host = "localhost";
    private final int port = 10009;
    private final String username;
    private final String password;
    private CordaRPCConnection rpcConnection;
    private CordaRPCOps proxy;

    public NodeRPCConnection(String username, String password) {
        this.username = username;
        this.password = password;
    }


    public void initialiseNodeRPCConnection() {
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, port);
        CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
        logger.debug("Connecting to node " + host + ":" + port + " with user " + username);
        rpcConnection = rpcClient.start(username, password);
        logger.debug("connected to node.");
        proxy = rpcConnection.getProxy();
    }

    public CordaRPCConnection getRpcConnection() {
        return rpcConnection;
    }

    public CordaRPCOps getProxy() {
        return proxy;
    }
}
