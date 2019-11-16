package org.shield.webserver.connection;

public class Connection {
    private User user;
    private static ProxyEntry proxyEntry;

    public Connection(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public ProxyEntry login(){
        // we get an available opened connection for this user
        proxyEntry = ConnectionPool.getProxyEntry(user.getUsername());

        // no connection was found, we need to open one.
        if (proxyEntry == null) {
            NodeRPCConnection nodeRPCConnection = new NodeRPCConnection(user.getUsername(), user.getPassword());
            nodeRPCConnection.initialiseNodeRPCConnection();
            proxyEntry = new ProxyEntry(nodeRPCConnection.getRpcConnection());

            // we store the connection in the connection pool.
            ConnectionPool.putProxyEntry(proxyEntry, user.getUsername());
        }
        return proxyEntry;
    }

    /**
     * Static method with no password. it is assumed that user has already logged before and has an open connection.
     * @param userName
     * @return
     */
    public static ProxyEntry login(String userName){
        return proxyEntry = ConnectionPool.getProxyEntry(userName);
    }
}
