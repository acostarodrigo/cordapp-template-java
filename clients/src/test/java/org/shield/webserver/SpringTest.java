package org.shield.webserver;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.shield.webserver.bond.BondController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.json.stream.JsonParser;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=Starter.class)
public class SpringTest {
    @Autowired
    private BondController bondController;

    @Test
    public void testSomething() throws Exception {
        assertNotNull(bondController);

        ObjectMapper mapper = new ObjectMapper();
        String body = "{\"user\": {\"serverName\":\"issuer:10006\", \"username\":\"user1\",\"password\":\"test\"}}";
        JsonNode parameter = mapper.readTree(body);
        System.out.println(bondController.getBonds(parameter).toString());


    }
}
