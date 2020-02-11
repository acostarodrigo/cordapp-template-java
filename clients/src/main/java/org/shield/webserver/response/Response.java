package org.shield.webserver.response;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.Arrays;

public class Response implements Serializable {
    public static ResponseEntity<String> getValidResponse(JsonElement message){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("success", true);
        jsonObject.add("message", message);

        return new ResponseEntity<>(jsonObject.toString(),HttpStatus.OK);
    }

    public static String getErrorResponse(Object message){
        JSONObject response = new JSONObject();
        response.put("message", message);
        response.put("success", false);
        return  response.toJSONString();
    }

    public static ResponseEntity getConnectionErrorResponse(Exception exception){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("reason", "Unable to establish a connection with node. Verify user credentials.");
        jsonObject.put("error", exception.toString());
        return new ResponseEntity<>(Arrays.asList("Unable to parse user to establish connection."), HttpStatus.BAD_REQUEST);
    }
}
