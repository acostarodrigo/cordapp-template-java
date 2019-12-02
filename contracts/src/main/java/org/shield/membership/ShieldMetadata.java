package org.shield.membership;

import net.corda.core.serialization.CordaSerializable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;


/**
 * Shield organization in the context of Business Network membership service
 */
@CordaSerializable
public class ShieldMetadata  {

    /**
     * Organization Type Enum
     */
    @CordaSerializable
    public enum OrgType {
        BOND_PARTICIPANT(1),
        CUSTODIAN(2),
        NETWORK_TREASURER(3);

        private int value;

        // constructor for the OrgType enum
        OrgType(int value){
            this.value = value;
        }

        public int getValue(){return this.value;}

        public static OrgType getType(int value){
            if (value == 1) return OrgType.BOND_PARTICIPANT;
            if (value == 2) return OrgType.CUSTODIAN;
            if (value == 3) return OrgType.NETWORK_TREASURER;

            return null;
        }
    };

    /**
     * Bond role enum
     */
    @CordaSerializable
    public enum BondRole{
        ISSUER(1),
        BUYER(2),
        SELLER(3);

        private int value;
        BondRole(int value){this.value = value;}

        public int getValue(){return this.value;}

        public static BondRole getType(int value){
            if (value == 1) return BondRole.ISSUER;
            if (value == 2) return BondRole.BUYER;
            if (value == 3) return BondRole.SELLER;

            return null;
        }
    }

    private String orgName;
    private List<OrgType> orgTypes;
    private List<BondRole> bondRoles;
    private String orgContact;


    public ShieldMetadata(String orgName, List<OrgType> orgTypes, String orgContact, @Nullable List<BondRole> bondRoles) {
        this.orgName = orgName;
        this.orgTypes = orgTypes;
        this.orgContact = orgContact;
        this.bondRoles = bondRoles;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public List<OrgType> getOrgTypes() {
        return orgTypes;
    }

    public void setOrgTypes(List<OrgType> orgTypes) {
        this.orgTypes = orgTypes;
    }

    public List<BondRole> getBondRoles() {
        return bondRoles;
    }

    public void setBondRoles(List<BondRole> bondRoles) {
        this.bondRoles = bondRoles;
    }

    public String getOrgContact() {
        return orgContact;
    }

    public void setOrgContact(String orgContact) {
        this.orgContact = orgContact;
    }

    @Override
    public String toString() {
        return "ShieldMetadata{" +
            "orgName='" + orgName + '\'' +
            ", orgTypes=" + orgTypes +
            ", bondRoles=" + bondRoles +
            ", orgContact='" + orgContact + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShieldMetadata metadata = (ShieldMetadata) o;
        return Objects.equals(getOrgName(), metadata.getOrgName()) &&
            Objects.equals(getOrgTypes(), metadata.getOrgTypes()) &&
            Objects.equals(getBondRoles(), metadata.getBondRoles()) &&
            Objects.equals(getOrgContact(), metadata.getOrgContact());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrgName(), getOrgTypes(), getBondRoles(), getOrgContact());
    }
}
