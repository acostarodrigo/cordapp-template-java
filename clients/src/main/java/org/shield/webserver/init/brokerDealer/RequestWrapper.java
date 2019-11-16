package org.shield.webserver.init.brokerDealer;

import org.shield.webserver.connection.User;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Entity
public class RequestWrapper{
    @NotNull(message = "Please provide a valid username")
    private User user;
    @NotEmpty(message = "Please provide a valid issuer")
    private String issuer;

    public User getUser() {
        return user;
    }

    public String getIssuer() {
        return issuer;
    }
}
