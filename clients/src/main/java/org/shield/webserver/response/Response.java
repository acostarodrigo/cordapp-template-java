package org.shield.webserver.response;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.glassfish.jersey.server.JSONP;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.Arrays;

public class Response implements Serializable {
    private boolean success;
    private JSONObject message;

    public Response(boolean success, JSONObject message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public JSONObject getMessage() {
        return message;
    }

    public void setMessage(JSONObject message) {
        this.message = message;
    }

    public static ResponseEntity<Response> getValidResponse(JsonObject message){
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(message.toString());
            Response response = new Response(true, jsonObject);
            return new ResponseEntity<>(response,HttpStatus.OK);
        } catch (ParseException e) {
            return null;
        }

    }


    public static ResponseEntity<Response> getErrorResponse(JsonObject message){
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) parser.parse(message.toString());
            Response response = new Response(true, jsonObject);
            return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
        } catch (ParseException e) {
            return null;
        }
    }

    public static ResponseEntity<Response> getConnectionErrorResponse(Exception exception){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("reason", "Unable to establish a connection with node. Verify user credentials.");
        jsonObject.put("error", exception.toString());


        Response response = new Response(false, jsonObject);
        return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }
}
