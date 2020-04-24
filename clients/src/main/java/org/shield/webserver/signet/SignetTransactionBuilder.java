package org.shield.webserver.signet;

import com.fasterxml.jackson.databind.JsonNode;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import org.shield.signet.IssueState;
import org.shield.signet.SignetAccountState;
import org.shield.signet.SignetIssueTransactionState;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class SignetTransactionBuilder {
    private JsonNode body;
    private CordaRPCOps proxy;


    public SignetTransactionBuilder(JsonNode body, CordaRPCOps proxy) {
        this.body = body;
        this.proxy = proxy;
    }

    public SignetIssueTransactionState build(){
        long amount = body.get("amount").asLong();
        String sourceNode = body.get("owner").asText();
        String escrowWallet = "0xce067dff1eb2567ed72feebe22dd0a8547d38c77";

        int timestamp = Timestamp.from(Instant.now()).getNanos();
        Amount tokenAmount = new Amount(amount, FiatCurrency.Companion.getInstance("USD"));

        CordaX500Name x500Name = CordaX500Name.parse(sourceNode);
        Party sourceParty = proxy.wellKnownPartyFromX500Name(x500Name);
        SignetAccountState source = new SignetAccountState(sourceParty,"0x8238017e7d940c29a9ad99a7a5e3774658924640","signetapidev+00@tassat.com");

        Party treasurerParty = proxy.nodeInfo().getLegalIdentities().get(0);
        // todo this needs to be fixed. UserToken should come from a configuration file in the corda node.
        SignetAccountState escrow = new SignetAccountState(treasurerParty, escrowWallet,"ZXlKMGVYQWlPaUpLVjFRaUxDSmhiR2NpT2lKSVV6STFOaUo5LmV5SnpkV0lpT2lJeU1ESXdNRE13T1RBM01EQXdORFU0TWlJc0luSnZiR1VpT2pBc0ltbHpjeUk2SWxObFkzVnlhWFJwZW1WVWIxTnBaMjVsZEVGUVNTMVZRVlFpTENKdVlXMWxJam9pVW05a2NtbG5ieUJCWTI5emRHRWlMQ0pwWVhRaU9qRTFPRE01TmpnNU5UQjkuUG1ZSEdpM2ZNc0o1VXU0WnowUmRORUltNGpndFR6RGdFR1N4dUJfTmJyTQ==");

        SignetIssueTransactionState signetIssueTransactionState = new SignetIssueTransactionState(UUID.randomUUID(),timestamp,tokenAmount,source,escrow,"", IssueState.CREATED);
        return signetIssueTransactionState;
    }
}
