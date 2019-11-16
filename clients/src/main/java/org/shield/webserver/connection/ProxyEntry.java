package org.shield.webserver.connection;


import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;

import java.util.Date;

public class ProxyEntry {
    private final CordaRPCOps proxy;
    private final CordaRPCConnection connection;
    private Date lastUsed = new Date();

    public ProxyEntry(CordaRPCConnection connection) {
        this.connection = connection;
        this.proxy = this.connection.getProxy();
    }

    public CordaRPCOps getProxy() {
        this.lastUsed = new Date();
        return proxy;
    }

    public CordaRPCConnection getConnection() {
        return connection;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    @Override
    public String toString() {
        return "ProxyEntry{" +
            "proxy=" + proxy +
            ", connection=" + connection +
            ", lastUsed=" + lastUsed +
            '}';
    }
}
