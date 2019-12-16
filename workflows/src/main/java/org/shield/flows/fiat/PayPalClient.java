package org.shield.flows.fiat;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;

public class PayPalClient {

    /**
     *Set up the PayPal Java SDK environment with PayPal access credentials.
     *This sample uses SandboxEnvironment. In production, use LiveEnvironment.
     */
    private PayPalEnvironment environment = new PayPalEnvironment.Sandbox(
        "Aewdva3oVI3i_F2v9Oyo8GkezDRKFUFpU_lBgaDOsKxU-8Ai3UGnyrTD6nIRMwpxxCxUTqxc2tV-tTqk",
        "EK08NeRkL-FACn-MkZbg92X6psr2EiTKoH19fg-eFAh2yM_cqDjj-wKNF-hn8oThSzf0gJuWE9cZCuAz");

    /**
     *PayPal HTTP client instance with environment that has access
     *credentials context. Use to invoke PayPal APIs.
     */
    PayPalHttpClient client = new PayPalHttpClient(environment);

    /**
     *Method to get client object
     *
     *@return PayPalHttpClient client
     */
    public PayPalHttpClient client() {
        return this.client;
    }
}
