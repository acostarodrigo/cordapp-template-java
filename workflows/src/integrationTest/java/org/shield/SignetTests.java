package org.shield;

import org.junit.Test;
import org.shield.flows.treasurer.signet.SignetAPI;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SignetTests {

    @Test
    public void isTokenExpiredTest(){
         Map<String,String> loginBody = new HashMap<>();
         loginBody.put("client_secret", "gv3MdMSN2WhFrSo86nXHVzDjCJqk-a2nmvdI_7fKehVAaPg2ClP2SzbU3PS5W1Zb");


        SignetAPI signetAPI = new SignetAPI(null, null, loginBody);
        assertFalse(signetAPI.isTokenExpired());
    }
}
