package org.shield.webserver.bond;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import kotlin.Pair;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.shield.bond.BondState;
import org.shield.bond.BondType;
import org.shield.bond.DealType;
import org.shield.custodian.CustodianState;
import org.shield.flows.bond.BondFlow;
import org.shield.trade.State;
import org.shield.trade.TradeState;
import org.shield.webserver.connection.Connection;
import org.shield.webserver.connection.ProxyEntry;
import org.shield.webserver.connection.User;
import org.shield.webserver.response.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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
            BondMapper bondMapper = new BondMapper(body.get("bond"));
            bond = bondMapper.getBond();
            // we initiate the connection
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
                BondState bondState = new BondState(id, ticker, currency, startDate,couponFrequency,minDenomination,increment,dealType,redemptionPrice,size,initialPrice,maturityDate,couponRate,0, BondType.VANILA);
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

    @GetMapping("/issuerPanelBonds")
    public ResponseEntity<Response> getIssuerPannelBonds(@NotNull @RequestBody JsonNode body){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        JsonArray jsonArray = new JsonArray();
        for (StateAndRef<BondState> stateAndRef : proxy.vaultQuery(BondState.class).getStates()){
            BondState bondState = stateAndRef.getState().getData();
            JsonObject bondObject = new JsonObject();
            bondObject.addProperty("id", bondState.getId());
            bondObject.addProperty("ticker", bondState.getTicker());
            jsonArray.add(bondObject);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("bonds", jsonArray);
        return getValidResponse(jsonObject);
    }

    @GetMapping("/issuerPanelBondDetails")
    public ResponseEntity<Response> getIssuerPannelBondDetails(@NotNull @RequestBody JsonNode body) throws ParseException {
        ObjectMapper objectMapper = new ObjectMapper();
        String bondId = body.get("bondId").textValue();
        try {
            User user = objectMapper.readValue(body.get("user").toString(),User.class);
            generateConnection(user);
        } catch (IOException e) {
            return getConnectionErrorResponse(e);
        }

        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        Party caller = proxy.nodeInfo().getLegalIdentities().get(0);

        BondState bondState = null;
        for (StateAndRef<BondState> stateAndRef : proxy.vaultQueryByCriteria(criteria, BondState.class).getStates()){
            if (stateAndRef.getState().getData().getId().equals(bondId)){
                bondState = stateAndRef.getState().getData();
                break;
            }
        }

        // first we are getting caller issued bonds.
        JsonArray result = new JsonArray();

        for (StateAndRef<FungibleToken> stateAndRef : proxy.vaultQueryByCriteria(criteria, FungibleToken.class).getStates()){
            FungibleToken token = stateAndRef.getState().getData();

            String tokenIdentifier = token.getTokenType().getTokenIdentifier();
            if (tokenIdentifier.equals(bondState.getLinearId().getId().toString())) {
                JsonObject bondJson = new JsonObject();
                bondJson.addProperty("investorName", caller.getName().toString());
                bondJson.addProperty("holdings", token.getAmount().getQuantity());
                bondJson.addProperty("lastPricePaid", 0);
                bondJson.addProperty("lastTradeDate", 0);
                bondJson.addProperty("currency", "USD");

                // we are adding issuer bond tokens into the result
                result.add(bondJson);
            }
        }

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");

        // then we are getting trade info of those bonds.
        Map<Pair<String, Party>, JsonObject> traderResult = new HashMap<>();
        for (StateAndRef<TradeState> stateAndRef : proxy.vaultQuery(TradeState.class).getStates()){
            TradeState tradeState = stateAndRef.getState().getData();
            if (tradeState.getSeller().equals(caller) && (tradeState.getState().equals(State.PENDING) || tradeState.getState().equals(State.SETTLED)) && (tradeState.getOffer().getBond().getId().equals(bondId))){
                // we need to group this for every investor
                Pair<String, Party> key = new Pair<>(tradeState.getOffer().getBond().getId(), tradeState.getBuyer());

                if (traderResult.containsKey(key)){
                    // we do have an entry for this bond and trader, we will update
                    JsonObject traderObject = traderResult.get(key);
                    long holdings = traderObject.get("holdings").getAsLong();
                    traderObject.addProperty("holdings", tradeState.getSize() + holdings);

                    Date tradeDate = f.parse(traderObject.get("lastTradeDate").getAsString());
                    if (tradeDate.before(tradeState.getTradeDate()))
                        traderObject.addProperty("lastTradeDate", f.format(tradeState.getTradeDate()));

                    if (tradeDate.before(tradeState.getTradeDate()))
                        traderObject.addProperty("lastPricePaid", tradeState.getSize());

                    traderResult.replace(key, traderObject);

                } else {
                    // new entry for this bond and trader
                    JsonObject traderObject = new JsonObject();
                    traderObject.addProperty("investorName", tradeState.getBuyer().getName().toString());
                    traderObject.addProperty("holdings", tradeState.getSize());
                    traderObject.addProperty("lastPricePaid", tradeState.getSize());
                    traderObject.addProperty("lastTradeDate", f.format(tradeState.getTradeDate()));
                    traderObject.addProperty("currency", "USD");
                    traderObject.add("trade", tradeState.toJson());
                    traderResult.put(key, traderObject);
                }
            }
        }


        for (JsonObject object : traderResult.values()){
            result.add(object);
        }


        JsonObject jsonObject = new JsonObject();
        jsonObject.add("bonds", result);
        return getValidResponse(jsonObject);
    }
}
