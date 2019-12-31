package org.shield.webserver.membership;

import com.fasterxml.jackson.databind.JsonNode;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import org.shield.membership.ShieldMetadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Because we can't fully deserialize to be used as JsonNode in the controller, we customize the creation of the metadata
 */
public class MetadataBuilder {
    private JsonNode jsonNode;
    private CordaRPCOps proxy;

    public MetadataBuilder(JsonNode jsonNode, CordaRPCOps proxy) {
        this.jsonNode = jsonNode;
        this.proxy = proxy;
    }

    /**
     * given a specified JsonNode, it will create the shield metadata
     * An example Json node is:
     * {
     * 		"orgName": "issuer",
     * 		"orgTypes": ["BOND_PARTICIPANT"],
     * 		"bondRoles": ["ISSUER", "SELLER", "BUYER"],
     * 		"orgContact": "issuer@shield.com",
     * 		"custodians": ["O=broker1,L=New York,C=US"],
     * 		"treasurers": ["O=broker1,L=New York,C=US"]
     *        }
     * @return
     */
    public ShieldMetadata getMetadata(){
        String orgName = jsonNode.get("orgName").toString();
        List<ShieldMetadata.OrgType> orgTypeList = getOrgTypes(jsonNode.get("orgTypes"));
        String orgContact = jsonNode.get("orgContact").toString();
        List<ShieldMetadata.BondRole> bondRoleList = getBondRoles(jsonNode.get("bondRoles"));
        List<Party> custodiansList = getCustodians(jsonNode.get("custodians"));
        List<Party> treasurersList = getCustodians(jsonNode.get("treasurers"));
        ShieldMetadata metadata = new ShieldMetadata(orgName, orgTypeList,orgContact,bondRoleList,custodiansList,treasurersList);
        return metadata;
    }

    private List<Party> getCustodians(JsonNode jsonNode) {
        Iterator custodians = jsonNode.elements();

        List<Party> custodianList = new ArrayList<>();
        while (custodians.hasNext()){
            JsonNode node = (JsonNode) custodians.next();
            CordaX500Name name = CordaX500Name.parse(node.asText());
            custodianList.add(proxy.wellKnownPartyFromX500Name(name));
        }
        return custodianList;
    }

    private List<ShieldMetadata.BondRole> getBondRoles(JsonNode jsonNode){
        Iterator bondRoles = jsonNode.elements();

        List<ShieldMetadata.BondRole> bondRoleList = new ArrayList<>();
        while (bondRoles.hasNext()){
            JsonNode node = (JsonNode) bondRoles.next();
            ShieldMetadata.BondRole bondRole = ShieldMetadata.BondRole.valueOf(node.asText());
            bondRoleList.add(bondRole);
        }
        return bondRoleList;
    }

    private List<ShieldMetadata.OrgType> getOrgTypes(JsonNode jsonNode){
        Iterator orgTypes = jsonNode.elements();

        List<ShieldMetadata.OrgType> orgTypeList = new ArrayList<>();
        while (orgTypes.hasNext()){
            JsonNode node = (JsonNode) orgTypes.next();
            ShieldMetadata.OrgType orgType = ShieldMetadata.OrgType.valueOf(node.asText());
            orgTypeList.add(orgType);
        }
        return orgTypeList;
    }
}
