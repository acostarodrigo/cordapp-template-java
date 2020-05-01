package org.shield.webserver.bond;

import com.fasterxml.jackson.databind.JsonNode;
import org.shield.bond.BondState;
import org.shield.bond.BondType;
import org.shield.bond.DealType;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;

public class BondMapper {
    private JsonNode body;

    public BondMapper(JsonNode body) {
        this.body = body;
    }

    public BondState getBond() throws ParseException {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        String id = body.get("id").textValue();
        String issuerTicker = body.get("issuerTicker").textValue();
        String denomination = body.get("denomination").textValue();
        String strStartDate = body.get("startDate").textValue();
        Date startdate = f.parse(strStartDate);
        int couponFrequency = body.get("couponFrequency").intValue();
        long minDenomination = body.get("minDenomination").longValue();
        long increment = body.get("increment").longValue();
        String strDealType = body.get("dealType").textValue();
        DealType dealType = DealType.valueOf(strDealType.toUpperCase());
        int redemptionPrice = body.get("redemptionPrice").intValue();
        long dealSize = body.get("dealSize").longValue();
        double initialPrice = body.get("initialPrice").doubleValue();
        String strMaturityDate = body.get("maturityDate").textValue();
        Date maturityDate = f.parse(strMaturityDate);
        double couponRate = body.get("couponRate").doubleValue();
        BondState bondState = new BondState(id, issuerTicker, Currency.getInstance(denomination),startdate,couponFrequency,minDenomination,increment, dealType, redemptionPrice, dealSize,initialPrice,maturityDate , couponRate,0, BondType.VANILA);
        System.out.println("BondMapper: " + bondState.toString());
        return bondState;
    }
}
