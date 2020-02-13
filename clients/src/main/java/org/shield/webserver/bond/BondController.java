package org.shield.webserver.bond;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.shield.bond.BondState;
import org.shield.bond.DealType;
import org.shield.flows.bond.BondFlow;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.shield.webserver.response.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.shield.webserver.response.Response.*;

@RestController
@RequestMapping("/bond") // The paths for HTTP requests are relative to this base path.
public class BondController {
    private Connection connection;
    private ProxyEntry proxyEntry;
    private CordaRPCOps proxy;

    @PostMapping
    public ResponseEntity<Response> issueBond (@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException, TimeoutException {
        ObjectMapper objectMapper = new ObjectMapper();
        User user = null;
        BondState bond = null;
        try {
            user = objectMapper.readValue(body.get("user").toString(),User.class);
            bond = objectMapper.readValue(body.get("bond").toString(),BondState.class);

            // we initiaite the connection
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }


        // manually defined ids of bond.
        bond.setLinearId(new UniqueIdentifier());

        // make the call to the node
        CordaFuture<String> cordaFuture = proxy.startFlowDynamic(BondFlow.Issue.class, bond).getReturnValue();
        String uniqueIdentifier = cordaFuture.get();

        // we set the issuer to provide the response
        Party caller = proxy.nodeInfo().getLegalIdentities().get(0);
        bond.setIssuer(caller);

        // we prepare the response
        return getValidResponse(bond.toJson());
    }

    @PostMapping("/csv")
    public ResponseEntity<Response> issueBondCSV (@NotNull @RequestBody JsonNode body) throws ExecutionException, InterruptedException, TimeoutException {
        ObjectMapper objectMapper = new ObjectMapper();
        User user = null;
        Reader csv = null;
        try {
            user = objectMapper.readValue(body.get("user").toString(), User.class);
            csv = new StringReader(body.get("csv").textValue());

            // we initiaite the connection
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        CSVParser csvParser = null;
        try {
            // we parse the CSV data
            csvParser = new CSVParser(csv,CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withIgnoreEmptyLines()
                .withTrim()
            );
        } catch (IOException e) {
            return getErrorResponse("Unable to parse CSV file", e);
        }

        // we iterate each record and parse the bonds
        List<BondState> bonds = new ArrayList<>();
        for (CSVRecord csvRecord : csvParser){
            try {
                String id = csvRecord.get("bond_id");
                String ticker = csvRecord.get("ticker");
                Currency currency = Currency.getInstance(csvRecord.get("currency"));
                Date startDate = new SimpleDateFormat("YYYY-mm-dd").parse(csvRecord.get("start_date"));
                Date maturityDate = new SimpleDateFormat("YYYY-mm-dd").parse(csvRecord.get("maturity_date"));
                int couponFrequency = Integer.valueOf(csvRecord.get("coupon"));
                int minDenomination = Integer.valueOf(csvRecord.get("min_denomination"));
                long size = Long.parseLong(csvRecord.get("size"));
                long increment = Long.parseLong(csvRecord.get("min_increment"));
                DealType dealType = DealType.valueOf(csvRecord.get("deal_type"));
                int redemptionPrice = Integer.parseInt(csvRecord.get("price"));
                double initialPrice = Double.parseDouble(csvRecord.get("price"));
                double couponRate = Double.parseDouble(csvRecord.get("coupon"));
                BondState bondState = new BondState(id, ticker, currency, startDate,couponFrequency,minDenomination,increment,dealType,redemptionPrice,size,initialPrice,maturityDate,couponRate,0);
                bonds.add(bondState);
            } catch (Exception e){
                return getErrorResponse("Unable to parse bond from CSV file", e);
            }

        }

        // now we issue the bonds
        JsonArray jsonElements = new JsonArray();
        boolean success = true;
        for (BondState bondState : bonds){
            try{
                // manually defined ids of bond.
                bondState.setLinearId(new UniqueIdentifier());

                CordaFuture<String> cordaFuture = proxy.startFlowDynamic(BondFlow.Issue.class, bondState).getReturnValue();
                String id = cordaFuture.get();

                // we prepare the response
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("success",true);
                jsonObject.addProperty("id", id);
                jsonElements.add(jsonObject);
            } catch (Exception e){
                success = false;
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("success", false);
                jsonObject.addProperty("error", e.toString());
                jsonElements.add(jsonObject);
            }
        }

        JsonObject response = new JsonObject();
        response.add("bonds", jsonElements);
        return getResponse(success, response);

    }


    private void generateConnection(User user){
        connection = new Connection(user);
        proxyEntry = connection.login();
        proxy = proxyEntry.getProxy();
    }

    @GetMapping
    public ResponseEntity<Response> getBonds(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray response = new JsonArray();
        for (StateAndRef<BondState> stateAndRef : proxy.vaultQuery(BondState.class).getStates()){
            BondState bond = stateAndRef.getState().getData();
            response.add(bond.toJson());
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("bonds", response);
        return getValidResponse(jsonObject);
    }
}
