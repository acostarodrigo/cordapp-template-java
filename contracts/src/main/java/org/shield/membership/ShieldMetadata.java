package org.shield.membership;

import net.corda.core.serialization.CordaSerializable;

import java.io.Serializable;

@CordaSerializable
/**
 * Shield organization in the context of Business Network membership service
 */
public class ShieldMetadata implements Serializable {
    public enum OrgType {
        BOND_PARTICIPANT,
        CUSTODIAN,
        NETWORK_TREASURER
    };

    private String orgName;
    private OrgType orgType;
    private String orgContact;

    public ShieldMetadata(String orgName, OrgType orgType, String orgContact) {
        this.orgName = orgName;
        this.orgType = orgType;
        this.orgContact = orgContact;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public OrgType getOrgType() {
        return orgType;
    }

    public void setOrgType(OrgType orgType) {
        this.orgType = orgType;
    }

    public String getOrgContact() {
        return orgContact;
    }

    public void setOrgContact(String orgContact) {
        this.orgContact = orgContact;
    }
}
