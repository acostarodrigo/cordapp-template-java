package org.shield.flows.treasurer;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BufferedHeader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;


public class SignetFlow {
    private SignetFlow(){
        // we don't allow instantiation
    }

    /**
     * logs in into Signature Bank and retrieves the access token.
     */
    @InitiatingFlow
    private static class Login extends FlowLogic<String> {
        @Override
        @Suspendable
        public String call() throws FlowException {
            String url = subFlow(new ConfigurationFlow.GetLoginURL());

            // we set the header for the login request
            List<NameValuePair> header = subFlow(new ConfigurationFlow.GetLoginHeader());
            List<NameValuePair> loginParameters = subFlow(new ConfigurationFlow.GetLoginBody());

            SignetLogin signetLogin = new SignetLogin(url, header, loginParameters);
            String accessToken = null;
            try {
                accessToken = signetLogin.login();
            } catch (IOException e) {
                throw new FlowException(e);
            }
            return accessToken;
        }
    }
}
