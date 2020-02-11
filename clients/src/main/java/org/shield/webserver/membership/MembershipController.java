package org.shield.webserver.membership;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.r3.businessnetworks.membership.flows.bno.ActivateMembershipFlow;
import com.r3.businessnetworks.membership.flows.bno.SuspendMembershipFlow;
import com.r3.businessnetworks.membership.flows.member.AmendMembershipMetadataFlow;
import com.r3.businessnetworks.membership.flows.member.GetMembershipsFlow;
import com.r3.businessnetworks.membership.flows.member.RequestMembershipFlow;
import com.r3.businessnetworks.membership.states.MembershipState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;
import org.shield.membership.ShieldMetadata;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.shield.webserver.response.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.json.Json;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.shield.webserver.response.Response.getConnectionErrorResponse;
import static org.shield.webserver.response.Response.getValidResponse;

@RestController
@RequestMapping("/membership")
public class MembershipController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;
    @Value("${bno}")
    private String bnoString;

    @GetMapping
    public ResponseEntity<Response> getMemberships (@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray responses = new JsonArray();
        int index = 0;
        for (StateAndRef<MembershipState> memberships : proxy.vaultQuery(MembershipState.class).getStates()){
            MembershipState membership = memberships.getState().getData();
            ShieldMetadata metadata = (ShieldMetadata) membership.getMembershipMetadata();
            ResponseWrapper response = new ResponseWrapper(index, membership.getStatus().name(), metadata, membership.getMember().toString(), membership.getBno().toString(),membership.getIssued().toString());
            responses.add(response.toJson());
            index++;
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("memberships", responses);
        return getValidResponse(jsonObject);
    }

    @PostMapping(value = "/request")
    public ResponseEntity<Response> requestMembership(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // we parse the user object
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            // and generate the connection
            generateConnection(user);
        } catch (IOException e){
            return getConnectionErrorResponse(e);
        }

        CordaX500Name bnoName = CordaX500Name.parse(bnoString);
        Party bno = proxy.wellKnownPartyFromX500Name(bnoName);

        MetadataBuilder metadataBuilder = new MetadataBuilder(body.get("metadata"),proxy);
        ShieldMetadata metadata = metadataBuilder.getMetadata();

        SignedTransaction signedTransaction = proxy.startFlowDynamic(RequestMembershipFlow.class,bno,metadata).getReturnValue().get();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("transaction", signedTransaction.getId().toString());
        return getValidResponse(jsonObject);
    }

    @PostMapping(value = "/update")
    public ResponseEntity<Response> updateMembership(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // we parse the user object
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            // and generate the connection
            generateConnection(user);
        } catch (IOException e){
            return getConnectionErrorResponse(e);
        }

        CordaX500Name bnoName = CordaX500Name.parse(bnoString);
        Party bno = proxy.wellKnownPartyFromX500Name(bnoName);

        MetadataBuilder metadataBuilder = new MetadataBuilder(body.get("metadata"),proxy);
        ShieldMetadata metadata = metadataBuilder.getMetadata();

        SignedTransaction signedTransaction = proxy.startFlowDynamic(AmendMembershipMetadataFlow.class,bno,metadata).getReturnValue().get();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("transaction", signedTransaction.getId().toString());
        return getValidResponse(jsonObject);
    }

    @PostMapping(value = "/activate")
    public ResponseEntity<Response> activateMembership(@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        int index = 0;
        try {
            // we parse the user object
            User user = objectMapper.readValue(body.get("user").toString(),User.class);

            // and we get the index
            index = body.get("index").asInt();

            // and generate the connection
            generateConnection(user);
        } catch (IOException e){
            return getConnectionErrorResponse(e);
        }


        StateAndRef<MembershipState> membership = proxy.vaultQuery(MembershipState.class).getStates().get(index);
        if (membership == null) throw new InterruptedException("No memberships available to activate");

        if (!membership.getState().getData().isPending()) throw new InterruptedException("Provided membership is not in pending state");

        SignedTransaction signedTransaction = proxy.startFlowDynamic(ActivateMembershipFlow.class, membership).getReturnValue().get();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("transaction", signedTransaction.getId().toString());
        return getValidResponse(jsonObject);

    }

    @PostMapping(value = "/suspend")
    public ResponseEntity<Response> suspendMembership(@Valid @RequestBody User user, int index) throws ExecutionException, InterruptedException {
        // we connect to the passed node
        generateConnection(user);

        StateAndRef<MembershipState> membership = proxy.vaultQuery(MembershipState.class).getStates().get(index);
        if (membership == null) throw new InterruptedException("No memberships available to suspend");

        if (!membership.getState().getData().isSuspended()) throw new InterruptedException("Provided membership is already suspended");

        SignedTransaction signedTransaction = proxy.startFlowDynamic(SuspendMembershipFlow.class, membership).getReturnValue().get();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("transaction", signedTransaction.getId().toString());
        return getValidResponse(jsonObject);
    }

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }
}
