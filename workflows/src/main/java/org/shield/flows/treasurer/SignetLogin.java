package org.shield.flows.treasurer;

import net.corda.core.flows.FlowException;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BufferedHeader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class SignetLogin {
    private String url;
    private String uuid;
    private String sourceId;
    private String user_token;
    private String client_id;
    private String client_secret;
    private String audience;
    private HttpPost httpPost;
    private List<NameValuePair> loginParameters;
    private CloseableHttpClient httpClient;

    // I will use this constructor mainly for testing purposes to execute from whitelisted ip directly
    public SignetLogin(String url, String uuid, String sourceId, String user_token, String client_id, String client_secret, String audience) {
        this.url = url;
        this.uuid = uuid;
        this.sourceId = sourceId;
        this.user_token = user_token;
        this.client_id = client_id;
        this.client_secret = client_secret;
        this.audience = audience;

        httpPost = new HttpPost(url);

        // we create the request header
        httpPost.addHeader("UUID", uuid);
        httpPost.addHeader("SourceId", sourceId);
        httpPost.addHeader("user_token", user_token);
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

        // lets setup the body param
        loginParameters = new ArrayList<>();
        loginParameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
        loginParameters.add(new BasicNameValuePair("client_id", client_id));
        loginParameters.add(new BasicNameValuePair("client_secret", client_secret));
        loginParameters.add(new BasicNameValuePair("audience", audience));
    }

    public SignetLogin(String url, List<NameValuePair> header, List<NameValuePair> body){
        httpPost = new HttpPost(url);
        for (NameValuePair valuePair : header){
            httpPost.addHeader(valuePair.getName(), valuePair.getValue());
        }
        this.loginParameters = body;
    }

    public String login() throws IOException {
        httpPost.setEntity(new UrlEncodedFormEntity(loginParameters));
        httpClient = HttpClients.createDefault();
        String accessToken = "";

        CloseableHttpResponse response = httpClient.execute(httpPost);
        for (int loop = 0; loop < response.getAllHeaders().length; loop++) {
            BufferedHeader header = (BufferedHeader) response
                .getAllHeaders()[loop];
            if (header.getName().equals("AccessToken")) {
                accessToken = header.getValue();
                break;
            }
        }
        return accessToken;
    }
}
