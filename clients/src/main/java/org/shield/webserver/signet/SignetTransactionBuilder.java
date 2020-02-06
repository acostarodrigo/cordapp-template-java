package org.shield.webserver.signet;

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
    private long amount;
    private String sourceNode;
    private String escrowWallet;
    private CordaRPCOps proxy;

    public SignetTransactionBuilder(CordaRPCOps proxy,long amount, String sourceNode, String escrowWallet) {
        this.proxy = proxy;
        this.amount = amount;
        this.sourceNode = sourceNode;
        this.escrowWallet = escrowWallet;
    }

    public SignetIssueTransactionState build(){
        Timestamp timestamp = Timestamp.from(Instant.now());
        Amount tokenAmount = new Amount(amount, FiatCurrency.Companion.getInstance("USD"));

        CordaX500Name x500Name = CordaX500Name.parse(sourceNode);
        Party sourceParty = proxy.wellKnownPartyFromX500Name(x500Name);
        SignetAccountState source = new SignetAccountState(sourceParty,"NA","NA");

        Party treasurerParty = proxy.nodeInfo().getLegalIdentities().get(0);
        SignetAccountState escrow = new SignetAccountState(treasurerParty, escrowWallet,"signetapidev+00@tassat.com");

        SignetIssueTransactionState signetIssueTransactionState = new SignetIssueTransactionState(UUID.randomUUID(),timestamp,tokenAmount,source,escrow,"", IssueState.CREATED);
        return signetIssueTransactionState;
    }
}
