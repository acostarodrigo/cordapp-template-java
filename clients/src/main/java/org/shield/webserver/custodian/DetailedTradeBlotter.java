package org.shield.webserver.custodian;

import com.google.gson.JsonObject;
import org.shield.trade.State;
import org.shield.trade.TradeState;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DetailedTradeBlotter {
    private TradeState trade;
    private Map<State, Date> stateDateMap;

    public DetailedTradeBlotter() {
        stateDateMap = new HashMap<>();
    }

    public TradeState getTrade() {
        return trade;
    }

    public void setTrade(TradeState trade) {
        this.trade = trade;
    }


    public void addState(State state, Date date){
        stateDateMap.put(state, date);
    }

    public JsonObject toJson(){
        JsonObject jsonObject = trade.toJson();
        if (stateDateMap.size()>0) {
            for (Map.Entry<State, Date> entry : stateDateMap.entrySet()) {
                jsonObject.addProperty(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return jsonObject;
    }
}
