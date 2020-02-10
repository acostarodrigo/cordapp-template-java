package org.shield.webserver.response;

import com.google.gson.Gson;
import org.json.simple.JSONObject;

import java.io.Serializable;

public class Response implements Serializable {
    public static String getValidResponse(Object message){
        JSONObject response = new JSONObject();
        response.put("message", message);
        response.put("success", true);
        return response.toJSONString();
    }

    public static String getErrorResponse(Object message){
        JSONObject response = new JSONObject();
        response.put("message", message);
        response.put("success", false);
        return  response.toJSONString();
    }
}
