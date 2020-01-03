package org.shield.webserver;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.shield.webserver.bond.BondController;
import org.shield.webserver.membership.MembershipController;
import org.shield.webserver.membership.ResponseWrapper;
import org.shield.webserver.trade.TradeController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import javax.json.stream.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=Starter.class)
public class SpringTest {
    ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private BondController bondController;

    @Autowired
    private MembershipController membershipController;


    @Test
    public void requestIssuerMembershipTest() throws InterruptedException, ExecutionException, IOException {
        String body = "{\n" +
            "\t\"user\": {\"serverName\":\"localhost:10006\", \"username\":\"user1\",\"password\":\"test\"},\n" +
            "\t\"metadata\":{\n" +
            "\t\t\"orgName\": \"issuer\",\n" +
            "\t\t\"orgTypes\": [\"BOND_PARTICIPANT\"],\n" +
            "\t\t\"bondRoles\": [\"ISSUER\", \"SELLER\", \"BUYER\"],\n" +
            "\t\t\"orgContact\": \"issuer@shield.com\",\n" +
            "\t\t\"custodians\": [\"O=broker1,L=New York,C=US\"],\n" +
            "\t\t\"treasurers\": [\"O=broker1,L=New York,C=US\"]\n" +
            "\t}\n" +
            "}";
        ResponseEntity responseEntity = requestMembership(body);
    }

    @Test
    public void configureMembershipTest() throws InterruptedException, ExecutionException, IOException {
        requestIssuerMembershipTest();
        requestBuyerMembershipTest();
        approvePendingRequestsTest();
    }

    @Test
    public void requestBuyerMembershipTest() throws InterruptedException, ExecutionException, IOException {
        String body = "{\n" +
            "\t\"user\": {\"serverName\":\"localhost:10009\", \"username\":\"user1\",\"password\":\"test\"},\n" +
            "\t\"metadata\":{\n" +
            "\t\t\"orgName\": \"issuer\",\n" +
            "\t\t\"orgTypes\": [\"BOND_PARTICIPANT\"],\n" +
            "\t\t\"bondRoles\": [\"ISSUER\", \"SELLER\", \"BUYER\"],\n" +
            "\t\t\"orgContact\": \"issuer@shield.com\",\n" +
            "\t\t\"custodians\": [\"O=broker1,L=New York,C=US\"],\n" +
            "\t\t\"treasurers\": [\"O=broker1,L=New York,C=US\"]\n" +
            "\t}\n" +
            "}";
        ResponseEntity responseEntity = requestMembership(body);
    }

    @Test
    public void approvePendingRequestsTest() throws InterruptedException, ExecutionException, IOException {
        ResponseEntity<List<ResponseWrapper>> responseEntity = getMemberships();
        for (ResponseWrapper response : responseEntity.getBody()){
            approveRequest();
        }

    }

    public void approveRequest() throws IOException, ExecutionException, InterruptedException {
        assertNotNull(membershipController);
        String body = "{\n" +
            "\t\"user\":{\"serverName\":\"BNO:10012\", \"username\":\"user1\",\"password\":\"test\"},\n" +
            "\t\"index\": 0\n" +
            "}";
        JsonNode jsonNode = mapper.readTree(body);
        membershipController.activateMembership(jsonNode);
    }

    public ResponseEntity<List<ResponseWrapper>> getMemberships() throws IOException, ExecutionException, InterruptedException {
        assertNotNull(membershipController);
        String body = "";
        JsonNode user = mapper.readTree(body);
        return membershipController.getMemberships(user);
    }

    private ResponseEntity requestMembership(String body) throws IOException, ExecutionException, InterruptedException {
        assertNotNull(membershipController);
        String bodyString = "";
        JsonNode jsonNode = mapper.readTree(bodyString);
        ResponseEntity responseEntity = membershipController.requestMembership(jsonNode);
        assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        return responseEntity;
    }

    @Test
    public void issueBondTest() throws Exception {
        assertNotNull(bondController);

        String issuerBody = "{\"user\": {\"serverName\":\"issuer:10006\", \"username\":\"user1\",\"password\":\"test\"}}";

        JsonNode parameter = mapper.readTree(issuerBody);
        System.out.println(bondController.getBonds(parameter).toString());


    }
}
