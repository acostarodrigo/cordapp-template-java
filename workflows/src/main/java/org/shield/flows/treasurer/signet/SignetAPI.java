package org.shield.flows.treasurer.signet;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import shadow.okhttp3.FormBody;
import shadow.okhttp3.OkHttpClient;
import shadow.okhttp3.Request;
import shadow.okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SignetAPI {
    private OkHttpClient client;
    private String url;
    private Request.Builder requestBuilder;
    private String accessToken;
    private Map<String,String> loginHeader;
    private Map<String,String> loginBody;

    // possible send status for transfer operations
    public enum SendStatus{PENDING, DONE};



    // constructor used by corda flows. We get this values from config file
    public SignetAPI(String url, Map<String,String> loginHeader, Map<String,String> loginBody){
        this.client = new OkHttpClient();
        this.url = url;
        this.loginHeader = loginHeader;
        this.loginBody = loginBody;
    }

    /**
     * Logs in into Signet and sets the accessToken
     * @return a string representing the token to use on future calls.
     * @throws IOException
     */
    private void login() throws IOException, ParseException {
        // we create the request with the login url
        requestBuilder = new Request.Builder()
            .url(url.concat("login"));

        // add the header
        for (Map.Entry<String,String> entry  : loginHeader.entrySet()){
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        // Lets create the body of the request.
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : loginBody.entrySet()){
            formBodyBuilder.add(entry.getKey(), entry.getValue());
        }
        FormBody body = formBodyBuilder.build();

        // we add the body to the request
        requestBuilder.post(body);
        Request request = this.requestBuilder.build();

        // execute the request and get the response
        Response response = client.newCall(request).execute();

        // we will parse the response and get the token
        JSONParser jsonParser = new JSONParser();
        JSONObject json  = (JSONObject) jsonParser.parse(response.body().string());

        this.accessToken = (String) json.get("AccessToken");
    }

    /**
     * parses current access token and verifies if it has expired already
     * @return true if expired (or close to expire)
     */
    public boolean isTokenExpired(){
        if (this.accessToken.isEmpty()) return true;

        // we will parse the token without signature
        int i = this.accessToken.lastIndexOf('.');
        String withoutSignature = accessToken.substring(0, i+1);
        try {
            Claims claims = (Claims) Jwts.parser().parse(withoutSignature).getBody();
            // and compare the date
            Date now = new Date();
            if (claims.getExpiration().before(now)) return true;
        } catch (ExpiredJwtException e){
            // token is expired
            return true;
        }
        // token is good to use
        return false;
    }


    /**
     * Gets all the wallets associated with the specified user.
     * @param userToken
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public List<String> getUserWallets(String userToken) throws IOException, ParseException {
        if (isTokenExpired()) login();
        loginHeader.put("access_token", this.accessToken);

        // we create the request with the clientData url
        requestBuilder = new Request.Builder()
            .url(url.concat("clientdata"));

        // we add the header with the specified userToken
        Map<String,String> header = loginHeader;
        header.replace("user_token", userToken);

        for (Map.Entry<String,String> entry : header.entrySet()){
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        // we add the body and build the request
        FormBody body = new FormBody.Builder()
            .add("Time", new Date().toString())
            .build();

        Request request = this.requestBuilder.build();

        // execute the request and get the response
        Response response = client.newCall(request).execute();

        // we will parse the response and get the token
        JSONParser jsonParser = new JSONParser();
        JSONObject json  = (JSONObject) jsonParser.parse(response.body().string());

        List<String> wallets = (List<String>) json.get("Wallets");
        return wallets;
    }

    public long getWalletBalance(String userToken, String walletaddress) throws IOException, ParseException {
        if (isTokenExpired()) login();
        loginHeader.put("access_token", this.accessToken);

        // we create the request with the balance url
        requestBuilder = new Request.Builder()
            .url(url.concat("balance"));

        // we add the header with the specified userToken
        Map<String,String> header = loginHeader;
        header.replace("user_token", userToken);

        for (Map.Entry<String,String> entry : header.entrySet()){
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        // we add the body and build the request
        FormBody body = new FormBody.Builder()
            .add("Time", new Date().toString())
            .add("Wallet", walletaddress)
            .build();

        Request request = this.requestBuilder.build();

        // execute the request and get the response
        Response response = client.newCall(request).execute();

        // we will parse the response and get the token
        JSONParser jsonParser = new JSONParser();
        JSONObject json  = (JSONObject) jsonParser.parse(response.body().string());
        long balance = Long.getLong(json.get("WalletAmount").toString());
        return balance;
    }

    public String transferSend(String fromWallet, String toWallet, long amount) throws IOException, ParseException {
        if (isTokenExpired()) login();
        loginHeader.put("access_token", this.accessToken);

        // we create the request with the balance url
        requestBuilder = new Request.Builder()
            .url(url.concat("send/request"));

        // we add the body and build the request
        FormBody body = new FormBody.Builder()
            .add("Time", new Date().toString())
            .add("SenderWallet", fromWallet)
            .add("RecipientWallet", toWallet)
            .add("Amount", String.valueOf(amount))
            .add("Description", "TBD")
            .build();

        Request request = this.requestBuilder.build();

        // execute the request and get the response
        Response response = client.newCall(request).execute();

        // we will parse the response and get the token
        JSONParser jsonParser = new JSONParser();
        JSONObject json  = (JSONObject) jsonParser.parse(response.body().string());
        String referenceId = (String) json.get("ReferenceId");
        return referenceId;

    }

    public SendStatus transferStatus(String referenceId) {
        return null;

    }
}
