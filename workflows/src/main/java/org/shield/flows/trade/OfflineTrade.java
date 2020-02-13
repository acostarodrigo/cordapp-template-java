package org.shield.flows.trade;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.shield.bond.BondState;

import java.util.Date;

public class OfflineTrade {
    private OfflineTrade(){}


    public static class IssuerCreate extends FlowLogic<SignedTransaction> {
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
        public SignedTransaction call() throws FlowException {
            return null;
        }
    }
}
