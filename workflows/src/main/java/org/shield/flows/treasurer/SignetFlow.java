package org.shield.flows.treasurer;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import org.json.simple.parser.ParseException;
import org.shield.flows.membership.MembershipFlows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class SignetFlow {
    private static SignetAPI signetAPI;
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
            if (!subFlow(new MembershipFlows.isTreasure())) throw new FlowException("Only an active treasurer organization can log in into Signature Bank");

            String url = subFlow(new ConfigurationFlow.GetLoginURL());

            // we set the header for the login request
            Map<String,String> header = subFlow(new ConfigurationFlow.GetLoginHeader());
            Map<String,String> loginParameters = subFlow(new ConfigurationFlow.GetLoginBody());

            signetAPI = new SignetAPI(url, header);
            String accessToken = null;
            try {
                accessToken = signetAPI.login(loginParameters);
            } catch (IOException | ParseException e) {
                throw new FlowException(e);
            }
            return accessToken;
        }
    }

    @InitiatingFlow
    public static class GetTransactionHistory extends FlowLogic<List<String>>{
        private Date startDate;
        private Date endDate;

        /**
         * defines ther star and end date of the transactions history
         * @param startDate
         * @param endDate
         */
        public GetTransactionHistory(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @Override
        @Suspendable
        public List<String> call() throws FlowException {
            String access_token = subFlow(new Login());
            signetAPI.setAccessToken(access_token);
            List<String> result = new ArrayList<>();
            try {
                result = signetAPI.getTranstionHistory(startDate, endDate);
            } catch (IOException e) {
                throw new FlowException(e);
            }
            return result;
        }
    }

    @InitiatingFlow
    public static class DepositUSD extends FlowLogic<Void>{
        @Override
        @Suspendable
        public Void call() throws FlowException {
            return null;
        }
    }
}
