package org.shield.flows.treasurer;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import shadow.okhttp3.*;

import java.io.IOException;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class SignetAPI {
    private OkHttpClient client;
    private Request request;
    private Request.Builder requestBuilder;
    private String accessToken;


    // constructor used by corda flows. We get this values from config file
    public SignetAPI(String url, Map<String,String> header){
        this.client = new OkHttpClient();

        // we will generate a basic request.
        requestBuilder = new Request.Builder()
            .url(url);

        // and add the header
        for (Map.Entry<String,String> entry  : header.entrySet()){
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Logs in into Signet and retrieves the provided token
     * @return a string representing the token to use on future calls.
     * @throws IOException
     */
    public String login(Map<String,String> loginParameters) throws IOException, ParseException {
        // Lets create the body of the request.
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : loginParameters.entrySet()){
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
        return accessToken;
    }


    public void setAccessToken(String accessToken){
        this.accessToken = accessToken;
    }

    public List<String> getTranstionHistory (Date start, Date end) throws IOException {
        if (this.accessToken.isEmpty()) throw new IllegalArgumentException("Access token is missing");
//        httpPost.addHeader("access_token", this.accessToken);

        // define the json body
        // and execute

        return null;
    }

}
