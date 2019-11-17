package org.shield.webserver.init.issuer;

import org.shield.webserver.connection.User;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class RequestWrapper {
    @NotNull(message = "Please provide a valid username")
    private User user;
    @NotEmpty(message = "Please provide a valid broker dealer")
    private String brokerDealer;

    public User getUser() {
        return user;
    }

    public String getBrokerDealer() {
        return brokerDealer;
    }
}
