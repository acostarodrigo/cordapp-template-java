package org.shield.webserver.membership;

import net.corda.core.identity.Party;
import org.shield.membership.ShieldMetadata;
import org.shield.webserver.connection.User;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
public class RequestWrapper {
    @NotNull(message = "Please provide a valid user")
    private User user;
    @NotEmpty(message = "Please provide a valid organization Name")
    private String orgName;
    @NotEmpty(message = "Please provide valid organization types")
    private List<String> orgTypes;
    @NotEmpty(message = "Please provide valid bond roles if any")
    private List<String> bondRoles;
    @NotEmpty(message = "Please provide a valid organization contact")
    private String orgContact;
    private List<Party> custodians;
    private List<Party> treasurers;

    private ShieldMetadata metadata;

    public RequestWrapper(User user, String orgName, List<String> orgTypes, List<String> bondRoles, String orgContact, List<Party> custodians, List<Party> treasurers) {
        this.user = user;
        this.orgName = orgName;
        this.orgTypes = orgTypes;
        this.bondRoles = bondRoles;
        this.orgContact = orgContact;
        this.custodians = custodians;
        this.treasurers = treasurers;

        List<ShieldMetadata.OrgType> orgTypeList = new ArrayList<>();
        for (String type : orgTypes){
            orgTypeList.add(ShieldMetadata.OrgType.valueOf(type));
        }

        List<ShieldMetadata.BondRole> bondRoleList = new ArrayList<>();
        for (String type : bondRoles){
            bondRoleList.add(ShieldMetadata.BondRole.valueOf(type));
        }

        this.metadata = new ShieldMetadata(orgName,orgTypeList, orgContact, bondRoleList,custodians, treasurers );

    }

    public ShieldMetadata getMetadata() {
        return metadata;
    }

    public User getUser() {
        return user;
    }
}
