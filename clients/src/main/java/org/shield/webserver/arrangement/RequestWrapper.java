package org.shield.webserver.arrangement;

import org.shield.webserver.connection.User;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
public class RequestWrapper {
    @NotNull(message = "Please provide a valid user")
    private User user;
    @NotEmpty(message = "Please provide a valid broker Dealer")
    private String brokerDealer;
    @NotNull(message = "Please provide a valid size value")
    private int size;
    @NotNull(message = "Please provide a valid offering date")
    private Date offeringDate;

    public User getUser() {
        return user;
    }

    public String getBrokerDealer() {
        return brokerDealer;
    }

    public int getSize() {
        return size;
    }

    public Date getOfferingDate() {
        return offeringDate;
    }
}
