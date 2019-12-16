package org.shield.flows.bond;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.SignedTransaction;
import org.shield.bond.BondState;
import org.shield.bond.BondTypeContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

// start IssueFungibleToken offeringDate: "2020-12-12", fungibleAmount: 1, holder: "O=PartyB,L=New York,C=US"

public class BondFlow {

    // instantiation now allowed
    private BondFlow(){
    }


    /**
     * Creates CommercialPaper token type into blockchain.
     */
    @InitiatingFlow
    public static class IssueType extends FlowLogic<SignedTransaction> {
        private BondState bondState;
        private FlowSession callerSession;

        public IssueType(FlowSession callerSession) {
            this.callerSession = callerSession;
        }

        public void setBondState(BondState bondState) {
            this.bondState = bondState;
        }

        public BondState getBondState() {
            return bondState;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party notary = getServiceHub(). getNetworkMapCache(). getNotaryIdentities(). get(0);
            TransactionState transactionState = new TransactionState(this.bondState, BondTypeContract.ID, notary);
            return (SignedTransaction) subFlow(new CreateEvolvableTokens(transactionState));
        }
    }

    /**
     * Issues non fungible tokens of type CommercialPaperTokenType to the specified holder
     */
    @StartableByRPC
    @InitiatingFlow
    public static class IssueFungibleToken extends FlowLogic<UniqueIdentifier> {
        private long fungibleAmount;
        private final Party holder;
        private Date offeringDate;


        /**
         * Constructor
         * @param offeringDate the offering date of the commercial paper
         * @param fungibleAmount the amount of tokens to issue
         * @param holder the owner of the issued tokens
         */
        public IssueFungibleToken(Date offeringDate, long fungibleAmount, Party holder) {
            this.offeringDate = offeringDate;
            this.fungibleAmount = fungibleAmount;
            this.holder = holder;
        }

        @Suspendable
        @Override
        public UniqueIdentifier call() throws FlowException {
            BondState bondState = null;
            Party issuer = getOurIdentity();
            StateAndRef<BondState> input = null;

            ServiceHub context = getServiceHub();

            // let's verify if the commercial paper token type is created.
            for (StateAndRef<BondState> stateAndRef : getServiceHub ().getVaultService().queryBy(BondState.class).getStates()){
                if (stateAndRef.getState().getData().getofferingDate().equals(this.offeringDate)){
                    // we found a match by offering date
                    input = stateAndRef;
                    bondState = stateAndRef.getState().getData();
                }
            }

            FlowSession issuerSession = initiateFlow(issuer);
            FlowSession holderSession = initiateFlow(holder);

            // we don't have the commercial paper token type created. so we will do that first.
            if (bondState == null){
                UniqueIdentifier linearId;
                linearId = new UniqueIdentifier();
                bondState = new BondState(0L, issuer, this.offeringDate, 0, Arrays.asList(issuer),linearId);
                BondFlow.IssueType commercialPaperTokenTypeFlow = new BondFlow.IssueType(issuerSession);
                commercialPaperTokenTypeFlow.setBondState(bondState);
                SignedTransaction signedTransaction = subFlow(commercialPaperTokenTypeFlow);
                input = signedTransaction.getCoreTransaction().outRef(0);
            }

            // now we have the token type, so lets issue fungible tokens on it.
            // we create the pointer
            TokenPointer tokenPointer = bondState.toPointer(bondState.getClass());
            IssuedTokenType issuedTokenType = new IssuedTokenType(bondState.getIssuer(), tokenPointer);

            // we create the fungible token
            Amount<IssuedTokenType> amount = new Amount<>(fungibleAmount, issuedTokenType);
            FungibleToken fungibleToken = new FungibleToken(amount,holder, null);

            subFlow(new IssueTokens(Arrays.asList(fungibleToken),Arrays.asList(issuer, holder)));

            // now we will update the evolvable token with new value
            long currentValuation = bondState.getValuation();
            long newValuation = currentValuation + this.fungibleAmount;
            ArrayList<Party> newMaintainers = new ArrayList<>(bondState.getMaintainers());
            newMaintainers.add(holder);

            BondState newBondState = new BondState(newValuation,
                    bondState.getIssuer(),
                    bondState.getofferingDate(),
                    bondState.getFractionDigits(),
                    newMaintainers,
                    bondState.getLinearId());

            subFlow(new UpdateEvolvableToken(input, newBondState, Arrays.asList(holder)));

            return bondState.getLinearId();
        }
    }
}
