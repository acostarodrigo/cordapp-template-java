package org.shield.webserver.arrangement;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import org.shield.flows.arrangement.ArrangementFlow;
import org.shield.states.ArrangementState;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/arrangement") // The paths for HTTP requests are relative to this base path.
public class ArrangementController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @PostMapping (value = "/preIssue")
    public ResponseEntity<String> preIssueArrangement(@Valid @RequestBody RequestWrapper body) throws ExecutionException, InterruptedException {
        // we connect to the passed node
        generateConnection(body.getUser());


        // we generate the broker dealer party
        CordaX500Name brokerDealerName = CordaX500Name.parse(body.getBrokerDealer());
        Party brokerDealer = proxy.wellKnownPartyFromX500Name(brokerDealerName);

        int size = body.getSize();
        Date offeringDate = body.getOfferingDate();

        UniqueIdentifier id = proxy.startFlowDynamic(ArrangementFlow.PreIssue.class, brokerDealer, size, offeringDate).getReturnValue().get();
        return new ResponseEntity<>(id.toString(), HttpStatus.OK);
    }

    @PostMapping (value = "/accept")
    public ResponseEntity<String> acceptArrangement(@Valid @RequestBody RequestWrapper2 body) throws ExecutionException, InterruptedException {
        // we connect to the passed node
        generateConnection(body.getUser());
        UniqueIdentifier id = new UniqueIdentifier(body.getId().split("_")[0], UUID.fromString(body.getId().split("_")[1]));
        int size = body.getSize();

        proxy.startFlowDynamic(ArrangementFlow.Accept.class, id, size).getReturnValue().get();
        return new ResponseEntity<>(id.toString(),HttpStatus.OK);
    }

    @PostMapping (value = "/cancel")
    public ResponseEntity<String> cancelArrangement(@Valid @RequestBody RequestWrapper2 body) throws ExecutionException, InterruptedException {
        // we connect to the passed node
        generateConnection(body.getUser());
        UniqueIdentifier id = new UniqueIdentifier(body.getId().split("_")[0], UUID.fromString(body.getId().split("_")[1]));


        proxy.startFlowDynamic(ArrangementFlow.Cancel.class, id).getReturnValue().get();
        return new ResponseEntity<>(id.toString(),HttpStatus.OK);
    }

    @PostMapping (value = "/issue")
    public ResponseEntity<String> issueArrangement(@Valid @RequestBody RequestWrapper2 body) throws ExecutionException, InterruptedException {
        // we connect to the passed node
        generateConnection(body.getUser());
        UniqueIdentifier id = new UniqueIdentifier(body.getId().split("_")[0], UUID.fromString(body.getId().split("_")[1]));


        proxy.startFlowDynamic(ArrangementFlow.Issue.class, id).getReturnValue().get();
        return new ResponseEntity<>(id.toString(),HttpStatus.OK);
    }

    @GetMapping (value = "")
    @JsonIgnore
    public ResponseEntity<List<ResponseWrapper>> preIssueArrangement(@Valid @RequestBody User user) throws ExecutionException, InterruptedException, JsonProcessingException {

        generateConnection(user);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.DEFAULT);


        List<ResponseWrapper> result = new ArrayList<>();
        for (StateAndRef<ArrangementState> stateAndRef : proxy.vaultQuery(ArrangementState.class).getStates()){
            ArrangementState arrangementState = stateAndRef.getState().getData();

            ResponseWrapper responseWrapper = new ResponseWrapper(arrangementState.getId().toString(),arrangementState.getIssuer().getName().toString(), arrangementState.getBrokerDealer().getName().toString(),arrangementState.getSize(), arrangementState.getOfferingDate(), arrangementState.getState().toString());
            result.add(responseWrapper);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }
}
