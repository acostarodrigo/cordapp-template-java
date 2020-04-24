package org.shield.flows.trade;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.shield.bond.BondState;
import org.shield.flows.membership.MembershipFlows;
import org.shield.flows.offer.OfferFlow;
import org.shield.offer.OfferState;
import org.shield.trade.State;
import org.shield.trade.TradeState;

import java.util.Date;

public class OfflineTrade {
    private OfflineTrade(){}

    @StartableByRPC
    public static class IssuerCreate extends FlowLogic<UniqueIdentifier> {
        private String bondId;
        private Party buyer;
        private long size;
        private float tradedPrice;
        private float tradedYield;
        private Date settleDate;
        private long proceeds;
        private String arranger;


        public IssuerCreate(String bondId, Party buyer, long size, float tradedPrice, float tradedYield, Date settleDate, long proceeds, String arranger) {
            this.bondId = bondId;
            this.buyer = buyer;
            this.size = size;
            this.tradedPrice = tradedPrice;
            this.tradedYield = tradedYield;
            this.settleDate = settleDate;
            this.proceeds = proceeds;
            this.arranger = arranger;
        }

        @Override
        @Suspendable
        public UniqueIdentifier call() throws FlowException {
            // only issuer can create an offline trade process.
            if (!subFlow(new MembershipFlows.isIssuer())) throw new FlowException("Only a valid issuer organization can start an offline trade flow.");


            // bond must exists and be issued by issuer
            Party caller = getOurIdentity();
            VaultService vaultService = getServiceHub().getVaultService();
            BondState bond = null;
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            for (StateAndRef<BondState> stateAndRef : vaultService.queryBy(BondState.class,criteria).getStates()){
                if (stateAndRef.getState().getData().getId().equals(bondId)){
                    bond = stateAndRef.getState().getData();
                    break;
                }
            }
            if (bond == null) throw new FlowException(String.format("Specified bond with bondId %s does not exists.", bondId));
            if (!bond.getIssuer().equals(caller)) throw new FlowException(String.format("Caller is not the issuer for bond %s", bondId));

            // Issuer must have enought bonds in balance.
            TokenPointer tokenPointer = bond.toPointer(bond.getClass());
            Amount balance = QueryUtilitiesKt.tokenBalance(vaultService,tokenPointer);
            if (balance.getQuantity()<this.size) throw new FlowException(String.format("Issuer doesn't have enought balance to create trade. Current balance is %s", String.valueOf(balance.getQuantity())));

            // lets verify if we already have an offer for this bond.
            OfferState offer = null;
            StateAndRef<OfferState> offerInput = null;
            for (StateAndRef<OfferState> stateAndRef : vaultService.queryBy(OfferState.class, criteria).getStates()){
                if (stateAndRef.getState().getData().getBond().equals(bond)){
                    offer = stateAndRef.getState().getData();
                    offerInput = stateAndRef;
                    break;
                }
            }

            // no offer, so we will create a new one with the total amount of bonds available. We are creating the offer not AFS.
            if (offer == null){
                // created offer is for the full balance amount.
                offer = new OfferState(new UniqueIdentifier(),caller,bond,bond.getIssuerTicker(),tradedPrice,tradedYield,balance.getQuantity(),false, new Date());
                SignedTransaction offerTransaction = subFlow(new OfferFlow.Create(offer));
                offerInput = offerTransaction.getCoreTransaction().outRef(0);
            } else {
                // lets make sure we have money available
                if (offer.getAfsSize() < tradedPrice) throw new FlowException("Not enought AFS on the current offer.");

                // offer might not be AFS, we need to make sure trader has it.
                subFlow(new OfferFlow.NotifyBuyers(offerInput,offer));
            }



            // we will create a trade based on the offer.
            TradeState trade = new TradeState(new UniqueIdentifier(), offer, new Date(),settleDate, caller, buyer,caller,"arranger", tradedPrice, tradedYield,size,proceeds,bond.getDenomination(), State.PROPOSED, new Date());
            UniqueIdentifier tradeId = subFlow(new TradeFlow.Create(trade));
            return tradeId;
        }
    }
}
