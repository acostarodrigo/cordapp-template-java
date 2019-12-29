package org.shield.flows.treasurer;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.flows.FlowException;
import net.corda.core.identity.Party;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.SignedTransaction;
import org.stellar.sdk.*;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import shadow.com.google.common.base.Optional;

import java.util.concurrent.ExecutionException;

@CordaService
public class StellarService extends SingletonSerializeAsToken {
    private AppServiceHub appServiceHub;


    public StellarService(AppServiceHub appServiceHub) {
        this.appServiceHub = appServiceHub;
    }


    /**
     * Starts listening on stellar for incomming crypto
     * @param secretSeed this would be Shields account
     * @param owner whom will be the owner of the issed tokens
     * @throws InterruptedException
     */
    public void startListener(String secretSeed, Party owner) throws InterruptedException {
        Server server = new Server("https://horizon-testnet.stellar.org");
        KeyPair account = KeyPair.fromSecretSeed(secretSeed);
        System.out.println("Preparing listener for account " + account.getAccountId());
        // Create an API call to query payments involving the account.
        PaymentsRequestBuilder paymentsRequest = server.payments().forAccount(account.getAccountId());

        // `stream` will send each recorded payment, one by one, then keep the
        // connection open and continue to send you new payments as they occur.
        paymentsRequest.stream(new EventListener<OperationResponse>() {
            @Override
            @Suspendable
            public void onEvent(OperationResponse payment) {
                // The payments stream includes both sent and received payments. We only
                // want to process received payments here.
                if (payment instanceof PaymentOperationResponse) {
                    if (((PaymentOperationResponse) payment).getTo().equals(account)) {
                        return;
                    }

                    String amount = ((PaymentOperationResponse) payment).getAmount();

                    Asset asset = ((PaymentOperationResponse) payment).getAsset();
                    String assetName;
                    if (asset.equals(new AssetTypeNative())) {
                        assetName = "lumens";
                    } else {
                        StringBuilder assetNameBuilder = new StringBuilder();
                        assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getCode());
                        assetNameBuilder.append(":");
                        assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getIssuer());
                        assetName = assetNameBuilder.toString();
                    }

                    StringBuilder output = new StringBuilder();
                    output.append(amount);
                    output.append(" ");
                    output.append(assetName);
                    output.append(" from ");
                    output.append(((PaymentOperationResponse) payment).getFrom());
                    output.append(" Issuing tokens in corda... ");
                    System.out.println(output.toString());

                    Thread flowThread = new Thread(new StartIssueFlow(owner, "1"));
                    flowThread.start();
                    System.out.println("Stellar tx https://testnet.steexp.com/account/" + account.getAccountId());
                }

            }

            @Override
            @Suspendable
            public void onFailure(Optional<Throwable> optional, Optional<Integer> optional1) {

            }
        });

        // adding this for testing
        Thread.sleep(6000);
    }


    /**
     * Since we are starting a new flow, to avoid blocking, this must happen in a new thread.
     */
    private class StartIssueFlow implements Runnable{
        private Party owner;
        private String amount;

        public StartIssueFlow(Party owner, String amount) {
            this.owner = owner;
            this.amount = amount;
        }

        @Override
        public void run() {
            CordaFuture<SignedTransaction> cordaFuture=  appServiceHub.startFlow(new USDFiatTokenFlow.Issue(owner, Long.valueOf(amount))).getReturnValue();
            try {
                SignedTransaction signedTransaction = cordaFuture.get();
                System.out.println("Tokens issued at corda tx " + signedTransaction.toString());

            } catch (Exception e) {
                System.out.println("Error on listener " + e.toString() );
            }
        }
    }


}
