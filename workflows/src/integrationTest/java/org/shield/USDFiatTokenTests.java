package org.shield;

import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shield.flows.treasurer.StellarService;
import org.shield.flows.treasurer.USDFiatTokenFlow;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.shield.TestHelper.*;

public class USDFiatTokenTests {
    @Before
    public void setUp(){
        TestHelper.setupNetwork();
    }

    @Test
    public void issueUSDTokenTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(new USDFiatTokenFlow.Issue(broker1, 100000000));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // we re issue more tokens
        cordaFuture = issuerNode.startFlow(new USDFiatTokenFlow.Issue(broker1, 100000000));
        mockNet.runNetwork();
        signedTransaction = cordaFuture.get();
        assertNotNull(signedTransaction);

        // lets get usd token balance of broker1
        TokenType usd = FiatCurrency.Companion.getInstance("USD");
        Amount tokenBalance = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(), usd);

        // balance check
        assertEquals(tokenBalance.getQuantity(), 200000000);
    }

    @Test (expected = ExecutionException.class)
    public void issueUSDTokenWithoutBeingTreasurerTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> cordaFuture = issuerNode.startFlow(new USDFiatTokenFlow.Issue(broker1, 1000));
        mockNet.runNetwork();
        SignedTransaction signedTransaction = cordaFuture.get();
        assertNotNull(null); // this should never be triggered

    }

    @Test
    public void stellarTest() throws IOException, ExecutionException, InterruptedException {
        TokenType usd = FiatCurrency.Companion.getInstance("USD");
        // no balance for broker1
        assertEquals(QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(),usd).getQuantity(),0);

        // we create the omnibus account
        KeyPair pair = KeyPair.random();
        String friendbotUrl = String.format(
            "https://friendbot.stellar.org/?addr=%s",
            pair.getAccountId());
        new URL(friendbotUrl).openStream();

        MembershipTests membershipTests = new MembershipTests();
        membershipTests.configTreasurerTest();

        // we send some lumens to the escrow account
        sendLumensToEscrow(broker1.getName().toString(),pair.getAccountId());

        broker2Node.getServices().cordaService(StellarService.class).startListener(String.copyValueOf(pair.getSecretSeed()),broker1);
        mockNet.runNetwork();


        Amount amount = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(),usd);
        assertEquals(amount.getQuantity(), 1);
        System.out.println(String.format("Current Corda Fiat token Balance: %s", amount.getQuantity()));
    }


    private void sendLumensToEscrow(String from, String address) throws IOException {
        KeyPair source = KeyPair.fromSecretSeed("SAKTKP3V4WYVSOKCZRNNU2EPB757GI3DMCTV5GCCMAWTZ3LFTPDU6GR6");
        Server server = new Server("https://horizon-testnet.stellar.org");
        KeyPair destination = KeyPair.fromAccountId(address);

        // If there was no error, load up-to-date information on your account.
        AccountResponse sourceAccount = server.accounts().account(source.getAccountId());

        // Start building the transaction.
        Transaction transaction = new Transaction.Builder(sourceAccount,Network.TESTNET)
            .setOperationFee(100)
            .addOperation(new PaymentOperation.Builder(destination.getAccountId(), new AssetTypeNative(), "1").build())
            // A memo allows you to add your own metadata to a transaction. It's
            // optional and does not affect how Stellar treats the transaction.
            .addMemo(Memo.text(from))
            // Wait a maximum of three minutes for the transaction
            .setTimeout(180)
            .build();
        // Sign the transaction to prove you are actually the person sending it.
        transaction.sign(source);

        // And finally, send it off to Stellar!
        try {
            SubmitTransactionResponse response = server.submitTransaction(transaction);

        } catch (Exception e) {
            System.out.println("Something went wrong!");
            System.out.println(e.getMessage());
            // If the result is unknown (no response body, timeout etc.) we simply resubmit
            // already built transaction:
            // SubmitTransactionResponse response = server.submitTransaction(transaction);
        }

    }

    @After
    public void cleanUp(){
        TestHelper.cleanUpNetwork();
    }
}
