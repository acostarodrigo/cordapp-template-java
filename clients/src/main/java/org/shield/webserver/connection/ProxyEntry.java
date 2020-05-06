package org.shield.webserver.connection;


import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;

import java.util.Date;

public class ProxyEntry {
    private final CordaRPCOps proxy;
    private final CordaRPCConnection connection;


    public ProxyEntry(CordaRPCConnection connection) {
        this.connection = connection;
        this.proxy = this.connection.getProxy();
    }

    public CordaRPCOps getProxy() {
        return proxy;
    }

    public CordaRPCConnection getConnection() {
        return connection;
    }


    @Override
    public String toString() {
        return "ProxyEntry{" +
            "proxy=" + proxy +
            ", connection=" + connection  +
            '}';
    }
}
