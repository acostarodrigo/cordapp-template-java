package org.shield;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.flows.fiat.PayPalFlow;
import org.shield.flows.fiat.USDFiatTokenFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.shield.TestHelper.*;

public class USDFiatTest {

    @Before
    public void setUp() {
        TestHelper.setupNetwork();
    }

    @Test
    public void issueUSDTest() throws ExecutionException, InterruptedException {
        USDFiatTokenFlow.Issue usdFiatTokenFlow = new USDFiatTokenFlow.Issue(broker1, 10000);
        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(usdFiatTokenFlow);
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();

        for (StateAndRef<FungibleToken> stateAndRef : broker1Node.getServices().getVaultService().queryBy(FungibleToken.class).getStates()){
            System.out.println(stateAndRef.getState().getData().toString());
        }

    }

    @Test
    public void generatePaymentTest(){
//        String clientId = "Aewdva3oVI3i_F2v9Oyo8GkezDRKFUFpU_lBgaDOsKxU-8Ai3UGnyrTD6nIRMwpxxCxUTqxc2tV-tTqk";
//        String clientSecret = "EK08NeRkL-FACn-MkZbg92X6psr2EiTKoH19fg-eFAh2yM_cqDjj-wKNF-hn8oThSzf0gJuWE9cZCuAz";

        String clientId = "AYSq3RDGsmBLJE-otTkBtM-jBRd1TCQwFf9RGfwddNXWz0uFU9ztymylOhRS";
        String clientSecret = "EGnHDxD_qRPdaLdZz8iCr8N7_MzF-YHPTkjs6NKYQvQSBngp4PTTVWkPZRbL";

        Amount amount = new Amount();
        amount.setCurrency("USD");
        amount.setTotal("1.00");

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        List<Transaction> transactions = new ArrayList<Transaction>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl("https://example.com/cancel");
        redirectUrls.setReturnUrl("https://example.com/return");
        payment.setRedirectUrls(redirectUrls);

        try {
            APIContext apiContext = new APIContext(clientId, clientSecret, "sandbox");
            Payment createdPayment = payment.create(apiContext);
            System.out.println(createdPayment.toJSON());
            // For debug purposes only: System.out.println(createdPayment.toString());
        } catch (PayPalRESTException e) {
            // Handle errors
        } catch (Exception ex) {
            // Handle errors
        }
    }
    @Test
    public void PaypalTest() throws ExecutionException, InterruptedException {
        PayPalFlow payPalFlow = new PayPalFlow("PAYID-LXSYHVA3MB31185RS749111X");
        CordaFuture<Void> cordaFuture = issuerNode.startFlow(payPalFlow);
        mockNet.runNetwork();
        cordaFuture.get();
    }

    @After
    public void cleanUp(){
        TestHelper.cleanUpNetwork();
    }
}
