package org.shield.webserver.connection;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.client.rpc.GracefulReconnect;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class NodeRPCConnection {
    private static final Logger logger = LoggerFactory.getLogger(NodeRPCConnection.class);

    private final String serverName;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private CordaRPCConnection rpcConnection;
    private CordaRPCOps proxy;

    public NodeRPCConnection(String serverName, String username, String password) {
        this.serverName = serverName;
        this.host = serverName.split(":")[0];
        this.port = Integer.valueOf(serverName.split(":")[1]); //todo this needs to be validated
        this.username = username;
        this.password = password;
    }


    public void initialiseNodeRPCConnection() {
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, port);
        CordaRPCClientConfiguration configuration = new CordaRPCClientConfiguration(Duration.ofSeconds(30), 4, java.lang.Boolean.getBoolean("net.corda.client.rpc.trackRpcCallSites"), Duration.ofSeconds(1), 4, 1, Duration.ofSeconds(1), new Double(1), 5, 10485760, Duration.ofDays(1));
        CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress, configuration);

        logger.debug("Connecting to node " + host + ":" + port + " with user " + username);

        GracefulReconnect gracefulReconnect = new GracefulReconnect(this::onDisconnect, this::onReconnect, 3);
        rpcConnection = rpcClient.start(username, password, gracefulReconnect);
        logger.debug("connected to node.");
        proxy = rpcConnection.getProxy();
    }

    private void onDisconnect() {
        // Insert implementation
        logger.debug("On Disconnet triggered");
    }

    private void onReconnect() {
        // Insert implementation
        logger.debug("On reconnect triggered");
    }

    public CordaRPCConnection getRpcConnection() {
        return rpcConnection;
    }

    public CordaRPCOps getProxy() {
        return proxy;
    }
}
