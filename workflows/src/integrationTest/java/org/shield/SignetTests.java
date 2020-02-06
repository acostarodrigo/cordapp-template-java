package org.shield;

import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.shield.flows.treasurer.signet.SignetFlow;
import org.shield.signet.IssueState;
import org.shield.signet.SignetAccountState;
import org.shield.signet.SignetIssueTransactionState;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.shield.TestHelper.*;

public class SignetTests {

    @Before
    public void configure() throws ExecutionException, InterruptedException {
        if (mockNet == null)
            TestHelper.setupNetwork();

        MembershipTests membershipTests = new MembershipTests();
        membershipTests.configBuyerTest();
        membershipTests.configTreasurerTest();
        membershipTests.configIssuerTest();
    }

    @Test
    public void depositToEscrowAndIssueTest() throws ExecutionException, InterruptedException {
        SignetAccountState escrow = new SignetAccountState(issuer,"0x8238017e7d940c29a9ad99a7a5e3774658924640","signetapidev+00@tassat.com");
        SignetAccountState source = new SignetAccountState(broker1, "0x8238017e7d940c29a9ad99a7a5e3774658924640", "signetapidev+00@tassat.com");
        Amount amount = new Amount(10000000, FiatCurrency.Companion.getInstance("USD"));

        Amount balance = QueryUtilitiesKt.tokenBalance(broker2Node.getServices().getVaultService(),FiatCurrency.Companion.getInstance("USD"));

        SignetIssueTransactionState signetIssueTransactionState = new SignetIssueTransactionState(UUID.randomUUID(), Timestamp.from(Instant.now()),amount,source,escrow,"", IssueState.CREATED);
        CompletableFuture<UUID> future = broker2Node.startFlow(new SignetFlow.DepositToEscrowAndIssue(signetIssueTransactionState)).toCompletableFuture();
        mockNet.runNetwork();
        future.get();

        Amount newBalance = QueryUtilitiesKt.tokenBalance(broker1Node.getServices().getVaultService(),FiatCurrency.Companion.getInstance("USD"));
        mockNet.runNetwork();
        Assert.assertEquals(amount, newBalance);
    }

}
