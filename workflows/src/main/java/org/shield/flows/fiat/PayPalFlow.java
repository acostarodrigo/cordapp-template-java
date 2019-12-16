package org.shield.flows.fiat;

import co.paralleluniverse.fibers.Suspendable;
import com.braintreepayments.http.HttpResponse;
import com.braintreepayments.http.serializer.Json;
import com.paypal.orders.Order;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.OrdersGetRequest;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class PayPalFlow  extends FlowLogic<Void> {
    String orderId;

    public PayPalFlow(String orderId) {
        this.orderId = orderId;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        PayPalClient client = new PayPalClient();
        OrdersGetRequest request = new OrdersGetRequest(orderId);
        try {
            HttpResponse<Order> response = client.client().execute(request);
            System.out.println("Full response body:");
            System.out.println(response.result().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
