package org.shield.webserver.connection;

public class Connection {
    private User user;
    private final String key;
    private static ProxyEntry proxyEntry;

    public Connection(User user) {
        this.user = user;
        this.key = user.getUsername()+user.getServerName();
    }

    public User getUser() {
        return user;
    }

    public ProxyEntry login(){
        // we get an available opened connection for this user
        proxyEntry = ConnectionPool.getProxyEntry(key);

        // no connection was found, we need to open one.
        if (proxyEntry == null) {
            NodeRPCConnection nodeRPCConnection = new NodeRPCConnection(user.getServerName(),user.getUsername(), user.getPassword());
            nodeRPCConnection.initialiseNodeRPCConnection();
            proxyEntry = new ProxyEntry(nodeRPCConnection.getRpcConnection());

            // we store the connection in the connection pool.
            ConnectionPool.putProxyEntry(proxyEntry, key);
        }
        return proxyEntry;
    }


}
