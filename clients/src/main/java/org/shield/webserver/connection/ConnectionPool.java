package org.shield.webserver.connection;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores and keep track of open connections to corda nodes by user. Opened connections are reused.
 * Connection recycler sub class will close those connections that have been idle for some time.
 */
public class ConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    private final int period = 60 * 60 * 60; // time in seconds the pool will review old proxies to remove. By default every 60 seconds.
    private int threshold; // number of seconds we will allow proxies to remain at queue.

    private static Map<String, ProxyEntry> queue = new HashMap<>();

    /**
     * constructor.
     * @param threshold defines how many milliseconds connections are keep idle.
     */
    public ConnectionPool(int threshold) {
        this.threshold = threshold;
        logger.debug("Initializing Connection Pool recycler. Runs every " + period + " ms and kills connection over " + threshold + " ms.");


    }

    /**
     * returns the {@link ProxyEntry} which contains the proxy that connects to the corda node
     * @param key user name used as key
     * @return {@link ProxyEntry}
     */
    public static ProxyEntry getProxyEntry (String key){
        return queue.get(key);
    }

    /**
     * Whenever a new connection is open, it includes it in the pool.
     * @param proxyEntry
     * @param key
     */
    public static void putProxyEntry(ProxyEntry proxyEntry, String key){
        queue.put(key, proxyEntry);
    }

    public static int getProxyEntryCount(){
        return queue.size();
    }

}
