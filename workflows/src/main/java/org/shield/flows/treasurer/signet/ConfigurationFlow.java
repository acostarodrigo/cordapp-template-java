package org.shield.flows.treasurer.signet;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.cordapp.CordappConfig;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import org.shield.flows.membership.MembershipFlows;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to get configuration values from treasurer configuration file
 */
public class ConfigurationFlow {
    private ConfigurationFlow(){
        //no instantiation
    }

    /**
     * Gets the general API url.
     */
    @InitiatingFlow
    private static class GetAPIURL extends FlowLogic<String>{
        @Override
        @Suspendable
        public String call() throws FlowException {
            CordappConfig config = subFlow(new GetConfiguration());
            if (!config.exists("URL") || !config.exists("PORT")) throw new FlowException("Configuration file for treasurer is not correct. Missing URL and PORT keys.");

            // we generate the URL and return it
            String address = config.getString("URL");
            int port = config.getInt("PORT");
            String url = address.concat(":").concat(String.valueOf(port));
            url.concat("/mydomain/v1/");
            return url;
        }
    }

    @InitiatingFlow
    private static class GetConfiguration extends FlowLogic<CordappConfig>{
        @Override
        @Suspendable
        public CordappConfig call() throws FlowException {
            //we validate only the treasurer can get this configuration from the file
            if (!subFlow(new MembershipFlows.isTreasure())) throw new FlowException("Only a treasurer can get configuration file");

            // lets validate the needed keys are there
            CordappConfig config = getServiceHub().getAppContext().getConfig();
            return config;
        }
    }

    @InitiatingFlow
    public static class GetTransactionsURL extends FlowLogic<String>{
        @Override
        @Suspendable
        public String call() throws FlowException {
            String url = subFlow(new GetAPIURL());
            url.concat("transactions/");
            return url;
        }
    }

    @InitiatingFlow
    public static class GetLoginURL extends FlowLogic<String>{
        @Override
        @Suspendable
        public String call() throws FlowException {
            String url = subFlow(new GetAPIURL());
            url.concat("login/");
            return url;
        }
    }

    @InitiatingFlow
    public static class GetLoginHeader extends FlowLogic<Map<String,String>>{
        @Override
        @Suspendable
        public Map<String,String> call() throws FlowException {
            // we get the configuration file
            CordappConfig config = subFlow(new GetConfiguration());
            if (!config.exists("UUID") || !config.exists("SourceId") || !config.exists("user_token")) throw new FlowException("Configuration file for treasurer is not correct.");

            Map<String,String> loginParameters = new HashMap<>();
            loginParameters.put("UUID", config.getString("UUID"));
            loginParameters.put("SourceId", config.getString("SourceId"));
            loginParameters.put("user_token", config.getString("user_token"));
            return loginParameters;
        }
    }

    @InitiatingFlow
    public static class GetLoginBody extends FlowLogic<Map<String,String>>{
        @Override
        @Suspendable
        public Map<String,String> call() throws FlowException {
            // we get the configuration file
            CordappConfig config = subFlow(new GetConfiguration());
            if ( !config.exists("client_id") || !config.exists("client_secret") || !config.exists("audience")) throw new FlowException("Configuration file for treasurer is not correct.");

            Map<String,String> loginParameters = new HashMap<>();
            loginParameters.put("client_id", config.getString("client_id"));
            loginParameters.put("client_secret", config.getString("client_secret"));
            loginParameters.put("audience", config.getString("audience"));
            return loginParameters;
        }
    }
}
