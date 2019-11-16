package org.shield.webserver.connection;

import com.template.Client;
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
        ConnectionRecycler connectionRecycler = new ConnectionRecycler(period, threshold);
        logger.debug("Initializing Connection Pool recycler. Runs every " + period + " ms and kills connection over " + threshold + " ms.");
        new Thread(connectionRecycler).start();

    }

    /**
     * returns the {@link ProxyEntry} which contains the proxy that connects to the corda node
     * @param key user name used as key
     * @return {@link ProxyEntry}
     */
    public static ProxyEntry getProxyEntry (String key){
        if (queue.get(key)!=null) queue.get(key).setLastUsed(new Date()); // we define a new used date every time it is requested.
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

    /**
     * runs on its own thread. Closes open connections that are inactive
     */
    private class ConnectionRecycler implements Runnable{
        private boolean running = true;
        private final int period;
        private final int threshold;

        public ConnectionRecycler(int period, int threshold) {
            this.period = period;
            this.threshold = threshold;
        }

        public void stop(){
            this.running = false;
            logger.debug("Stopping connection pool recycler.");
        }

        @Override
        public void run() {
            while (running){
                try {
                    logger.debug("Connection pool recycler reviewing " + queue.size() + " connections.");
                    Date now = new Date();
                    for (Map.Entry<String, ProxyEntry> proxyEntry : ConnectionPool.queue.entrySet()){
                        long diff = now.getTime() - proxyEntry.getValue().getLastUsed().getTime();

                        if (diff > threshold){
                            logger.debug("Connection pool recycler found an idle connection. Closing for user " + proxyEntry.getKey());
                            ConnectionPool.queue.remove(proxyEntry.getKey());
                            proxyEntry.getValue().getConnection().notifyServerAndClose();
                            logger.debug("connection closed.");
                        }
                    }

                    Thread.sleep(period);
                } catch (InterruptedException e) {
                    logger.error("Connection pool recycler error. " + e);
                }
            }
        }
    }
}
